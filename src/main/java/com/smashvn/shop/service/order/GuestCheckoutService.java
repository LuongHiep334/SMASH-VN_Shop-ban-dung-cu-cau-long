package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.util.PhoneUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCheckoutService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final JavaMailSender mailSender;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public String checkEmailStatus(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "NEW";
        }
        
        TaiKhoan tk = taiKhoanRepository.findByUsername(email.trim());
        if (tk == null) {
            return "NEW";
        }

        // Chặn tài khoản bị khóa hoặc chờ khóa
        if (tk.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || tk.getTrangThaiTaiKhoan() == AccountStatus.PENDING_LOCK
                || "bi_khoa".equals(tk.getTrangThai()) || "cho_khoa".equals(tk.getTrangThai())) {
            log.warn("[GUEST_CHECKOUT] Blocked locked account attempt for email: {}", email);
            return "LOCKED";
        }

        if (tk.getTrangThaiTaiKhoan() == AccountStatus.ACTIVE) {
            return "ACTIVE";
        }

        if (tk.getTrangThaiTaiKhoan() == AccountStatus.GUEST) {
            return hasText(tk.getMatKhau())
                    ? "GUEST_WITH_TEMP_PASSWORD"
                    : "GUEST_NO_PASSWORD";
        }

        return tk.getTrangThaiTaiKhoan().name();
    }

    public static class GuestRegisterResult {
        private final TaiKhoan taiKhoan;
        private final String token;
        private final boolean newAccount;

        public GuestRegisterResult(TaiKhoan taiKhoan, String token) {
            this(taiKhoan, token, false);
        }

        public GuestRegisterResult(TaiKhoan taiKhoan, String token, boolean newAccount) {
            this.taiKhoan = taiKhoan;
            this.token = token;
            this.newAccount = newAccount;
        }

        public TaiKhoan getTaiKhoan() {
            return taiKhoan;
        }

        public String getToken() {
            return token;
        }

        public boolean isNewAccount() {
            return newAccount;
        }
    }

    @Transactional
    public GuestRegisterResult autoRegisterGuest(String hoTen, String soDienThoai, String email) {
        String trimmedEmail = (email != null) ? email.trim() : "";
        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        // Normalize phone number
        String normalizedPhone = PhoneUtils.normalize(soDienThoai);
        if (!normalizedPhone.isEmpty()) {
            if (!PhoneUtils.isValid(normalizedPhone)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
            }
        }

        TaiKhoan existingTk = taiKhoanRepository.findByUsername(trimmedEmail);
        if (existingTk != null) {
            log.info("[GUEST_CHECKOUT] Linked order with existing guest account: {}", trimmedEmail);

            // An toàn dữ liệu: KHÔNG ghi đè họ tên, số điện thoại của hồ sơ KhachHang cũ
            // Tìm hoặc sinh token GUEST_ACTIVATION duy nhất còn hiệu lực
            List<TokenKhoiPhuc> activeTokens = tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(existingTk.getId(), "GUEST_ACTIVATION");
            String tokenToUse = null;
            LocalDateTime now = LocalDateTime.now();
            for (TokenKhoiPhuc t : activeTokens) {
                if (t.getThoiGianHetHan() != null && t.getThoiGianHetHan().isAfter(now)) {
                    tokenToUse = t.getMaXacNhan();
                    break;
                } else {
                    t.setDaSuDung(true);
                }
            }

            if (tokenToUse == null) {
                // Vô hiệu hóa token cũ
                for (TokenKhoiPhuc t : activeTokens) {
                    t.setDaSuDung(true);
                }
                if (!activeTokens.isEmpty()) {
                    tokenRepository.saveAll(activeTokens);
                }

                String newToken = UUID.randomUUID().toString();
                TokenKhoiPhuc tkp = new TokenKhoiPhuc();
                tkp.setTaiKhoan(existingTk);
                tkp.setMaXacNhan(newToken);
                tkp.setLoaiXacNhan("GUEST_ACTIVATION");
                tkp.setThoiGianHetHan(LocalDateTime.now().plusDays(30));
                tkp.setDaSuDung(false);
                tokenRepository.save(tkp);
                tokenToUse = newToken;
            }

            return new GuestRegisterResult(existingTk, tokenToUse, false);
        }

        // Check duplicate phone conflict for new registration against all customers
        if (!normalizedPhone.isEmpty()) {
            KhachHang otherKh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
            if (otherKh != null) {
                throw new IllegalArgumentException("Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
            }
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(trimmedEmail);
        tk.setMatKhau(null); // No password initially
        tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        tk.setSoLanMuaThanhCong(0); // Will be incremented when order is created
        tk.setVaiTro("KH");

        TaiKhoan savedTk = taiKhoanRepository.save(tk);

        String ho = "Khách";
        String ten = "Vãng Lai";
        if (hoTen != null && !hoTen.trim().isEmpty()) {
            String name = hoTen.trim();
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace >= 0) {
                ho = name.substring(0, lastSpace).trim();
                ten = name.substring(lastSpace + 1).trim();
            } else {
                ho = "";
                ten = name;
            }
        }

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(savedTk);
        kh.setHoKh(ho);
        kh.setTenKh(ten);
        kh.setSoDienThoaiKh(normalizedPhone);
        kh.setNhanBanTin(false);

        khachHangRepository.save(kh);
        
        // Generate and save token synchronously in the same transaction với loại GUEST_ACTIVATION
        String token = UUID.randomUUID().toString();
        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(savedTk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("GUEST_ACTIVATION");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusDays(30)); // 30 days validation limit
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        // Tạo thông báo hệ thống cho tài khoản vãng lai được khởi tạo tự động
        try {
            ThongBao thongBaoAcc = ThongBao.builder()
                    .taiKhoan(savedTk)
                    .tieuDe("Tài khoản của bạn đã được khởi tạo tự động")
                    .noiDung("Cảm ơn bạn đã mua sắm tại Smash VN! Tài khoản của bạn đã được hệ thống tự động khởi tạo theo email " 
                            + trimmedEmail + ". Vui lòng kiểm tra hộp thư email của bạn để nhận đường dẫn kích hoạt và thiết lập lại mật khẩu.")
                    .daDoc(false)
                    .loaiThongBao("tai_khoan")
                    .ngayTao(LocalDateTime.now())
                    .build();
            thongBaoRepository.save(thongBaoAcc);
            log.info("[GUEST_CHECKOUT] Created system notification for auto-registered account ID {}", savedTk.getId());
        } catch (Exception e) {
            log.error("[GUEST_CHECKOUT] Failed to create system notification for account ID {}: {}", savedTk.getId(), e.getMessage());
        }

        log.info("[GUEST_CHECKOUT] Auto-registered GUEST account & generated activation token: {}", trimmedEmail);

        return new GuestRegisterResult(savedTk, token, true);
    }

    @Transactional
    public void resendGuestActivationEmail(Integer idTaiKhoan, String appUrl) {
        if (idTaiKhoan == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin tài khoản");
        }
        TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        if (tk.getTrangThaiTaiKhoan() != AccountStatus.GUEST) {
            throw new IllegalStateException("Tài khoản này đã được kích hoạt hoặc không phải tài khoản khách vãng lai.");
        }

        // Revoke active old GUEST_ACTIVATION tokens
        List<TokenKhoiPhuc> activeTokens = tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(tk.getId(), "GUEST_ACTIVATION");
        for (TokenKhoiPhuc oldTok : activeTokens) {
            oldTok.setDaSuDung(true);
            tokenRepository.save(oldTok);
        }

        // Generate new token (30 days)
        String token = UUID.randomUUID().toString();
        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(tk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("GUEST_ACTIVATION");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusDays(30));
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        // Send email asynchronously
        sendOrderAndAccountNotification(tk.getUsername(), token, appUrl);
        log.info("[GUEST_CHECKOUT] Resent GUEST_ACTIVATION email to: {}", tk.getUsername());
    }

    @Transactional
    public void incrementPurchaseCount(Integer idTaiKhoan) {
        if (idTaiKhoan == null) return;
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(idTaiKhoan).orElse(null);
        if (tk == null) return;
        int current = (tk.getSoLanMuaThanhCong() != null) ? tk.getSoLanMuaThanhCong() : 0;
        tk.setSoLanMuaThanhCong(current + 1);
        taiKhoanRepository.save(tk);
        log.info("[GUEST_CHECKOUT] Incremented successful purchase count for TaiKhoan ID {} to {}.", idTaiKhoan, tk.getSoLanMuaThanhCong());
    }

    @org.springframework.scheduling.annotation.Async
    public void sendOrderAndAccountNotification(String recipientEmail, String token, String appUrl) {
        long startEmailThread = System.currentTimeMillis();
        log.info("[EmailService] Starting asynchronous email sending process.");
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.warn("[EmailService] Recipient email is null or empty, skipping email notification.");
            return;
        }

        if (recipientEmail.contains("test") || recipientEmail.contains("tester") || recipientEmail.endsWith("@smashvn.com") || recipientEmail.endsWith("@example.com")) {
            log.info("[TEST] Skipping sending order and account notification email for test recipient: {}", recipientEmail);
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            log.warn("[EmailService] Activation token is null or empty, skipping email notification.");
            return;
        }

        String activationUrl = appUrl + "/user/thiet-lap-mat-khau?token=" + token;

        String htmlMsg = buildGuestActivationEmailHtml(activationUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("[Smash VN] Thiết lập mật khẩu và kích hoạt tài khoản");
            helper.setText(htmlMsg, true);
            helper.addInline("smashLogo", new org.springframework.core.io.ClassPathResource("static/images/logo/logo-2.png"));

            mailSender.send(message);
            long endEmailThread = System.currentTimeMillis();
            log.info("[EmailService] HTML Email sent successfully in {}ms to {}", (endEmailThread - startEmailThread), recipientEmail);
        } catch (Exception e) {
            long endEmailThread = System.currentTimeMillis();
            log.error("[EmailService] Failed to send email in {}ms to {}. Exception: {}", (endEmailThread - startEmailThread), recipientEmail, e.getMessage(), e);
            // Do not fail order checkout if mail sending fails
        }
    }

    static String buildGuestActivationEmailHtml(String activationUrl) {
        String safeActivationUrl = org.springframework.web.util.HtmlUtils.htmlEscape(activationUrl);

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <title>Thiết lập mật khẩu tài khoản - Smash VN</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f5f5f5; font-family: 'Open Sans', Arial, sans-serif; color: #333333; -webkit-font-smoothing: antialiased;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="width: 100%; background-color: #f5f5f5;">
                        <tr>
                            <td align="center" style="padding: 28px 12px;">
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="600" style="width: 100%; max-width: 600px; background-color: #ffffff; border: 1px solid #e8e8e8; border-top: 4px solid #ff4500; border-radius: 10px; overflow: hidden;">
                                    <tr>
                                        <td align="center" style="padding: 16px 32px 14px; background-color: #ffffff; border-bottom: 1px solid #eeeeee;">
                                            <img src="cid:smashLogo" width="75" alt="SMASH VN" style="display: block; width: 75px; max-width: 75px; height: auto; border: 0;">
                                            <p style="margin: 5px 0 0; color: #777777; font-size: 10px; font-weight: 600; line-height: 1.4; letter-spacing: 1.2px; text-transform: uppercase;">
                                                Cửa hàng vợt cầu lông chính hãng
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 30px 36px 18px;">
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" width="44" height="44" style="width: 44px; height: 44px; border-radius: 50%; background-color: #fff0eb; color: #ff4500; font-size: 24px; font-weight: 700; line-height: 44px;">
                                                        &#10003;
                                                    </td>
                                                </tr>
                                            </table>
                                            <h1 style="margin: 16px 0 0; color: #15171c; font-size: 24px; font-weight: 700; line-height: 1.35;">
                                                Đặt hàng thành công!
                                            </h1>
                                            <p style="margin: 10px 0 0; color: #666666; font-size: 14px; line-height: 1.7;">
                                                Cảm ơn bạn đã mua sắm tại <strong style="color: #15171c;">Smash VN</strong>.<br>
                                                Đơn hàng của bạn đã được ghi nhận trên hệ thống.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 0 36px 24px;">
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="width: 100%; background-color: #fff7f3; border: 1px solid #ffd8ca; border-left: 4px solid #ff4500; border-radius: 8px;">
                                                <tr>
                                                    <td style="padding: 18px 20px;">
                                                        <p style="margin: 0 0 5px; color: #ff4500; font-size: 11px; font-weight: 700; line-height: 1.4; letter-spacing: 0.8px; text-transform: uppercase;">
                                                            Tài khoản khách hàng
                                                        </p>
                                                        <h2 style="margin: 0 0 8px; color: #15171c; font-size: 17px; font-weight: 700; line-height: 1.4;">
                                                            Thiết lập mật khẩu lần đầu
                                                        </h2>
                                                        <p style="margin: 0; color: #5f5f5f; font-size: 13px; line-height: 1.65;">
                                                            Smash VN đã tạo tài khoản theo email này để bạn lưu lịch sử mua hàng và theo dõi đơn thuận tiện hơn. Hãy tạo mật khẩu để hoàn tất kích hoạt tài khoản.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 0 36px 32px;">
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" bgcolor="#ff4500" style="background-color: #ff4500; border-radius: 6px;">
                                                        <a href="{{ACTIVATION_URL}}" target="_blank" style="display: inline-block; padding: 14px 30px; color: #ffffff; font-size: 13px; font-weight: 700; line-height: 1.2; letter-spacing: 0.5px; text-decoration: none; text-transform: uppercase;">
                                                            Thiết lập mật khẩu &nbsp;&rarr;
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 14px 0 0; color: #999999; font-size: 11px; line-height: 1.5;">
                                                Liên kết có hiệu lực trong 30 ngày.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 18px 36px; background-color: #fafafa; border-top: 1px solid #eeeeee;">
                                            <p style="margin: 0 0 6px; color: #777777; font-size: 11px; font-weight: 600; line-height: 1.5;">
                                                Nếu nút trên không hoạt động, hãy mở đường dẫn này:
                                            </p>
                                            <p style="margin: 0; font-size: 11px; line-height: 1.5; word-break: break-all;">
                                                <a href="{{ACTIVATION_URL}}" style="color: #ff4500; text-decoration: underline;">{{ACTIVATION_URL}}</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 22px 32px; background-color: #000000;">
                                            <p style="margin: 0 0 5px; color: #ffffff; font-size: 12px; font-weight: 700; line-height: 1.5; letter-spacing: 0.3px;">
                                                SMASH VN
                                            </p>
                                            <p style="margin: 0; color: #9b9b9b; font-size: 10px; line-height: 1.5;">
                                                Cửa hàng vợt cầu lông chính hãng &nbsp;&bull;&nbsp; &copy; 2026 Smash VN
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.replace("{{ACTIVATION_URL}}", safeActivationUrl);
    }

    @Transactional
    public void setPasswordForGuest(Integer idTaiKhoan, String password) {
        // Concurrency lock: Load TaiKhoan with Pessimistic Write Lock
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(idTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Chặn tài khoản bị khóa hoặc chờ khóa
        if (tk.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || tk.getTrangThaiTaiKhoan() == AccountStatus.PENDING_LOCK
                || "bi_khoa".equals(tk.getTrangThai()) || "cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản này đã bị khóa hoặc đang chờ khóa. Vui lòng liên hệ quản trị viên!");
        }

        // GUEST + password là mật khẩu tạm; chỉ thao tác này hoặc activation link mới được ACTIVE.
        if (tk.getTrangThaiTaiKhoan() != AccountStatus.GUEST) {
            throw new IllegalStateException("Tài khoản đã được kích hoạt trước đó.");
        }

        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống!");
        }
        if (password.length() < 8 || password.length() > 30) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 30 ký tự!");
        }
        if (password.contains(" ") || password.contains("\t") || password.contains("\n") || password.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            throw new RuntimeException("Mật khẩu phải chứa cả chữ và số!");
        }
        if (hasText(tk.getMatKhau()) && passwordEncoder.matches(password, tk.getMatKhau())) {
            throw new RuntimeException("Mật khẩu chính thức phải khác mật khẩu tạm thời!");
        }

        tk.setMatKhau(passwordEncoder.encode(password));
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        taiKhoanRepository.saveAndFlush(tk);

        List<TokenKhoiPhuc> activeTokens = tokenRepository
                .findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(tk.getId(), "GUEST_ACTIVATION");
        for (TokenKhoiPhuc activeToken : activeTokens) {
            activeToken.setDaSuDung(true);
        }
        if (!activeTokens.isEmpty()) {
            tokenRepository.saveAll(activeTokens);
        }

        log.info("[GUEST_CHECKOUT] Upgraded guest account ID {} to ACTIVE status and saved password.", idTaiKhoan);
    }

    @Transactional
    public void setPasswordByToken(String token, String password) {
        TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
        if (tkp == null) {
            throw new RuntimeException("Đường link thiết lập mật khẩu không hợp lệ!");
        }
        if (!"GUEST_ACTIVATION".equals(tkp.getLoaiXacNhan())) {
            throw new RuntimeException("Đường link thiết lập mật khẩu không hợp lệ hoặc sai loại xác nhận!");
        }
        if (tkp.isDaSuDung()) {
            throw new RuntimeException("Đường link này đã được sử dụng!");
        }
        if (tkp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đường link này đã hết hạn!");
        }

        TaiKhoan tk = tkp.getTaiKhoan();
        setPasswordForGuest(tk.getId(), password);

        tkp.setDaSuDung(true);
        tokenRepository.saveAndFlush(tkp);

        // Vô hiệu hóa tất cả token GUEST_ACTIVATION còn lại của tài khoản này
        List<TokenKhoiPhuc> otherTokens = tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(tk.getId(), "GUEST_ACTIVATION");
        for (TokenKhoiPhuc ot : otherTokens) {
            ot.setDaSuDung(true);
        }
        if (!otherTokens.isEmpty()) {
            tokenRepository.saveAll(otherTokens);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @org.springframework.scheduling.annotation.Async
    public void sendOrderConfirmationEmail(String recipientEmail, com.smashvn.shop.entity.HoaDon hd, String appUrl) {
        long startEmailThread = System.currentTimeMillis();
        log.info("[EmailService] Starting asynchronous order confirmation email sending process.");
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.warn("[EmailService] Recipient email is null or empty, skipping order confirmation email.");
            return;
        }

        String maDonHang = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
        
        // Skip real emails in test environment
        if (maDonHang.startsWith("TEST-") || recipientEmail.contains("test") || recipientEmail.contains("tester") || recipientEmail.endsWith("@smashvn.com") || recipientEmail.endsWith("@example.com")) {
            log.info("[TEST] Skipping sending order confirmation email for test order: {} to {}", maDonHang, recipientEmail);
            return;
        }

        String tenNguoiNhan = hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Quý khách";
        String sdt = hd.getSdtNhan() != null ? hd.getSdtNhan() : "N/A";
        String diaChi = hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "N/A";
        String phuongThuc = hd.getPaymentMethod() != null ? hd.getPaymentMethod() : (hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "N/A");
        String formattedTongTien = hd.getTongTien() != null ? String.format("%,.0f", hd.getTongTien()) : "0";
        String trackingUrl = appUrl + "/user/track-order?id=" + maDonHang;

        // Query product list for invoice email
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
        StringBuilder itemsHtml = new StringBuilder();
        BigDecimal tamTinh = BigDecimal.ZERO;
        if (items != null) {
            for (HoaDonChiTiet ct : items) {
                String tenSp = ct.getTenSanPhamSnapshot() != null ? ct.getTenSanPhamSnapshot() : (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "N/A");
                String thuocTinh = (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getPhanLoaiHienThi() != null)
                        ? ct.getSanPhamChiTiet().getPhanLoaiHienThi()
                        : (ct.getThuocTinhSnapshot() != null ? ct.getThuocTinhSnapshot() : "Mặc định");
                int sl = ct.getSoLuong();
                BigDecimal donGia = ct.getDonGia() != null ? ct.getDonGia() : BigDecimal.ZERO;
                BigDecimal thanhTien = donGia.multiply(new BigDecimal(sl));
                tamTinh = tamTinh.add(thanhTien);
                
                itemsHtml.append("<tr style=\"border-bottom: 1px solid #f3f4f6;\">")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: left;\">")
                    .append("    <strong style=\"color: #1f2937; font-size: 14px;\">").append(tenSp).append("</strong><br>")
                    .append("    <span style=\"color: #6b7280; font-size: 12px;\">").append(thuocTinh).append("</span>")
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: center; color: #4b5563; font-size: 14px;\">")
                    .append(sl)
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: right; color: #4b5563; font-size: 14px;\">")
                    .append(String.format("%,.0f", donGia)).append(" đ")
                    .append("  </td>")
                    .append("  <td style=\"padding: 12px 8px; vertical-align: middle; text-align: right; color: #111827; font-weight: 600; font-size: 14px;\">")
                    .append(String.format("%,.0f", thanhTien)).append(" đ")
                    .append("  </td>")
                    .append("</tr>");
            }
        }

        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal voucherGiam = hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : BigDecimal.ZERO;

        String detailsHtml = "";
        if (hd.getMaGiaoDich() != null && !hd.getMaGiaoDich().isEmpty()) {
            detailsHtml += "                    <tr>" +
                    "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Mã giao dịch:</td>" +
                    "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + hd.getMaGiaoDich() + "</td>" +
                    "                    </tr>";
        } else if (hd.getTransactionId() != null && !hd.getTransactionId().isEmpty()) {
            detailsHtml += "                    <tr>" +
                    "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Transaction ID:</td>" +
                    "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + hd.getTransactionId() + "</td>" +
                    "                    </tr>";
        }

        String htmlMsg = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"utf-8\">" +
                "    <title>Xác nhận đặt hàng thành công</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">" +
                "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 650px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">" +
                "        <tr>" +
                "            <td style=\"padding: 32px 40px; background-color: #e02424; text-align: center;\">" +
                "                <h2 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;\">SMASH VN</h2>" +
                "                <p style=\"margin: 4px 0 0 0; color: #fecaca; font-size: 14px;\">Cảm ơn bạn đã mua sắm tại cửa hàng của chúng tôi!</p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 40px;\">" +
                "                <p style=\"margin: 0 0 16px 0; color: #111827; font-size: 18px; font-weight: 600;\">Xin chào " + tenNguoiNhan + ",</p>" +
                "                <p style=\"margin: 0 0 24px 0; color: #4b5563; font-size: 16px; line-height: 1.6;\">" +
                "                    Đơn hàng của bạn đã được ghi nhận thành công. Dưới đây là thông tin chi tiết hóa đơn đơn hàng của bạn:" +
                "                </p>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f9fafb; border-radius: 12px; padding: 20px; margin-bottom: 24px; border: 1px solid #f3f4f6;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; width: 160px; font-size: 15px;\">Mã đơn hàng:</td>" +
                "                        <td style=\"padding: 8px 0; color: #e02424; font-size: 15px; font-weight: 700;\">" + maDonHang + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Số điện thoại:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + sdt + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Địa chỉ nhận hàng:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px; line-height: 1.4;\">" + diaChi + "</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 8px 0; color: #374151; font-weight: 600; font-size: 15px;\">Thanh toán:</td>" +
                "                        <td style=\"padding: 8px 0; color: #111827; font-size: 15px;\">" + phuongThuc + "</td>" +
                "                    </tr>" +
                detailsHtml +
                "                </table>" +
                "                <h3 style=\"color: #111827; font-size: 16px; font-weight: 700; margin-bottom: 12px; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px;\">CHI TIẾT SẢN PHẨM</h3>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"border-collapse: collapse; margin-bottom: 24px;\">" +
                "                    <thead>" +
                "                        <tr style=\"background-color: #f3f4f6; border-bottom: 2px solid #e5e7eb;\">" +
                "                            <th style=\"padding: 8px; text-align: left; font-size: 12px; font-weight: 600; color: #374151;\">Sản phẩm</th>" +
                "                            <th style=\"padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: #374151; width: 40px;\">SL</th>" +
                "                            <th style=\"padding: 8px; text-align: right; font-size: 12px; font-weight: 600; color: #374151; width: 90px;\">Đơn giá</th>" +
                "                            <th style=\"padding: 8px; text-align: right; font-size: 12px; font-weight: 600; color: #374151; width: 100px;\">Thành tiền</th>" +
                "                        </tr>" +
                "                    </thead>" +
                "                    <tbody>" +
                itemsHtml.toString() +
                "                    </tbody>" +
                "                </table>" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"right\" width=\"300px\" style=\"margin-bottom: 30px; font-size: 14px;\">" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Tạm tính:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #111827; font-weight: 600;\">" + String.format("%,.0f", tamTinh) + " đ</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Phí vận chuyển:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #111827; font-weight: 600;\">" + String.format("%,.0f", phiShip) + " đ</td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding: 6px 0; color: #4b5563;\">Voucher giảm giá:</td>" +
                "                        <td style=\"padding: 6px 0; text-align: right; color: #10b981; font-weight: 600;\">-" + String.format("%,.0f", voucherGiam) + " đ</td>" +
                "                    </tr>" +
                "                    <tr style=\"border-top: 2px solid #e5e7eb;\">" +
                "                        <td style=\"padding: 12px 0; color: #111827; font-weight: 700; font-size: 16px;\">Tổng thanh toán:</td>" +
                "                        <td style=\"padding: 12px 0; text-align: right; color: #e02424; font-size: 18px; font-weight: 700;\">" + formattedTongTien + " đ</td>" +
                "                    </tr>" +
                "                </table>" +
                "                <div style=\"clear: both;\"></div>" +
                "                <p style=\"margin: 20px 0 24px 0; color: #4b5563; font-size: 15px; line-height: 1.6;\">" +
                "                    Bạn có thể sử dụng mã đơn hàng trên để theo dõi hành trình giao nhận hàng trực tiếp tại trang web của chúng tôi bằng nút bên dưới:" +
                "                </p>" +
                "                <div style=\"text-align: center; margin-bottom: 30px;\">" +
                "                    <a href=\"" + trackingUrl + "\" style=\"display: inline-block; padding: 14px 30px; background-color: #e02424; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; box-shadow: 0 4px 6px rgba(224, 36, 36, 0.2);\">" +
                "                        Theo Dõi Đơn Hàng" +
                "                    </a>" +
                "                </div>" +
                "                <p style=\"margin: 0; color: #6b7280; font-size: 14px; line-height: 1.5;\">" +
                "                    Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ hotline bộ phận CSKH của chúng tôi để được giải đáp sớm nhất." +
                "                </p>" +
                "            </td>" +
                "        </tr>" +
                "        <tr>" +
                "            <td style=\"padding: 24px; background-color: #f9fafb; text-align: center; border-top: 1px solid #f3f4f6;\">" +
                "                <p style=\"margin: 0; color: #9ca3af; font-size: 12px;\">Hệ thống Cửa hàng Smash VN &copy; 2026</p>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("[Smash VN] Xác nhận đặt hàng thành công - Đơn hàng " + maDonHang);
            helper.setText(htmlMsg, true);
            mailSender.send(message);
            long endEmailThread = System.currentTimeMillis();
            log.info("[EmailService] Order confirmation email sent successfully in {}ms to {}", (endEmailThread - startEmailThread), recipientEmail);
        } catch (Exception e) {
            long endEmailThread = System.currentTimeMillis();
            log.error("[EmailService] Failed to send order confirmation email in {}ms to {}. Exception: {}", (endEmailThread - startEmailThread), recipientEmail, e.getMessage(), e);
        }
    }
}
