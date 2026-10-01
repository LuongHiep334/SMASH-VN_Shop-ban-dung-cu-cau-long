package com.smashvn.shop.service.product;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dao.DanhGiaDAO;
import com.smashvn.shop.entity.CommentViolationLog;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.HinhAnhDanhGia;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.CommentViolationLogRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.service.blog.CommentModerationService;
import com.smashvn.shop.service.common.FileStorageService;
import com.smashvn.shop.util.ProfanityFilter;
import com.smashvn.shop.util.SeverityLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DanhGiaService {

    private final DanhGiaDAO danhGiaDAO;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final CommentViolationLogRepository commentViolationLogRepository;
    private final FileStorageService fileStorageService;
    private final ProfanityFilter profanityFilter;
    private final CommentModerationService commentModerationService;
    private final org.springframework.mail.javamail.JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.admin.emails}")
    private String adminEmailsConfig;

    /**
     * Lấy danh sách đánh giá chưa xóa của một sản phẩm
     */
    public List<DanhGia> layDanhSachDanhGiaTheoSanPham(Integer sanPhamId) {
        return danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(sanPhamId)
                .stream()
                .filter(dg -> dg.getBinhLuanAn() == null || !dg.getBinhLuanAn())
                .filter(dg -> dg.getKhachHang() == null || dg.getKhachHang().getTaiKhoan() == null
                || dg.getKhachHang().getTaiKhoan().getNgayKhoaBinhLuanDen() == null
                || dg.getKhachHang().getTaiKhoan().getNgayKhoaBinhLuanDen().isBefore(LocalDateTime.now()))
                .toList();
    }

    /**
     * Lấy toàn bộ danh sách đánh giá của hệ thống (cho Admin)
     */
    public List<DanhGia> layTatCaDanhGia() {
        return danhGiaDAO.findAllByOrderByNgayDanhGiaDesc();
    }

    /**
     * Kiểm tra xem tài khoản đã mua sản phẩm này chưa
     */
    public boolean daMuaSanPham(Integer idTaiKhoan, Integer idSanPham) {
        if (idTaiKhoan == null || idSanPham == null) {
            return false;
        }
        return hoaDonChiTietRepository.hasPurchasedProduct(idTaiKhoan, idSanPham);
    }

    /**
     * Kiểm tra xem tài khoản đã đánh giá sản phẩm này chưa
     */
    public Optional<DanhGia> layDanhGiaDaCo(Integer idTaiKhoan, Integer idSanPham) {
        if (idTaiKhoan == null || idSanPham == null) {
            return Optional.empty();
        }
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return Optional.empty();
        }
        return danhGiaDAO.findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(kh.getId(), idSanPham);
    }

    private void guiEmailCanhBaoAdmin(String nameKh, String emailKh, String productName, String rawComment, String filteredComment, String severity, int violationCount, String banDuration, String banExpiration) {
        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            log.warn("[EMAIL] Khong co email quan tri nao duoc cau hinh trong app.admin.emails!");
            return;
        }
        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
                message.setTo(email.trim());
                message.setSubject("[Cảnh báo] Bình luận vi phạm nghiêm trọng - Smash VN");
                message.setText(String.format(
                        "Chào Admin,\n\n"
                        + "Hệ thống phát hiện bình luận có mức độ vi phạm %s từ khách hàng:\n"
                        + "- Người bình luận: %s\n"
                        + "- Email: %s\n"
                        + "- Sản phẩm: %s\n"
                        + "- Nội dung gốc: %s\n"
                        + "- Nội dung đã lọc: %s\n"
                        + "- Số lần vi phạm của tài khoản: %d/5\n"
                        + "- Hình phạt áp dụng: Khóa bình luận %s (Đến: %s)\n\n"
                        + "Vui lòng truy cập trang quản trị để xử lý nếu cần: http://localhost:8080/admin/danh-gia\n",
                        severity, nameKh, emailKh, productName, rawComment, filteredComment, violationCount, banDuration, banExpiration
                ));
                mailSender.send(message);
            } catch (Exception e) {
                log.error("[EMAIL_ERROR] Failed to send admin email alert to {}: {}", email, e.getMessage());
            }
        }
    }

    /**
     * Thêm mới hoặc Cập nhật đánh giá của khách hàng
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean themHoacCapNhatDanhGia(Integer idTaiKhoan, Integer idSanPham, Double soSao, String binhLuan, List<MultipartFile> files) throws Exception {
        if (binhLuan == null || binhLuan.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung đánh giá không được để trống.");
        }
        // 1. Kiểm tra sự tồn tại và trạng thái sản phẩm (Chặn sản phẩm không hoạt động)
        SanPham sanPham = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));
        if (sanPham.getTrangThai() == null || !"dang_ban".equals(sanPham.getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm này hiện không còn hỗ trợ đánh giá.");
        }

        // 2. Lấy thông tin Khách Hàng
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập tài khoản khách hàng để thực hiện đánh giá.");
        }

        // 3. Kiểm tra lịch sử mua hàng thành công
        boolean daMua = hoaDonChiTietRepository.hasPurchasedProduct(idTaiKhoan, idSanPham);
        if (!daMua) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công.");
        }

        // Concurrency lock: Load TaiKhoan with Pessimistic Write Lock
        TaiKhoan tk = taiKhoanRepository.findByIdForUpdate(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại!"));
        if (tk.getTrangThaiTaiKhoan() != AccountStatus.ACTIVE
                || (tk.getTrangThai() != null && !"hoat_dong".equalsIgnoreCase(tk.getTrangThai()))) {
            throw new IllegalStateException("Bạn cần kích hoạt tài khoản thành viên để thực hiện đánh giá.");
        }

        // Check if currently comment-banned
        if (tk.getNgayKhoaBinhLuanDen() != null && tk.getNgayKhoaBinhLuanDen().isAfter(LocalDateTime.now())) {
            if (tk.getNgayKhoaBinhLuanDen().getYear() >= 9999) {
                throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa tính năng bình luận vĩnh viễn do vi phạm tiêu chuẩn cộng đồng.");
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                throw new IllegalArgumentException("Tài khoản của bạn đang bị khóa tính năng bình luận/đánh giá đến " + tk.getNgayKhoaBinhLuanDen().format(formatter) + " do vi phạm tiêu chuẩn cộng đồng.");
            }
        }

        // Reset violation count if it's been 180 days since the last violation
        LocalDateTime lastViPham = tk.getNgayViPhamGanNhat();
        if (lastViPham != null && java.time.Duration.between(lastViPham, LocalDateTime.now()).toDays() >= 180) {
            tk.setSoLanNhacNhoViPham(0);
            if (tk.getNgayKhoaBinhLuanDen() != null && tk.getNgayKhoaBinhLuanDen().isBefore(LocalDateTime.now())) {
                tk.setNgayKhoaBinhLuanDen(null);
            }
        }

        // 4. Kiểm tra Spam Rate Limit (Cấm spam liên tục)
        // Check 30s giữa 2 lần đánh giá liên tiếp
        Optional<DanhGia> latestOpt = danhGiaDAO.findTopByKhachHang_TaiKhoan_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(idTaiKhoan);
        if (latestOpt.isPresent()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime latestTime = latestOpt.get().getNgayCapNhat() != null ? latestOpt.get().getNgayCapNhat() : latestOpt.get().getNgayDanhGia();
            long seconds = Duration.between(latestTime, now).getSeconds();
            if (seconds < 30) {
                throw new IllegalArgumentException("Bạn gửi yêu cầu quá nhanh! Vui lòng đợi ít nhất 30 giây giữa các lần đánh giá.");
            }
        }
        // Check 5 đánh giá trong 1 giờ
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourCount = danhGiaDAO.countByKhachHang_TaiKhoan_IdAndDaXoaFalseAndNgayDanhGiaAfter(idTaiKhoan, oneHourAgo);
        if (hourCount >= 5) {
            throw new IllegalArgumentException("Bạn đã vượt quá số lượng đánh giá cho phép (tối đa 5 đánh giá mỗi giờ).");
        }

        // 5. Phân tích tục tĩu
        java.util.List<String> customKeywords;
        try {
            customKeywords = commentModerationService.getActiveRawKeywords();
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("[REVIEW_MODERATION_ERROR] Cannot load active moderation keywords", e);
            throw new IllegalStateException("Hệ thống kiểm duyệt đang tạm thời gián đoạn. Vui lòng thử lại sau.");
        }
        ProfanityFilter.FilterResult moderation = profanityFilter.filterWithResult(binhLuan, customKeywords);
        SeverityLevel severity = profanityFilter.getSeverity(binhLuan);
        if (severity == SeverityLevel.NONE && moderation.moderated()) {
            severity = SeverityLevel.MEDIUM;
        }
        boolean isViolation = moderation.moderated();
        String filteredComment = moderation.content();

        boolean autoHide = false;
        String textThoiHan = "";
        String expirationStr = "";

        if (isViolation) {
            tk.setSoLanNhacNhoViPham(tk.getSoLanNhacNhoViPham() + 1);
            tk.setNgayViPhamGanNhat(LocalDateTime.now());

            LocalDateTime khoaDen;
            int violations = tk.getSoLanNhacNhoViPham();
            switch (violations) {
                case 1:
                    khoaDen = LocalDateTime.now().plusHours(3);
                    textThoiHan = "3 giờ";
                    break;
                case 2:
                    khoaDen = LocalDateTime.now().plusDays(1);
                    textThoiHan = "1 ngày";
                    break;
                case 3:
                    khoaDen = LocalDateTime.now().plusDays(7);
                    textThoiHan = "7 ngày";
                    break;
                case 4:
                    khoaDen = LocalDateTime.now().plusDays(30);
                    textThoiHan = "30 ngày";
                    break;
                default:
                    khoaDen = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
                    textThoiHan = "Vĩnh viễn";
            }
            tk.setNgayKhoaBinhLuanDen(khoaDen);
            taiKhoanRepository.save(tk);

            expirationStr = khoaDen.getYear() >= 9999 ? "Vĩnh viễn" : khoaDen.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            // Create notification for customer
            String thongBaoNoiDung = String.format(
                    "Đánh giá của bạn tại sản phẩm '%s' chứa từ ngữ không phù hợp và vi phạm tiêu chuẩn cộng đồng của SMASH-VN (Mức độ: %s). "
                    + "Số lần vi phạm hiện tại: %d/5. "
                    + "Quyền bình luận của bạn đã bị khóa tạm thời %s đến %s. Vui lòng tuân thủ hướng dẫn cộng đồng.",
                    sanPham.getTenSanPham(), severity.name(), violations, textThoiHan, expirationStr
            );
            ThongBao tb = ThongBao.builder()
                    .taiKhoan(tk)
                    .tieuDe("Cảnh báo vi phạm tiêu chuẩn cộng đồng")
                    .noiDung(thongBaoNoiDung)
                    .daDoc(false)
                    .ngayTao(LocalDateTime.now())
                    .build();
            thongBaoRepository.save(tb);

            // Send admin email for HIGH and CRITICAL severity
            if (severity == SeverityLevel.HIGH || severity == SeverityLevel.CRITICAL) {
                String nameKh = kh.getHoKh() + " " + kh.getTenKh();
                guiEmailCanhBaoAdmin(nameKh, tk.getUsername(), sanPham.getTenSanPham(), binhLuan, filteredComment, severity.name(), violations, textThoiHan, expirationStr);
            }

            // CRITICAL severity automatically hides the review
            if (severity == SeverityLevel.CRITICAL) {
                autoHide = true;
            }
        }

        // Tìm đánh giá cũ của khách hàng cho sản phẩm này
        Optional<DanhGia> existingOpt = danhGiaDAO.findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(kh.getId(), idSanPham);
        List<String> uploadedFileNames = new ArrayList<>();
        DanhGia dgSaved;

        try {
            if (existingOpt.isPresent()) {
                // CASE CHỈNH SỬA / GHI ĐÈ ĐÁNH GIÁ CŨ
                DanhGia dg = existingOpt.get();
                boolean starChanged = !dg.getSoSao().equals(soSao);

                dg.setSoSao(soSao);
                dg.setBinhLuan(filteredComment);
                dg.setNgayCapNhat(LocalDateTime.now());
                if (autoHide) {
                    dg.setAnBinhLuan(true);
                }

                // Xử lý ảnh đính kèm cũ
                boolean hasNewUpload = false;
                if (files != null) {
                    for (MultipartFile file : files) {
                        if (file != null && !file.isEmpty()) {
                            hasNewUpload = true;
                            break;
                        }
                    }
                }

                if (hasNewUpload) {
                    // Xóa ảnh cũ trên đĩa vật lý
                    for (HinhAnhDanhGia oldAnh : dg.getDanhSachAnh()) {
                        fileStorageService.deleteImage(oldAnh.getDuongDan(), "reviews");
                    }
                    // Dọn danh sách trong DB
                    dg.getDanhSachAnh().clear();

                    // Tải ảnh mới lên
                    uploadedFileNames = fileStorageService.saveReviewImages(files);
                    for (String name : uploadedFileNames) {
                        HinhAnhDanhGia anh = HinhAnhDanhGia.builder()
                                .danhGia(dg)
                                .urlHinhAnh(name)
                                .ngayTao(LocalDateTime.now())
                                .build();
                        dg.getDanhSachAnh().add(anh);
                    }
                }

                danhGiaDAO.save(dg);
                dgSaved = dg;

                // Cập nhật Cache Rating của sản phẩm nếu số sao thay đổi
                if (starChanged) {
                    updateProductRatingStats(idSanPham);
                }
            } else {
                // CASE GỬI ĐÁNH GIÁ MỚI LẦN ĐẦU
                DanhGia dg = DanhGia.builder()
                        .khachHang(kh)
                        .sanPham(sanPham)
                        .soSao(soSao)
                        .binhLuan(filteredComment)
                        .ngayDanhGia(LocalDateTime.now())
                        .daXoa(false)
                        .binhLuanAn(autoHide)
                        .hinhAnhAn(false)
                        .build();

                // Lưu ảnh đính kèm nếu có
                boolean hasUpload = false;
                if (files != null) {
                    for (MultipartFile file : files) {
                        if (file != null && !file.isEmpty()) {
                            hasUpload = true;
                            break;
                        }
                    }
                }

                if (hasUpload) {
                    uploadedFileNames = fileStorageService.saveReviewImages(files);
                    for (String name : uploadedFileNames) {
                        HinhAnhDanhGia anh = HinhAnhDanhGia.builder()
                                .danhGia(dg)
                                .urlHinhAnh(name)
                                .ngayTao(LocalDateTime.now())
                                .build();
                        dg.getDanhSachAnh().add(anh);
                    }
                }

                danhGiaDAO.save(dg);
                dgSaved = dg;

                // Cập nhật Cache Rating của sản phẩm khi có review mới
                updateProductRatingStats(idSanPham);
            }

            // Save violation log if it's a violation
            if (isViolation) {
                CommentViolationLog logEntry = CommentViolationLog.builder()
                        .taiKhoan(tk)
                        .danhGia(dgSaved)
                        .sanPham(sanPham)
                        // Never persist the unmoderated text, including in audit logs.
                        .noiDungGoc(filteredComment)
                        .noiDungDaLoc(filteredComment)
                        .mucDoViPham(severity.name())
                        .soLanViPham(tk.getSoLanNhacNhoViPham())
                        .thoiHanKhoa(textThoiHan)
                        .ngayViPham(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();
                commentViolationLogRepository.save(logEntry);
            }

        } catch (Exception e) {
            // Rollback an toàn: Dọn dẹp tệp tin mồ côi vừa lưu trên đĩa nếu ghi DB thất bại
            log.error("[ROLLBACK_CLEANUP] Database transaction failed. Cleaning up uploaded review files.");
            for (String name : uploadedFileNames) {
                fileStorageService.deleteImage(name, "reviews");
            }
            throw e;
        }
        return moderation.moderated();
    }

    /**
     * Tính toán điểm rating trung bình và tổng số lượng đánh giá để cache vào
     * bảng SanPham
     */
    public void updateProductRatingStats(Integer idSanPham) {
        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseAndBinhLuanAnFalseOrderByNgayDanhGiaDesc(idSanPham)
                .stream()
                .filter(dg -> dg.getKhachHang() == null || dg.getKhachHang().getTaiKhoan() == null
                || dg.getKhachHang().getTaiKhoan().getNgayKhoaBinhLuanDen() == null
                || dg.getKhachHang().getTaiKhoan().getNgayKhoaBinhLuanDen().isBefore(LocalDateTime.now()))
                .toList();
        int soDanhGia = activeReviews.size();
        double diemTrungBinh = activeReviews.stream()
                .mapToDouble(DanhGia::getSoSao)
                .average()
                .orElse(0.0);

        // Làm tròn 1 chữ số thập phân
        diemTrungBinh = Math.round(diemTrungBinh * 10.0) / 10.0;

        SanPham sp = sanPhamRepository.findById(idSanPham).orElse(null);
        if (sp != null) {
            sp.setSoDanhGia(soDanhGia);
            sp.setDiemTrungBinh(diemTrungBinh);
            sanPhamRepository.save(sp);
            log.info("[CACHE_RATING] Updated stats for product {}: {} reviews, avg {} stars", idSanPham, soDanhGia, diemTrungBinh);
        }
    }

    /**
     * Ẩn Bình Luận (Soft Moderation)
     */
    @Transactional
    public void anBinhLuan(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnBinhLuan(true);
        dg.setNguoiAnBinhLuan(admin);
        dg.setNgayAnBinhLuan(LocalDateTime.now());
        danhGiaDAO.saveAndFlush(dg);

        updateProductRatingStats(dg.getSanPham().getId());
    }

    /**
     * Phục hồi hiển thị bình luận
     */
    @Transactional
    public void hienBinhLuan(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        List<String> customKeywords = commentModerationService.getActiveRawKeywords();
        if (dg.getBinhLuan() != null && !dg.getBinhLuan().isBlank()) {
            ProfanityFilter.FilterResult moderation = profanityFilter.filterWithResult(dg.getBinhLuan(), customKeywords);
            if (moderation.moderated()) {
                throw new IllegalArgumentException("Không thể hiển thị lại vì nội dung bình luận vẫn vi phạm từ khóa cấm hiện tại.");
            }
        }

        dg.setAnBinhLuan(false);
        dg.setNguoiHienBinhLuan(admin);
        dg.setNgayHienBinhLuan(LocalDateTime.now());
        danhGiaDAO.saveAndFlush(dg);

        updateProductRatingStats(dg.getSanPham().getId());
    }

    /**
     * Quét và tự động ẩn các bình luận vi phạm danh sách từ cấm mới
     */
    @Transactional
    public void scanAndModerateReviews() {
        long startTime = System.currentTimeMillis();
        List<String> customKeywords = commentModerationService.getActiveRawKeywords();
        if (customKeywords == null || customKeywords.isEmpty()) {
            log.info("[REVIEW_MODERATION_JOB] No active custom keywords found. Exiting scan.");
            return;
        }

        List<DanhGia> activeReviews = danhGiaDAO.findByDaXoaFalseAndBinhLuanAnFalse();
        java.util.Set<Integer> productIdsToUpdate = new java.util.HashSet<>();
        int hiddenCount = 0;

        for (DanhGia dg : activeReviews) {
            String text = dg.getBinhLuan();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            ProfanityFilter.FilterResult moderation = profanityFilter.filterWithResult(text, customKeywords);
            if (moderation.moderated()) {
                dg.setAnBinhLuan(true);
                dg.setNgayAnBinhLuan(LocalDateTime.now());
                danhGiaDAO.save(dg);

                productIdsToUpdate.add(dg.getSanPham().getId());
                hiddenCount++;

                SeverityLevel severity = profanityFilter.getSeverity(text, customKeywords);
                if (severity == SeverityLevel.NONE) {
                    severity = SeverityLevel.MEDIUM;
                }

                TaiKhoan tk = (dg.getKhachHang() != null) ? dg.getKhachHang().getTaiKhoan() : null;
                if (tk == null) {
                    // Fallback to avoid dropping the log silently
                    tk = taiKhoanRepository.findAll().stream().findFirst().orElse(null);
                }

                if (tk != null) {
                    CommentViolationLog logEntry = CommentViolationLog.builder()
                            .taiKhoan(tk)
                            .danhGia(dg)
                            .sanPham(dg.getSanPham())
                            .noiDungGoc(text)
                            .noiDungDaLoc(moderation.content())
                            .mucDoViPham(severity.name())
                            .soLanViPham(tk.getSoLanNhacNhoViPham())
                            .thoiHanKhoa(null)
                            .ngayViPham(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();
                    commentViolationLogRepository.save(logEntry);
                }
            }
        }

        if (!productIdsToUpdate.isEmpty()) {
            danhGiaDAO.flush();
            for (Integer productId : productIdsToUpdate) {
                updateProductRatingStats(productId);
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("[REVIEW_MODERATION_JOB] Scan summary: checked {} reviews, hidden {} reviews, recalculated {} products, duration {} ms.",
                 activeReviews.size(), hiddenCount, productIdsToUpdate.size(), duration);
    }

    /**
     * Ẩn Hình Ảnh (Soft Moderation)
     */
    @Transactional
    public void anHinhAnh(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnHinhAnh(true);
        dg.setNguoiAnHinhAnh(admin);
        dg.setNgayAnHinhAnh(LocalDateTime.now());
        danhGiaDAO.save(dg);

        // KHÔNG cập nhật cache rating khi ẩn hình ảnh
    }

    /**
     * Phục hồi hiển thị hình ảnh
     */
    @Transactional
    public void hienHinhAnh(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setAnHinhAnh(false);
        dg.setNguoiHienHinhAnh(admin);
        dg.setNgayHienHinhAnh(LocalDateTime.now());
        danhGiaDAO.save(dg);
    }

    /**
     * Xóa Mềm Đánh Giá (Soft Delete)
     */
    @Transactional
    public void xoaMemDanhGia(Integer id, Integer adminId) {
        DanhGia dg = danhGiaDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        TaiKhoan admin = taiKhoanRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị!"));

        dg.setDaXoa(true);
        dg.setNguoiXoa(admin);
        dg.setNgayXoa(LocalDateTime.now());
        danhGiaDAO.saveAndFlush(dg);

        // Cập nhật lại cache rating trên thực thể SanPham khi đánh giá bị loại bỏ
        updateProductRatingStats(dg.getSanPham().getId());
    }

}
