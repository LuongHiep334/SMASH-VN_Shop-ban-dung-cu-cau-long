package com.smashvn.shop.service.admin;

import java.time.LocalDateTime;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.util.ValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKhachHangService {

    private static final String MSG_DUPLICATE_EMAIL = "Email đã được sử dụng trong hệ thống.";
    private static final String MSG_DUPLICATE_PHONE = "Số điện thoại đã được sử dụng.";

    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuditService auditService;

    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
    }

    private String formatState(KhachHang kh, TaiKhoan tk) {
        if (kh == null || tk == null) {
            return "";
        }
        return String.format("id=%s, email=%s, hoTenKh=%s, soDienThoaiKh=%s, vaiTro=%s, trangThai=%s",
                kh.getId() != null ? kh.getId().toString() : "null",
                ValidationUtils.maskEmail(tk.getUsername()),
                kh.getHoTenKh(),
                ValidationUtils.maskPhone(kh.getSoDienThoaiKh()),
                tk.getVaiTro(),
                tk.getTrangThai());
    }

    private String mapDataIntegrityMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                message += " " + cause.getMessage();
            }
            cause = cause.getCause();
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("uk_username") || lowerMessage.contains("username") || lowerMessage.contains("email")) {
            return MSG_DUPLICATE_EMAIL;
        }
        if (lowerMessage.contains("so_dien_thoai") || lowerMessage.contains("sodienthoai")) {
            return MSG_DUPLICATE_PHONE;
        }
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("unique") || lowerMessage.contains("constraint")) {
            return "Dữ liệu đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.";
        }
        return "Không thể lưu thông tin khách hàng. Vui lòng kiểm tra lại dữ liệu.";
    }

    private TaiKhoan saveTaiKhoan(TaiKhoan taiKhoan) {
        try {
            return taiKhoanRepository.saveAndFlush(taiKhoan);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(mapDataIntegrityMessage(ex), ex);
        }
    }

    private KhachHang saveKhachHang(KhachHang khachHang) {
        try {
            return khachHangRepository.saveAndFlush(khachHang);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(mapDataIntegrityMessage(ex), ex);
        }
    }

    private void guiEmailCanhBaoMatKhau(String email, String hoTenKh) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email.trim());
            message.setSubject("[SmashVN] Cảnh báo bảo mật: Mật khẩu tài khoản của bạn đã được thay đổi");

            String nameDisplay = (hoTenKh != null && !hoTenKh.isBlank()) ? hoTenKh.trim() : "Khách hàng";
            String content = String.format(
                    "Xin chào %s,\n\n"
                    + "Mật khẩu tài khoản SmashVN của bạn (%s) đã được quản trị viên đặt lại vào lúc %s.\n\n"
                    + "Nếu bạn KHÔNG yêu cầu thao tác này, vui lòng liên hệ ngay với bộ phận hỗ trợ hoặc quản trị viên hệ thống SmashVN để đảm bảo an toàn cho tài khoản.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ Hỗ trợ SmashVN",
                    nameDisplay, email, LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            // Ghi log lỗi gửi mail nhưng không làm ngắt giao dịch CSDL đã lưu thành công
            log.error("Không thể gửi email cảnh báo đổi mật khẩu tới {}: {}", email, e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public KhachHang createKhachHang(String email, String matKhau, String hoTenKh, String soDienThoaiKh, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedEmail = (email == null) ? "" : email.trim();
        String sanitizedEmail = Jsoup.clean(trimmedEmail, Safelist.none());

        String trimmedName = (hoTenKh == null) ? "" : hoTenKh.trim();
        String sanitizedName = Jsoup.clean(trimmedName, Safelist.none());

        String trimmedPhone = (soDienThoaiKh == null) ? "" : soDienThoaiKh.trim();

        // 1. Validation
        if (sanitizedEmail.isEmpty() && trimmedPhone.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Email hoặc Số điện thoại để tạo tài khoản!");
        }

        String usernameToUse = "";
        if (!sanitizedEmail.isEmpty()) {
            if (!sanitizedEmail.matches(ValidationUtils.EMAIL_REGEX)) {
                throw new IllegalArgumentException("Email không đúng định dạng!");
            }
            usernameToUse = sanitizedEmail;
        } else {
            String normalizedPhone = com.smashvn.shop.util.PhoneUtils.normalize(trimmedPhone);
            if (!com.smashvn.shop.util.PhoneUtils.isValid(normalizedPhone)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam!");
            }
            usernameToUse = normalizedPhone;
        }

        if (matKhau == null || matKhau.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống!");
        }
        String trimmedPassword = matKhau.trim();
        if (trimmedPassword.length() < 8 || trimmedPassword.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu phải dài từ 8 đến 50 ký tự!");
        }

        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên khách hàng không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new IllegalArgumentException("Họ và tên phải từ 2 đến 100 ký tự!");
        }

        if (!trimmedPhone.isEmpty() && !trimmedPhone.matches(ValidationUtils.PHONE_REGEX)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam!");
        }

        // Check duplicate username
        if (taiKhoanRepository.existsByUsername(usernameToUse)) {
            throw new IllegalArgumentException("Tên đăng nhập (" + usernameToUse + ") đã tồn tại trong hệ thống!");
        }

        // 2. Tạo TaiKhoan mới
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(usernameToUse);
        tk.setMatKhau(passwordEncoder.encode(trimmedPassword));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setSoLanMuaThanhCong(0);
        tk.setSoLanNhacNhoViPham(0);
        tk.setNgayTao(LocalDateTime.now());
        tk = saveTaiKhoan(tk);

        // 3. Tạo KhachHang mới
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoTenKh(sanitizedName);
        kh.setSoDienThoaiKh(trimmedPhone);
        kh.setNgayTao(LocalDateTime.now());
        kh = saveKhachHang(kh);

        // 4. Audit Log
        if (actingTaiKhoanId != null) {
            TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
            String role = actingUser != null ? actingUser.getVaiTro() : "ADMIN";
            auditService.log(actingTaiKhoanId, "KhachHang", kh.getId().longValue(), "INSERT", null, formatState(kh, tk), ipAddress, "Tạo mới tài khoản khách hàng: " + ValidationUtils.maskEmail(sanitizedEmail), role);
        }

        return kh;
    }

    @Transactional(rollbackFor = Exception.class)
    public KhachHang updateKhachHang(Integer idKhachHang, String hoTenKh, String soDienThoaiKh, String trangThai, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedName = (hoTenKh == null) ? "" : hoTenKh.trim();
        String sanitizedName = Jsoup.clean(trimmedName, Safelist.none());

        String trimmedPhone = (soDienThoaiKh == null) ? "" : soDienThoaiKh.trim();

        // 1. Validation
        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên khách hàng không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new IllegalArgumentException("Họ và tên phải từ 2 đến 100 ký tự!");
        }

        if (!trimmedPhone.isEmpty() && !trimmedPhone.matches(ValidationUtils.PHONE_REGEX)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam!");
        }

        KhachHang kh = findById(idKhachHang);
        TaiKhoan tk = kh.getTaiKhoan();

        String oldStateStr = formatState(kh, tk);

        // 2. Cập nhật KhachHang
        kh.setHoTenKh(sanitizedName);
        kh.setSoDienThoaiKh(trimmedPhone);
        kh.setNgayCapNhat(LocalDateTime.now());
        saveKhachHang(kh);

        // 3. Cập nhật TaiKhoan
        if (tk != null) {
            if (trangThai != null && !trangThai.isBlank()) {
                tk.setTrangThai(trangThai);
            }
            tk.setNgayCapNhat(LocalDateTime.now());
            saveTaiKhoan(tk);
        }

        // 4. Audit Log
        if (actingTaiKhoanId != null) {
            TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
            String role = actingUser != null ? actingUser.getVaiTro() : "ADMIN";
            String note = "Cập nhật thông tin khách hàng: " + (tk != null ? ValidationUtils.maskEmail(tk.getUsername()) : "ID " + idKhachHang);
            auditService.log(actingTaiKhoanId, "KhachHang", kh.getId().longValue(), "UPDATE", oldStateStr, formatState(kh, tk), ipAddress, note, role);
        }

        return kh;
    }
}
