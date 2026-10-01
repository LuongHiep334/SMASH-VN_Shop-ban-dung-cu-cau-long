package com.smashvn.shop.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;

@ExtendWith(MockitoExtension.class)
class UserQuenMatKhauServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;
    @Mock
    private KhachHangRepository khachHangRepository;
    @Mock
    private TokenKhoiPhucRepository tokenRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GuestCheckoutService guestCheckoutService;

    private UserQuenMatKhauService service;

    @BeforeEach
    void setUp() {
        service = new UserQuenMatKhauService(
                taiKhoanRepository,
                khachHangRepository,
                tokenRepository,
                mailSender,
                passwordEncoder,
                guestCheckoutService);
    }

    @Test
    void guestWithoutTemporaryPasswordCannotRequestForgotPasswordToken() {
        TaiKhoan guest = guestAccount(null);
        when(taiKhoanRepository.findByUsername("guest@realmail.vn")).thenReturn(guest);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.guiYeuCauKhoiPhuc("guest@realmail.vn", "https://smash.vn"));

        assertTrue(exception.getMessage().contains("chưa thiết lập mật khẩu"));
        verifyNoInteractions(tokenRepository);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void guestWithTemporaryPasswordMayRequestForgotPasswordAsEmailOwnershipFallback() {
        TaiKhoan guest = guestAccount("$2a$temporary-hash");
        when(taiKhoanRepository.findByUsername("guest@realmail.vn")).thenReturn(guest);
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(50, "FORGOT_PASSWORD"))
                .thenReturn(List.of());

        service.guiYeuCauKhoiPhuc("guest@realmail.vn", "https://smash.vn");

        ArgumentCaptor<TokenKhoiPhuc> tokenCaptor = ArgumentCaptor.forClass(TokenKhoiPhuc.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals("FORGOT_PASSWORD", tokenCaptor.getValue().getLoaiXacNhan());
        assertEquals(guest, tokenCaptor.getValue().getTaiKhoan());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void forgotPasswordForGuestDelegatesToTheSingleGuestActivationPoint() {
        TaiKhoan guest = guestAccount("$2a$temporary-hash");
        TokenKhoiPhuc token = new TokenKhoiPhuc();
        token.setTaiKhoan(guest);
        token.setMaXacNhan("forgot-token");
        token.setLoaiXacNhan("FORGOT_PASSWORD");
        token.setThoiGianHetHan(LocalDateTime.now().plusMinutes(10));
        token.setDaSuDung(false);

        when(tokenRepository.findByMaXacNhan("forgot-token")).thenReturn(token);
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(50, "FORGOT_PASSWORD"))
                .thenReturn(List.of());

        service.datLaiMatKhau("forgot-token", "Official456");

        verify(guestCheckoutService).setPasswordForGuest(50, "Official456");
        verify(taiKhoanRepository, never()).saveAndFlush(guest);
        assertTrue(token.isDaSuDung());
    }

    private TaiKhoan guestAccount(String passwordHash) {
        TaiKhoan account = new TaiKhoan();
        account.setId(50);
        account.setUsername("guest@realmail.vn");
        account.setVaiTro("KH");
        account.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        account.setMatKhau(passwordHash);
        return account;
    }
}
