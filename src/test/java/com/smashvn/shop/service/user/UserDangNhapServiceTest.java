package com.smashvn.shop.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.exception.AccountLockedException;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.exception.InvalidPasswordException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

@ExtendWith(MockitoExtension.class)
class UserDangNhapServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserDangNhapService service;

    @BeforeEach
    void setUp() {
        service = new UserDangNhapService(taiKhoanRepository, khachHangRepository, passwordEncoder);
    }

    @Test
    void existingEmailIsReadFromDatabaseAndComparedWithStoredHash() {
        TaiKhoan account = activeCustomer("customer@gmail.com", "$2a$storedHash");
        when(taiKhoanRepository.findByUsername("customer@gmail.com")).thenReturn(account);
        when(passwordEncoder.matches("Password1", "$2a$storedHash")).thenReturn(true);

        TaiKhoan result = service.kiemTraDangNhap(" CUSTOMER@GMAIL.COM ", "Password1");

        assertSame(account, result);
        verify(taiKhoanRepository).findByUsername("customer@gmail.com");
        verify(passwordEncoder).matches("Password1", "$2a$storedHash");
        verifyNoInteractions(khachHangRepository);
    }

    @Test
    void existingEmailWithWrongPasswordThrowsPasswordSpecificError() {
        TaiKhoan account = activeCustomer("customer@gmail.com", "$2a$storedHash");
        when(taiKhoanRepository.findByUsername("customer@gmail.com")).thenReturn(account);
        when(passwordEncoder.matches("WrongPassword", "$2a$storedHash")).thenReturn(false);

        InvalidPasswordException error = assertThrows(
                InvalidPasswordException.class,
                () -> service.kiemTraDangNhap("customer@gmail.com", "WrongPassword"));

        assertEquals("Mật khẩu không chính xác.", error.getMessage());
        verify(passwordEncoder).matches("WrongPassword", "$2a$storedHash");
    }

    @Test
    void missingEmailStillPerformsTimingSafePasswordCheckBeforeNotFoundError() {
        when(taiKhoanRepository.findByUsername("missing@gmail.com")).thenReturn(null);
        when(passwordEncoder.matches(
                org.mockito.ArgumentMatchers.eq("Password1"),
                argThat(hash -> hash != null && hash.toString().startsWith("$2a$"))))
                .thenReturn(false);

        assertThrows(
                AccountNotFoundException.class,
                () -> service.kiemTraDangNhap("missing@gmail.com", "Password1"));

        verify(taiKhoanRepository).findByUsername("missing@gmail.com");
        verify(passwordEncoder).matches(
                org.mockito.ArgumentMatchers.eq("Password1"),
                argThat(hash -> hash != null && hash.toString().startsWith("$2a$")));
    }

    @Test
    void phoneFallsBackToCustomerProfileAndUsesLinkedAccountHash() {
        TaiKhoan account = activeCustomer("customer@gmail.com", "$2a$phoneHash");
        KhachHang customer = new KhachHang();
        customer.setTaiKhoan(account);

        when(taiKhoanRepository.findByUsername("0912345678")).thenReturn(null);
        when(khachHangRepository.findBySoDienThoaiKh("0912345678")).thenReturn(customer);
        when(passwordEncoder.matches("Password1", "$2a$phoneHash")).thenReturn(true);

        TaiKhoan result = service.kiemTraDangNhap("+84 912-345-678", "Password1");

        assertSame(account, result);
        verify(khachHangRepository).findBySoDienThoaiKh("0912345678");
        verify(passwordEncoder).matches("Password1", "$2a$phoneHash");
    }

    @Test
    void legacyUsernameRemainsAvailableForAdminLoginService() {
        TaiKhoan account = activeCustomer("admin", "$2a$adminHash");
        account.setVaiTro("QL");
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(account);
        when(passwordEncoder.matches("Password1", "$2a$adminHash")).thenReturn(true);

        TaiKhoan result = service.kiemTraDangNhap("admin", "Password1");

        assertSame(account, result);
        verify(taiKhoanRepository).findByUsername("admin");
        verify(passwordEncoder).matches("Password1", "$2a$adminHash");
    }

    @Test
    void lockedAccountUsesAccountLevelErrorAfterCorrectPassword() {
        TaiKhoan account = activeCustomer("locked@gmail.com", "$2a$lockedHash");
        account.setTrangThaiTaiKhoan(AccountStatus.LOCKED);
        when(taiKhoanRepository.findByUsername("locked@gmail.com")).thenReturn(account);
        when(passwordEncoder.matches("Password1", "$2a$lockedHash")).thenReturn(true);

        AccountLockedException error = assertThrows(
                AccountLockedException.class,
                () -> service.kiemTraDangNhap("locked@gmail.com", "Password1"));

        assertEquals("Tài khoản của bạn đã bị khóa.", error.getMessage());
    }

    @Test
    void guestWithCorrectTemporaryPasswordRemainsGuestPendingPasswordSetup() {
        TaiKhoan account = activeCustomer("guest@gmail.com", "$2a$tempHash");
        account.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        when(taiKhoanRepository.findByUsername("guest@gmail.com")).thenReturn(account);
        when(passwordEncoder.matches("Temporary123", "$2a$tempHash")).thenReturn(true);

        TaiKhoan result = service.kiemTraDangNhap("guest@gmail.com", "Temporary123");

        assertSame(account, result);
        assertEquals(AccountStatus.GUEST, result.getTrangThaiTaiKhoan());
        verify(taiKhoanRepository, never()).saveAndFlush(account);
    }

    private TaiKhoan activeCustomer(String username, String passwordHash) {
        TaiKhoan account = new TaiKhoan();
        account.setUsername(username);
        account.setMatKhau(passwordHash);
        account.setVaiTro("KH");
        account.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        return account;
    }
}
