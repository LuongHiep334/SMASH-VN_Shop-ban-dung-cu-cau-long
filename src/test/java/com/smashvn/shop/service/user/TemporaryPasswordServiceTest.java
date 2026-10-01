package com.smashvn.shop.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.user.TemporaryPasswordService.IssueStatus;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;
    @Mock
    private TokenKhoiPhucRepository tokenRepository;
    @Mock
    private JavaMailSender mailSender;

    private BCryptPasswordEncoder passwordEncoder;
    private TemporaryPasswordService service;
    private TaiKhoan guest;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new TemporaryPasswordService(
                taiKhoanRepository,
                tokenRepository,
                passwordEncoder,
                mailSender);

        guest = new TaiKhoan();
        guest.setId(10);
        guest.setUsername("guest@example.com");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guest.setMatKhau(null);
        guest.setSoLanMuaThanhCong(0);

        org.mockito.Mockito.lenient()
                .when(taiKhoanRepository.findByIdForUpdate(10))
                .thenReturn(Optional.of(guest));
        org.mockito.Mockito.lenient()
                .when(tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(10, "GUEST_ACTIVATION"))
                .thenReturn(List.of(validActivationToken()));
    }

    @Test
    void issueInitialPasswordStoresOnlyBcryptAndReturnsStrongTemporaryPassword() {
        var result = service.issueInitialPassword(10);

        assertEquals(IssueStatus.ISSUED, result.status());
        assertNotNull(result.temporaryPassword());
        assertTrue(result.temporaryPassword().length() >= 12);
        assertTrue(result.temporaryPassword().matches(".*[A-Z].*"));
        assertTrue(result.temporaryPassword().matches(".*[a-z].*"));
        assertTrue(result.temporaryPassword().matches(".*[0-9].*"));
        assertNotEquals(result.temporaryPassword(), guest.getMatKhau());
        assertTrue(guest.getMatKhau().startsWith("$2"));
        assertTrue(passwordEncoder.matches(result.temporaryPassword(), guest.getMatKhau()));
        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
        verify(taiKhoanRepository).saveAndFlush(guest);
    }

    @Test
    void duplicateIssuanceDoesNotRotateExistingTemporaryPassword() {
        var first = service.issueInitialPassword(10);
        String firstHash = guest.getMatKhau();

        var second = service.issueInitialPassword(10);

        assertTrue(first.isIssued());
        assertEquals(IssueStatus.ALREADY_ISSUED, second.status());
        assertFalse(second.isIssued());
        assertEquals(firstHash, guest.getMatKhau());
    }

    @Test
    void twoDifferentSepayOrdersPaidInReverseOrderIssueOnlyForTheFirstSuccessfulCallback() {
        var webhookForOrderB = service.recordSepayPaymentSuccess(10);
        String firstHash = guest.getMatKhau();
        var webhookForOlderOrderA = service.recordSepayPaymentSuccess(10);

        assertTrue(webhookForOrderB.isIssued());
        assertEquals(10, webhookForOrderB.accountId());
        assertFalse(webhookForOlderOrderA.isIssued());
        assertEquals(IssueStatus.NOT_REQUESTED, webhookForOlderOrderA.status());
        assertEquals(2, guest.getSoLanMuaThanhCong());
        assertEquals(firstHash, guest.getMatKhau());
    }

    @Test
    void activeAccountPasswordIsNeverChanged() {
        guest.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        guest.setMatKhau(passwordEncoder.encode("Official123"));
        String officialHash = guest.getMatKhau();

        var result = service.issueInitialPassword(10);

        assertEquals(IssueStatus.NOT_ELIGIBLE, result.status());
        assertEquals(officialHash, guest.getMatKhau());
        verify(taiKhoanRepository, never()).saveAndFlush(guest);
    }

    @Test
    void codExistingGuestCanPurchaseAgainWithoutPasswordRotation() {
        guest.setMatKhau(passwordEncoder.encode("Temporary123"));
        String existingHash = guest.getMatKhau();

        var result = service.recordCodOrderCreated(10, false);

        assertEquals(IssueStatus.NOT_REQUESTED, result.status());
        assertEquals(1, guest.getSoLanMuaThanhCong());
        assertEquals(existingHash, guest.getMatKhau());
    }

    @Test
    void emailTemplateContainsTemporaryCredentialAndActivationFallback() {
        String html = TemporaryPasswordService.buildTemporaryPasswordEmailHtml(
                "guest@example.com",
                "A7kp2Qm9Xs4L",
                "https://smash.vn/user/dang-nhap",
                "https://smash.vn/user/thiet-lap-mat-khau?token=abc");

        assertTrue(html.contains("guest@example.com"));
        assertTrue(html.contains("A7kp2Qm9Xs4L"));
        assertTrue(html.contains("Đây không phải mật khẩu chính thức"));
        assertTrue(html.contains("token=abc"));
    }

    @Test
    void issuedPasswordEmailIsSentExactlyOnce() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        var result = new TemporaryPasswordService.TemporaryPasswordIssueResult(
                IssueStatus.ISSUED,
                10,
                "guest@example.com",
                "A7kp2Qm9Xs4L",
                "activation-token");

        service.sendTemporaryPasswordEmail(result, "https://smash.vn");

        verify(mailSender).send(message);
        assertEquals("guest@example.com", message.getAllRecipients()[0].toString());
        assertTrue(message.getSubject().contains("Mật khẩu tạm thời"));
    }

    @Test
    void smtpFailureKeepsGuestHashAndDoesNotRegenerateTemporaryPassword() {
        var issued = service.issueInitialPassword(10);
        String storedHash = guest.getMatKhau();
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP unavailable"));

        assertDoesNotThrow(() -> service.sendTemporaryPasswordEmail(issued, "https://smash.vn"));

        assertEquals(AccountStatus.GUEST, guest.getTrangThaiTaiKhoan());
        assertEquals(storedHash, guest.getMatKhau());
        assertEquals("activation-token", issued.activationToken());
        var retry = service.issueInitialPassword(10);
        assertEquals(IssueStatus.ALREADY_ISSUED, retry.status());
        assertEquals(storedHash, guest.getMatKhau());
    }

    private TokenKhoiPhuc validActivationToken() {
        TokenKhoiPhuc token = new TokenKhoiPhuc();
        token.setId(20);
        token.setMaXacNhan("activation-token");
        token.setThoiGianHetHan(LocalDateTime.now().plusDays(1));
        token.setDaSuDung(false);
        return token;
    }
}
