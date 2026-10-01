package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.AuditService;

import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNhanVienService {

    private static final String EMPLOYEE_PHONE_REGEX = "^(03|05|07|08|09)\\d{8}$";
    private static final String MSG_DUPLICATE_EMPLOYEE_PHONE = "Số điện thoại nhân viên đã tồn tại. Vui lòng nhập số khác.";
    private static final String MSG_DUPLICATE_EMAIL = "Email đã được sử dụng.";
    private static final String MSG_DUPLICATE_USERNAME = "Tên đăng nhập đã tồn tại.";
    private static final String MSG_DUPLICATE_EMPLOYEE_ACCOUNT = "Tài khoản nhân viên đã tồn tại.";
    private static final long LOCK_TOKEN_TTL_SECONDS = 24 * 60 * 60;
    private static final long LOCK_TOKEN_CLOCK_SKEW_SECONDS = 5 * 60;

    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final JavaMailSender mailSender;
    private final KhachHangRepository khachHangRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final org.springframework.cache.CacheManager cacheManager;

    @Value("${app.admin.emails:}")
    private String adminEmailsConfig;

    public static String[] splitFullName(String fullName) {
        if (fullName == null) {
            return new String[]{"", ""};
        }
        String hoTen = fullName.trim();
        if (hoTen.isEmpty()) {
            return new String[]{"", ""};
        }
        int lastSpaceIndex = hoTen.lastIndexOf(' ');
        if (lastSpaceIndex == -1) {
            return new String[]{"", hoTen};
        }
        String hoKh = hoTen.substring(0, lastSpaceIndex).trim();
        String tenKh = hoTen.substring(lastSpaceIndex + 1).trim();
        return new String[]{hoKh, tenKh};
    }

    public List<NhanVien> getAllNhanVien() {
        return nhanVienRepository.findAll();
    }

    public List<NhanVien> searchNhanVien(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllNhanVien();
        }
        return nhanVienRepository.searchNhanVien("%" + keyword.trim() + "%");
    }

    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));
    }

    private String formatState(NhanVien nv, TaiKhoan tk) {
        if (nv == null || tk == null) {
            return "";
        }
        return String.format("id=%s, email=%s, hoTenNv=%s, chucVu=%s, soDienThoaiNv=%s, vaiTro=%s, trangThai=%s",
                nv.getId() != null ? nv.getId().toString() : "null",
                com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername()),
                nv.getHoTenNv(),
                nv.getChucVu(),
                com.smashvn.shop.util.ValidationUtils.maskPhone(nv.getSoDienThoaiNv()),
                tk.getVaiTro(),
                tk.getTrangThai());
    }

    private String createLockConfirmationToken() {
        long issuedAt = java.time.Instant.now().getEpochSecond();
        return issuedAt + "." + java.util.UUID.randomUUID();
    }

    private boolean isValidLockConfirmationToken(String providedToken, String storedToken) {
        if (providedToken == null || storedToken == null) {
            return false;
        }

        byte[] providedBytes = providedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] storedBytes = storedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!java.security.MessageDigest.isEqual(providedBytes, storedBytes)) {
            return false;
        }

        int separatorIndex = storedToken.indexOf('.');
        if (separatorIndex <= 0) {
            return false;
        }

        try {
            long issuedAt = Long.parseLong(storedToken.substring(0, separatorIndex));
            long now = java.time.Instant.now().getEpochSecond();
            return issuedAt <= now + LOCK_TOKEN_CLOCK_SKEW_SECONDS
                    && now - issuedAt <= LOCK_TOKEN_TTL_SECONDS;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private TaiKhoan getActingManager(Integer actingTaiKhoanId) {
        if (actingTaiKhoanId == null) {
            return null;
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        return actingUser != null && "QL".equals(actingUser.getVaiTro()) ? actingUser : null;
    }

    private void validatePendingLockAuthorization(
            TaiKhoan targetAccount,
            String token,
            TaiKhoan actingManager,
            String actionDescription) {
        if (!"cho_khoa".equals(targetAccount.getTrangThai())) {
            throw new IllegalStateException("Tài khoản này không ở trạng thái chờ khóa!");
        }
        if (actingManager == null && !isValidLockConfirmationToken(token, targetAccount.getTokenXacThucKhoa())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền thực hiện " + actionDescription + " tài khoản này hoặc liên kết xác nhận đã hết hạn!");
        }
    }

    @Transactional(readOnly = true)
    public NhanVien getPendingLockForConfirmation(Integer id, String token, Integer actingTaiKhoanId) {
        NhanVien nv = findById(id);
        TaiKhoan actingManager = getActingManager(actingTaiKhoanId);
        validatePendingLockAuthorization(nv.getTaiKhoan(), token, actingManager, "xác nhận khóa");
        return nv;
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
        if (lowerMessage.contains("ux_nhanvien_sodienthoai")
                || lowerMessage.contains("so_dien_thoai")
                || lowerMessage.contains("sodienthoai")) {
            return MSG_DUPLICATE_EMPLOYEE_PHONE;
        }
        if (lowerMessage.contains("uk_email")
                || lowerMessage.contains("email")
                || lowerMessage.contains("tai_khoan.email")) {
            return MSG_DUPLICATE_EMAIL;
        }
        if (lowerMessage.contains("uk_tendangnhap")
                || lowerMessage.contains("ten_dang_nhap")
                || lowerMessage.contains("tendangnhap")) {
            return MSG_DUPLICATE_USERNAME;
        }
        if (lowerMessage.contains("id_tai_khoan")
                || lowerMessage.contains("tai_khoan")
                || lowerMessage.contains("taikhoan")) {
            return MSG_DUPLICATE_EMPLOYEE_ACCOUNT;
        }
        if (lowerMessage.contains("duplicate")
                || lowerMessage.contains("unique")
                || lowerMessage.contains("constraint")) {
            return "Dữ liệu đã tồn tại. Vui lòng kiểm tra lại thông tin vừa nhập.";
        }
        return "Không thể lưu nhân viên. Vui lòng kiểm tra lại thông tin.";
    }

    private TaiKhoan saveTaiKhoan(TaiKhoan taiKhoan) {
        try {
            return taiKhoanRepository.saveAndFlush(taiKhoan);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(mapDataIntegrityMessage(ex), ex);
        }
    }

    private NhanVien saveNhanVien(NhanVien nhanVien) {
        try {
            return nhanVienRepository.saveAndFlush(nhanVien);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(mapDataIntegrityMessage(ex), ex);
        }
    }

    private KhachHang saveKhachHang(KhachHang khachHang) {
        try {
            return khachHangRepository.saveAndFlush(khachHang);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(mapDataIntegrityMessage(ex), ex);
        }
    }

    @Transactional
    public void createNhanVien(String email, String matKhau, String hoTenNv, String chucVu, String soDienThoaiNv, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedEmail = (email == null) ? "" : email.trim();
        String sanitizedEmail = org.jsoup.Jsoup.clean(trimmedEmail, org.jsoup.safety.Safelist.none());

        String trimmedName = (hoTenNv == null) ? "" : hoTenNv.trim();
        String sanitizedName = org.jsoup.Jsoup.clean(trimmedName, org.jsoup.safety.Safelist.none());

        String trimmedChucVu = (chucVu == null) ? "" : chucVu.trim();
        String sanitizedChucVu = org.jsoup.Jsoup.clean(trimmedChucVu, org.jsoup.safety.Safelist.none());

        String trimmedPhone = (soDienThoaiNv == null) ? "" : soDienThoaiNv.trim();

        if (sanitizedEmail.isEmpty()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (sanitizedEmail.length() > 100) {
            throw new RuntimeException("Email không được vượt quá 100 ký tự!");
        }
        if (!sanitizedEmail.matches(com.smashvn.shop.util.ValidationUtils.EMAIL_REGEX)) {
            throw new RuntimeException("Email không đúng định dạng!");
        }

        if (matKhau == null || matKhau.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        String trimmedPassword = matKhau.trim();
        if (trimmedPassword.length() < 8 || trimmedPassword.length() > 50) {
            throw new RuntimeException("Mật khẩu phải có độ dài từ 8 đến 50 ký tự!");
        }

        if (sanitizedName.isEmpty()) {
            throw new RuntimeException("Họ tên nhân viên không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new RuntimeException("Họ tên nhân viên phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (sanitizedChucVu.isEmpty()) {
            throw new RuntimeException("Chức vụ không được để trống!");
        }
        if (sanitizedChucVu.length() < 2 || sanitizedChucVu.length() > 50) {
            throw new RuntimeException("Chức vụ phải có độ dài từ 2 đến 50 ký tự!");
        }

        if (trimmedPhone.isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (!trimmedPhone.matches(EMPLOYEE_PHONE_REGEX)) {
            throw new RuntimeException("Số điện thoại không đúng định dạng Việt Nam. Vui lòng nhập số bắt đầu bằng 03, 05, 07, 08, 09 và đủ 10 số.");
        }

        if (nhanVienRepository.existsBySoDienThoai(trimmedPhone)) {
            throw new IllegalArgumentException(MSG_DUPLICATE_EMPLOYEE_PHONE);
        }

        // 1. Kiểm tra Email tồn tại
        if (taiKhoanRepository.existsByUsername(sanitizedEmail)) {
            throw new IllegalArgumentException(MSG_DUPLICATE_EMAIL);
        }

        // 2. Tạo TaiKhoan mới
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(sanitizedEmail);
        tk.setMatKhau(passwordEncoder.encode(trimmedPassword));
        tk.setVaiTro("NV");
        tk.setTrangThai("hoat_dong");
        tk = saveTaiKhoan(tk);

        // 3. Tạo NhanVien
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(tk);
        nv.setHoTenNv(sanitizedName);
        nv.setChucVu(sanitizedChucVu);
        nv.setSoDienThoaiNv(trimmedPhone);
        nv = saveNhanVien(nv);

        // --- NEW: Tạo KhachHang ---
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        String[] nameParts = splitFullName(sanitizedName);
        kh.setHoKh(nameParts[0]);
        kh.setTenKh(nameParts[1]);
        kh.setSoDienThoaiKh(trimmedPhone);
        kh.setNhanBanTin(false);
        kh.setLaTaiKhoanNoiBo(true); // Internal account flag
        kh = saveKhachHang(kh);

        // 4. Lưu Audit Logs
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "INSERT", null, formatState(nv, tk), ipAddress, "Tạo nhân viên mới: " + com.smashvn.shop.util.ValidationUtils.maskEmail(sanitizedEmail), actingUser.getVaiTro());
            auditService.log(actingTaiKhoanId, "KhachHang", kh.getId().longValue(), "INSERT", null, String.format("id=%s, id_tai_khoan=%s, hoKh=%s, tenKh=%s, soDienThoaiKh=%s, laTaiKhoanNoiBo=true", kh.getId(), tk.getId(), kh.getHoKh(), kh.getTenKh(), com.smashvn.shop.util.ValidationUtils.maskPhone(kh.getSoDienThoaiKh())), ipAddress, "Tạo hồ sơ khách hàng thử nghiệm cho nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(sanitizedEmail), actingUser.getVaiTro());
        }
    }

    @Transactional
    public void updateNhanVien(Integer id, String hoTenNv, String chucVu, String soDienThoaiNv, String trangThai, String newPassword, Integer actingTaiKhoanId, String ipAddress) {
        String trimmedName = (hoTenNv == null) ? "" : hoTenNv.trim();
        String sanitizedName = org.jsoup.Jsoup.clean(trimmedName, org.jsoup.safety.Safelist.none());

        String trimmedChucVu = (chucVu == null) ? "" : chucVu.trim();
        String sanitizedChucVu = org.jsoup.Jsoup.clean(trimmedChucVu, org.jsoup.safety.Safelist.none());

        String trimmedPhone = (soDienThoaiNv == null) ? "" : soDienThoaiNv.trim();

        if (sanitizedName.isEmpty()) {
            throw new RuntimeException("Họ tên nhân viên không được để trống!");
        }
        if (sanitizedName.length() < 2 || sanitizedName.length() > 100) {
            throw new RuntimeException("Họ tên nhân viên phải có độ dài từ 2 đến 100 ký tự!");
        }

        if (sanitizedChucVu.isEmpty()) {
            throw new RuntimeException("Chức vụ không được để trống!");
        }
        if (sanitizedChucVu.length() < 2 || sanitizedChucVu.length() > 50) {
            throw new RuntimeException("Chức vụ phải có độ dài từ 2 đến 50 ký tự!");
        }

        if (trimmedPhone.isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (!trimmedPhone.matches(EMPLOYEE_PHONE_REGEX)) {
            throw new RuntimeException("Số điện thoại không đúng định dạng Việt Nam. Vui lòng nhập số bắt đầu bằng 03, 05, 07, 08, 09 và đủ 10 số.");
        }

        if (trangThai == null || (!"hoat_dong".equals(trangThai) && !"cho_khoa".equals(trangThai) && !"bi_khoa".equals(trangThai))) {
            throw new RuntimeException("Trạng thái không hợp lệ!");
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            String trimmedPassword = newPassword.trim();
            if (trimmedPassword.length() < 8 || trimmedPassword.length() > 50) {
                throw new RuntimeException("Mật khẩu mới phải có độ dài từ 8 đến 50 ký tự!");
            }
        }

        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if (nhanVienRepository.existsBySoDienThoaiNvAndIdNot(trimmedPhone, id)) {
            throw new IllegalArgumentException(MSG_DUPLICATE_EMPLOYEE_PHONE);
        }

        // 1. Lưu lại trạng thái cũ
        String oldStateStr = formatState(nv, tk);

        // 2. Cập nhật thông tin
        nv.setHoTenNv(sanitizedName);
        nv.setChucVu(sanitizedChucVu);
        nv.setSoDienThoaiNv(trimmedPhone);
        saveNhanVien(nv);

        // Update the linked KhachHang profile if it exists
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        if (kh != null) {
            String[] nameParts = splitFullName(sanitizedName);
            kh.setHoKh(nameParts[0]);
            kh.setTenKh(nameParts[1]);
            kh.setSoDienThoaiKh(trimmedPhone);
            saveKhachHang(kh);
        }

        tk.setTrangThai(trangThai);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            tk.setMatKhau(passwordEncoder.encode(newPassword.trim()));
        }
        saveTaiKhoan(tk);
        evictTaiKhoanStatusCache(tk.getId());

        // Soft deactivation via role flags is handled, NhanVien profile remains in database.
        // 3. Lưu Audit Log
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Cập nhật thông tin và vai trò nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername()), actingUser.getVaiTro());
        }
    }

    private void evictTaiKhoanStatusCache(Integer idTaiKhoan) {
        if (cacheManager != null && idTaiKhoan != null) {
            try {
                org.springframework.cache.Cache cache = cacheManager.getCache("taiKhoanStatus");
                if (cache != null) {
                    cache.evict(idTaiKhoan);
                }
            } catch (Exception e) {
                log.warn("Không thể xóa cache trạng thái tài khoản #{}: {}", idTaiKhoan, e.getMessage());
            }
        }
    }

    @Transactional
    public void toggleStatus(Integer id, Integer actingTaiKhoanId, String ipAddress, String appUrl) {
        NhanVien nv = findById(id);
        TaiKhoan tk = nv.getTaiKhoan();

        if ("QL".equals(tk.getVaiTro())) {
            throw new RuntimeException("Bạn không thể khóa/mở khóa tài khoản quản lý khác!");
        }

        // 1. Lưu trạng thái cũ
        String oldStateStr = formatState(nv, tk);

        // 2. Đổi trạng thái
        String oldStatus = tk.getTrangThai();
        String newStatus;
        String logMessage;

        if ("hoat_dong".equals(oldStatus)) {
            newStatus = "cho_khoa"; // Chờ phê duyệt khóa
            logMessage = "Yêu cầu khóa tài khoản nhân viên (chờ phê duyệt): " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername());

            // Token có dấu thời gian, chỉ dùng được trong 24 giờ.
            String token = createLockConfirmationToken();
            tk.setTokenXacThucKhoa(token);

            // Gửi email cho các admin hệ thống kèm link phê duyệt/từ chối trực tiếp
            guiEmailXacNhanKhoa(nv, tk, token, appUrl);
        } else if ("bi_khoa".equals(oldStatus)) {
            newStatus = "hoat_dong"; // Mở khóa trực tiếp
            logMessage = "Mở khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername());
            tk.setTokenXacThucKhoa(null);
        } else {
            // Đang ở trạng thái cho_khoa, bấm lại thì hủy yêu cầu khóa (trở lại hoạt động)
            newStatus = "hoat_dong";
            logMessage = "Hủy yêu cầu khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername());
            tk.setTokenXacThucKhoa(null);
        }

        tk.setTrangThai(newStatus);
        taiKhoanRepository.save(tk);
        evictTaiKhoanStatusCache(tk.getId());

        // 3. Lưu Audit Log
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, logMessage, actingUser.getVaiTro());
        }
    }

    private void guiEmailXacNhanKhoa(NhanVien nv, TaiKhoan tk, String token, String appUrl) {
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            System.err.println("Không có email quản trị nào được cấu hình trong app.admin.emails!");
            return;
        }
        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email.trim());
                message.setSubject("[Smash VN] Yêu cầu xác nhận khóa tài khoản nhân viên");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n"
                        + "Một yêu cầu khóa tài khoản nhân viên vừa được tạo và cần bạn xác nhận:\n"
                        + "- Nhân viên: %s\n"
                        + "- Email: %s\n"
                        + "- Chức vụ: %s\n"
                        + "- Số điện thoại: %s\n\n"
                        + "Vui lòng nhấp vào một trong các liên kết dưới đây để thực hiện hành động:\n"
                        + "1. PHÊ DUYỆT KHÓA TÀI KHOẢN: %s/admin/nhan-vien/approve-lock/%d?token=%s\n"
                        + "2. TỪ CHỐI KHÓA TÀI KHOẢN: %s/admin/nhan-vien/reject-lock/%d?token=%s\n\n"
                        + "Các liên kết chỉ mở trang xác nhận, không tự động thay đổi trạng thái tài khoản.\n"
                        + "Liên kết có hiệu lực trong 24 giờ.\n"
                        + "Yêu cầu này cũng hiển thị trên bảng điều khiển quản trị (Dashboard).\n"
                        + "Trân trọng,\n"
                        + "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getUsername(), nv.getChucVu(), nv.getSoDienThoaiNv(),
                        appUrl, nv.getId(), token,
                        appUrl, nv.getId(), token
                ));
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Lỗi gửi mail phê duyệt khóa đến {}: {}", email, e.getMessage(), e);
            }
        }
    }

    @Transactional
    public TaiKhoan approveLock(Integer id, String token, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(nv.getTaiKhoan().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên!"));
        TaiKhoan actingManager = getActingManager(actingTaiKhoanId);
        validatePendingLockAuthorization(tk, token, actingManager, "phê duyệt khóa");

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("bi_khoa");
        tk.setTokenXacThucKhoa(null); // Clear token
        taiKhoanRepository.save(tk);
        evictTaiKhoanStatusCache(tk.getId());

        if (actingManager == null) {
            auditService.log(tk.getId(), "TaiKhoan", tk.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Phê duyệt khóa tài khoản qua token Email", "SYSTEM_EMAIL");
        } else {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Phê duyệt khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername()), actingManager.getVaiTro());
        }
        return tk;
    }

    @Transactional
    public TaiKhoan rejectLock(Integer id, String token, Integer actingTaiKhoanId, String ipAddress) {
        NhanVien nv = findById(id);
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(nv.getTaiKhoan().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên!"));
        TaiKhoan actingManager = getActingManager(actingTaiKhoanId);
        validatePendingLockAuthorization(tk, token, actingManager, "từ chối khóa");

        String oldStateStr = formatState(nv, tk);
        tk.setTrangThai("hoat_dong");
        tk.setTokenXacThucKhoa(null); // Clear token
        taiKhoanRepository.save(tk);
        evictTaiKhoanStatusCache(tk.getId());

        // Gửi mail thông báo từ chối khóa về các admin
        guiEmailTuChoiKhoa(nv, tk);

        if (actingManager == null) {
            auditService.log(tk.getId(), "TaiKhoan", tk.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Từ chối khóa tài khoản qua token Email", "SYSTEM_EMAIL");
        } else {
            auditService.log(actingTaiKhoanId, "NhanVien", nv.getId().longValue(), "UPDATE", oldStateStr, formatState(nv, tk), ipAddress, "Từ chối khóa tài khoản nhân viên: " + com.smashvn.shop.util.ValidationUtils.maskEmail(tk.getUsername()), actingManager.getVaiTro());
        }
        return tk;
    }

    private void guiEmailTuChoiKhoa(NhanVien nv, TaiKhoan tk) {
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            return;
        }
        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email.trim());
                message.setSubject("[Smash VN] Yêu cầu khóa tài khoản bị từ chối");
                message.setText(String.format(
                        "Chào Admin hệ thống,\n\n"
                        + "Yêu cầu khóa tài khoản nhân viên sau đây đã bị TỪ CHỐI:\n"
                        + "- Nhân viên: %s\n"
                        + "- Email: %s\n"
                        + "- Chức vụ: %s\n\n"
                        + "Tài khoản của nhân viên này vẫn tiếp tục hoạt động bình thường trên hệ thống.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống Quản trị Smash VN",
                        nv.getHoTenNv(), tk.getUsername(), nv.getChucVu()
                ));
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Lỗi gửi mail thông báo từ chối khóa đến {}: {}", email, e.getMessage(), e);
            }
        }
    }
}
