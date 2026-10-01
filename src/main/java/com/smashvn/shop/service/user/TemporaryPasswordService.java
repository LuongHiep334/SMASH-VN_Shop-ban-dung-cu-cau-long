package com.smashvn.shop.service.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemporaryPasswordService {

    private static final int TEMPORARY_PASSWORD_LENGTH = 14;
    private static final char[] UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWERCASE = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] ALPHANUMERIC =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    public enum IssueStatus {
        ISSUED,
        ALREADY_ISSUED,
        NOT_ELIGIBLE,
        NOT_REQUESTED
    }

    /**
     * Plaintext chỉ được giữ trong kết quả ngắn hạn để gửi email sau commit.
     * Không lưu kết quả này vào entity, session hay log.
     */
    public record TemporaryPasswordIssueResult(
            IssueStatus status,
            Integer accountId,
            String recipientEmail,
            String temporaryPassword,
            String activationToken) {

        public boolean isIssued() {
            return status == IssueStatus.ISSUED;
        }
    }

    /** COD được tính là giao dịch thành công ngay sau khi đơn đã tạo thành công. */
    @Transactional
    public TemporaryPasswordIssueResult recordCodOrderCreated(Integer accountId, boolean isNewAccount) {
        TaiKhoan account = findAccountWithLock(accountId);
        incrementSuccessfulPurchase(account);
        return isNewAccount
                ? issueInitialPasswordLocked(account)
                : result(IssueStatus.NOT_REQUESTED, account, null, findValidActivationToken(account.getId()));
    }

    /**
     * SePay chỉ đi qua đây trong nhánh đã xác nhận thanh toán thành công. Counter trước
     * khi tăng xác định đây có phải giao dịch thành công đầu tiên hay không.
     */
    @Transactional
    public TemporaryPasswordIssueResult recordSepayPaymentSuccess(Integer accountId) {
        TaiKhoan account = findAccountWithLock(accountId);
        int previousPurchaseCount = successfulPurchaseCount(account);
        incrementSuccessfulPurchase(account);
        return previousPurchaseCount == 0
                ? issueInitialPasswordLocked(account)
                : result(IssueStatus.NOT_REQUESTED, account, null, findValidActivationToken(account.getId()));
    }

    /** API idempotent dùng cho các luồng cần cấp ban đầu nhưng không cập nhật counter. */
    @Transactional
    public TemporaryPasswordIssueResult issueInitialPassword(Integer accountId) {
        return issueInitialPasswordLocked(findAccountWithLock(accountId));
    }

    private TaiKhoan findAccountWithLock(Integer accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin tài khoản");
        }
        return taiKhoanRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
    }

    private TemporaryPasswordIssueResult issueInitialPasswordLocked(TaiKhoan account) {
        String activationToken = findValidActivationToken(account.getId());

        if (account.getTrangThaiTaiKhoan() != AccountStatus.GUEST) {
            return result(IssueStatus.NOT_ELIGIBLE, account, null, activationToken);
        }
        if (hasText(account.getMatKhau())) {
            return result(IssueStatus.ALREADY_ISSUED, account, null, activationToken);
        }

        String temporaryPassword = generateTemporaryPassword();
        account.setMatKhau(passwordEncoder.encode(temporaryPassword));
        taiKhoanRepository.saveAndFlush(account);

        log.info("[TEMP_PASSWORD] Issued initial temporary password hash for GUEST account ID {}.", account.getId());
        return result(IssueStatus.ISSUED, account, temporaryPassword, activationToken);
    }

    private void incrementSuccessfulPurchase(TaiKhoan account) {
        int current = successfulPurchaseCount(account);
        account.setSoLanMuaThanhCong(current + 1);
        taiKhoanRepository.save(account);
        log.info("[GUEST_CHECKOUT] Incremented successful purchase count for TaiKhoan ID {} to {}.",
                account.getId(), current + 1);
    }

    private int successfulPurchaseCount(TaiKhoan account) {
        return account.getSoLanMuaThanhCong() == null ? 0 : account.getSoLanMuaThanhCong();
    }

    private String findValidActivationToken(Integer accountId) {
        LocalDateTime now = LocalDateTime.now();
        List<TokenKhoiPhuc> tokens = tokenRepository
                .findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(accountId, "GUEST_ACTIVATION");
        return tokens.stream()
                .filter(token -> token.getThoiGianHetHan() != null && token.getThoiGianHetHan().isAfter(now))
                .max(Comparator.comparing(TokenKhoiPhuc::getId))
                .map(TokenKhoiPhuc::getMaXacNhan)
                .orElse(null);
    }

    private TemporaryPasswordIssueResult result(
            IssueStatus status,
            TaiKhoan account,
            String temporaryPassword,
            String activationToken) {
        return new TemporaryPasswordIssueResult(
                status,
                account.getId(),
                account.getUsername(),
                temporaryPassword,
                activationToken);
    }

    String generateTemporaryPassword() {
        char[] password = new char[TEMPORARY_PASSWORD_LENGTH];
        password[0] = randomChar(UPPERCASE);
        password[1] = randomChar(LOWERCASE);
        password[2] = randomChar(DIGITS);
        for (int i = 3; i < password.length; i++) {
            password[i] = randomChar(ALPHANUMERIC);
        }
        for (int i = password.length - 1; i > 0; i--) {
            int swapIndex = secureRandom.nextInt(i + 1);
            char current = password[i];
            password[i] = password[swapIndex];
            password[swapIndex] = current;
        }
        return new String(password);
    }

    private char randomChar(char[] source) {
        return source[secureRandom.nextInt(source.length)];
    }

    @Async
    public void sendTemporaryPasswordEmail(
            TemporaryPasswordIssueResult issueResult,
            String appUrl) {
        if (issueResult == null || !issueResult.isIssued()) {
            return;
        }

        String recipientEmail = issueResult.recipientEmail();
        if (!hasText(recipientEmail)) {
            log.warn("[TEMP_PASSWORD_EMAIL] Recipient email is empty; skipped for account ID {}.",
                    issueResult.accountId());
            return;
        }
        String normalizedBaseUrl = hasText(appUrl) ? appUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
        String loginUrl = normalizedBaseUrl + "/user/dang-nhap";
        String activationUrl = hasText(issueResult.activationToken())
                ? normalizedBaseUrl + "/user/thiet-lap-mat-khau?token=" + issueResult.activationToken()
                : normalizedBaseUrl + "/user/thiet-lap-mat-khau";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("[Smash VN] Mật khẩu tạm thời cho tài khoản của bạn");
            helper.setText(buildTemporaryPasswordEmailHtml(
                    recipientEmail,
                    issueResult.temporaryPassword(),
                    loginUrl,
                    activationUrl), true);
            mailSender.send(message);
            log.info("[TEMP_PASSWORD_EMAIL] Temporary password email sent to account ID {}.", issueResult.accountId());
        } catch (Exception exception) {
            log.error("[TEMP_PASSWORD_EMAIL] Could not send temporary password email for account ID {}: {}",
                    issueResult.accountId(), exception.getMessage(), exception);
        }
    }

    static String buildTemporaryPasswordEmailHtml(
            String email,
            String temporaryPassword,
            String loginUrl,
            String activationUrl) {
        String safeEmail = HtmlUtils.htmlEscape(email);
        String safePassword = HtmlUtils.htmlEscape(temporaryPassword);
        String safeLoginUrl = HtmlUtils.htmlEscape(loginUrl);
        String safeActivationUrl = HtmlUtils.htmlEscape(activationUrl);

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"><title>Mật khẩu tạm thời - SMASH VN</title></head>
                <body style="margin:0;padding:24px;background:#f5f5f5;font-family:Arial,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background:#fff;border-top:4px solid #ff4500;border-radius:10px;">
                        <tr><td style="padding:30px 36px;">
                          <h1 style="margin:0 0 18px;font-size:24px;color:#111827;">SMASH VN đã tạo tài khoản cho bạn</h1>
                          <p style="line-height:1.7;margin:0 0 18px;">Đơn hàng đủ điều kiện đầu tiên của bạn đã được ghi nhận. Đây là thông tin đăng nhập tạm thời:</p>
                          <div style="padding:18px;background:#fff7ed;border:1px solid #fed7aa;border-radius:8px;margin-bottom:18px;">
                            <p style="margin:0 0 10px;"><strong>Email đăng nhập:</strong> %s</p>
                            <p style="margin:0;"><strong>Mật khẩu tạm thời:</strong> <span style="font-family:monospace;font-size:18px;letter-spacing:1px;color:#c2410c;">%s</span></p>
                          </div>
                          <p style="line-height:1.7;margin:0 0 22px;"><strong>Đây không phải mật khẩu chính thức.</strong> Khi đăng nhập bằng mật khẩu tạm, bạn bắt buộc phải tạo mật khẩu chính thức trước khi sử dụng các chức năng Member.</p>
                          <p style="text-align:center;margin:0 0 14px;"><a href="%s" style="display:inline-block;padding:13px 24px;background:#ff4500;color:#fff;text-decoration:none;border-radius:7px;font-weight:700;">ĐĂNG NHẬP</a></p>
                          <p style="text-align:center;margin:0;"><a href="%s" style="color:#ff4500;font-weight:600;">Thiết lập mật khẩu ngay bằng activation link</a></p>
                          <p style="font-size:12px;color:#6b7280;line-height:1.6;margin:24px 0 0;">Nếu email gửi mật khẩu gặp sự cố, bạn vẫn có thể dùng activation link hoặc chức năng gửi lại link thiết lập mật khẩu. SMASH VN không lưu mật khẩu tạm ở dạng đọc được.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeEmail, safePassword, safeLoginUrl, safeActivationUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
