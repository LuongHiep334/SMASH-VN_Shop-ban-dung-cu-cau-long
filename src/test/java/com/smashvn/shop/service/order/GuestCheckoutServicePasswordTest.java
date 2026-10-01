package com.smashvn.shop.service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;

@ExtendWith(MockitoExtension.class)
class GuestCheckoutServicePasswordTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;
    @Mock
    private KhachHangRepository khachHangRepository;
    @Mock
    private TokenKhoiPhucRepository tokenRepository;
    @Mock
    private ThongBaoRepository thongBaoRepository;
    @Mock
    private HoaDonChiTietRepository hoaDonChiTietRepository;
    @Mock
    private JavaMailSender mailSender;

    private BCryptPasswordEncoder passwordEncoder;
    private GuestCheckoutService service;
    private TaiKhoan guest;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new GuestCheckoutService(
                taiKhoanRepository,
                khachHangRepository,
                tokenRepository,
                thongBaoRepository,
                hoaDonChiTietRepository,
                mailSender,
                passwordEncoder);

        guest = new TaiKhoan();
        guest.setId(30);
        guest.setUsername("guest@example.com");
        guest.setVaiTro("KH");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guest.setMatKhau(passwordEncoder.encode("Temporary123"));
        org.mockito.Mockito.lenient()
                .when(taiKhoanRepository.findByIdForUpdate(30))
                .thenReturn(Optional.of(guest));
    }

    @Test
    void officialPasswordIsTheStepThatActivatesGuestAndReplacesTemporaryHash() {
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(30, "GUEST_ACTIVATION"))
                .thenReturn(List.of());

        service.setPasswordForGuest(30, "Official456");

        assertEquals(AccountStatus.ACTIVE, guest.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches("Official456", guest.getMatKhau()));
        assertFalse(passwordEncoder.matches("Temporary123", guest.getMatKhau()));
    }

    @Test
    void officialPasswordMustDifferFromTemporaryPassword() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.setPasswordForGuest(30, "Temporary123"));

        assertTrue(exception.getMessage().contains("khác mật khẩu tạm"));
        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
    }

    @Test
    void activationLinkCanReplaceTemporaryPasswordAndCannotBeUsedTwice() {
        TokenKhoiPhuc activation = new TokenKhoiPhuc();
        activation.setId(40);
        activation.setTaiKhoan(guest);
        activation.setMaXacNhan("activation-token");
        activation.setLoaiXacNhan("GUEST_ACTIVATION");
        activation.setThoiGianHetHan(LocalDateTime.now().plusDays(1));
        activation.setDaSuDung(false);

        when(tokenRepository.findByMaXacNhan("activation-token")).thenReturn(activation);
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(30, "GUEST_ACTIVATION"))
                .thenReturn(List.of(activation));

        service.setPasswordByToken("activation-token", "Official789");

        assertEquals(AccountStatus.ACTIVE, guest.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches("Official789", guest.getMatKhau()));
        assertTrue(activation.isDaSuDung());
        assertThrows(RuntimeException.class,
                () -> service.setPasswordByToken("activation-token", "Another123"));
    }

    @Test
    void purchaseCountNeverExpiresGuestEmailStatus() {
        guest.setMatKhau(null);
        guest.setSoLanMuaThanhCong(10);
        when(taiKhoanRepository.findByUsername("guest@example.com")).thenReturn(guest);

        assertEquals("GUEST_NO_PASSWORD", service.checkEmailStatus("guest@example.com"));
        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
    }

    @Test
    void guestWithTemporaryPasswordIsNotMistakenForActive() {
        when(taiKhoanRepository.findByUsername("guest@example.com")).thenReturn(guest);

        assertEquals("GUEST_WITH_TEMP_PASSWORD", service.checkEmailStatus("guest@example.com"));
        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
    }

    @Test
    void smtpFailureDuringActivationResendStillAllowsTokenActivationWithoutRotatingTempPassword() {
        guest.setUsername("guest@realmail.vn");
        String temporaryHash = guest.getMatKhau();
        TokenKhoiPhuc oldToken = new TokenKhoiPhuc();
        oldToken.setId(41);
        oldToken.setTaiKhoan(guest);
        oldToken.setMaXacNhan("old-token");
        oldToken.setLoaiXacNhan("GUEST_ACTIVATION");
        oldToken.setThoiGianHetHan(LocalDateTime.now().plusDays(1));
        oldToken.setDaSuDung(false);

        when(taiKhoanRepository.findById(30)).thenReturn(Optional.of(guest));
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(30, "GUEST_ACTIVATION"))
                .thenReturn(List.of(oldToken));
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP unavailable"));

        assertDoesNotThrow(() -> service.resendGuestActivationEmail(30, "https://smash.vn"));

        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
        assertEquals(temporaryHash, guest.getMatKhau());
        assertTrue(oldToken.isDaSuDung());

        ArgumentCaptor<TokenKhoiPhuc> tokenCaptor = ArgumentCaptor.forClass(TokenKhoiPhuc.class);
        verify(tokenRepository, atLeast(2)).save(tokenCaptor.capture());
        TokenKhoiPhuc newToken = tokenCaptor.getAllValues().stream()
                .filter(token -> token != oldToken)
                .findFirst()
                .orElseThrow();
        assertEquals("GUEST_ACTIVATION", newToken.getLoaiXacNhan());
        assertFalse(newToken.isDaSuDung());

        when(tokenRepository.findByMaXacNhan(newToken.getMaXacNhan())).thenReturn(newToken);
        when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(30, "GUEST_ACTIVATION"))
                .thenReturn(List.of(newToken));

        service.setPasswordByToken(newToken.getMaXacNhan(), "Official987");

        assertEquals(AccountStatus.ACTIVE, guest.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches("Official987", guest.getMatKhau()));
    }
}
