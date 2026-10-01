package com.smashvn.shop.service.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.exception.PromotionValidationException;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.NewsletterService;
import com.smashvn.shop.util.ApDungKieu;
import com.smashvn.shop.util.PromotionValidationConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý toàn bộ nghiệp vụ QUẢN LÝ KHUYẾN MÃI của admin.
 *
 * <p>
 * Bao gồm hai nhóm chức năng chính:</p>
 * <ol>
 * <li><b>Đợt giảm giá (Campaign)</b> – áp dụng trực tiếp lên sản phẩm theo %
 * niêm yết.</li>
 * <li><b>Phiếu giảm giá (Voucher)</b> – khách nhập mã tại trang thanh
 * toán.</li>
 * </ol>
 *
 * <p>
 * Mỗi thao tác thêm/sửa/xóa đều ghi {@code EditLog} (audit trail) để truy vết
 * sau này.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKhuyenMaiService {

    private final DotGiamGiaDAO dotGiamGiaDAO;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final SanPhamRepository sanPhamRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final NewsletterService newsletterService;

    /**
     * Định dạng ngày giờ dùng trong audit log và thông báo lỗi.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==========================================
    // ĐỢT GIẢM GIÁ (CAMPAIGN) – CÁC PHƯƠNG THỨC
    // ==========================================
    /**
     * Lấy toàn bộ danh sách đợt giảm giá (kể cả đã vô hiệu hóa). Dùng để hiển
     * thị bảng quản lý tại trang admin.
     *
     * @return danh sách tất cả {@link DotGiamGia}.
     */
    @Transactional(readOnly = true)
    public List<DotGiamGia> getAllDotGiamGia() {
        return dotGiamGiaDAO.findAll();
    }

    /**
     * Tìm một đợt giảm giá theo ID.
     *
     * @param id ID của đợt giảm giá cần tìm.
     * @return đợt giảm giá tương ứng.
     * @throws RuntimeException nếu không tìm thấy.
     */
    @Transactional(readOnly = true)
    public DotGiamGia getDotGiamGiaById(Integer id) {
        return dotGiamGiaDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá id: " + id));
    }

    /**
     * Tạo mới một đợt giảm giá.
     *
     * <p>
     * Luồng xử lý:</p>
     * <ol>
     * <li>Validate tên chiến dịch (không trống, 2–100 ký tự, sanitize
     * XSS).</li>
     * <li>Validate loại giảm giá (chỉ "Theo Phần Trăm" hoặc "Theo
     * Khoảng").</li>
     * <li>Validate ngày bắt đầu phải trước ngày kết thúc.</li>
     * <li>Validate % giảm hợp lệ (1–MAX_CAMPAIGN_DISCOUNT_PERCENT).</li>
     * <li>Kiểm tra phải chọn ít nhất 1 sản phẩm.</li>
     * <li>Kiểm tra không được chồng lên đợt giảm giá khác cùng sản phẩm cùng
     * thời gian.</li>
     * <li>Lưu vào DB và ghi audit log.</li>
     * </ol>
     *
     * @param tenChienDich tên chiến dịch giảm giá.
     * @param start thời điểm bắt đầu.
     * @param end thời điểm kết thúc.
     * @param phanTramGiam % giảm giá.
     * @param loaiGiamGia loại giảm giá ("Theo Phần Trăm" / "Theo Khoảng").
     * @param productIds danh sách ID sản phẩm được áp dụng.
     * @param actingTaiKhoanId ID tài khoản admin đang thực hiện (để ghi log).
     * @param ipAddress IP thực hiện yêu cầu (để ghi log).
     * @return đợt giảm giá vừa được tạo.
     * @throws PromotionValidationException nếu dữ liệu không hợp lệ.
     */
    /**
     * Overloaded method for backward compatibility with existing tests.
     */
    @Transactional
    public DotGiamGia createDotGiamGia(String tenChienDich, LocalDateTime start, LocalDateTime end,
            Integer phanTramGiam, String loaiGiamGia, List<Integer> productIds,
            Integer actingTaiKhoanId, String ipAddress) {
        return createDotGiamGia(tenChienDich, start, end, phanTramGiam, loaiGiamGia,
                "MANUAL", productIds, null, null, actingTaiKhoanId, ipAddress);
    }

    @Transactional
    public DotGiamGia createDotGiamGia(String tenChienDich, LocalDateTime start, LocalDateTime end,
            Integer phanTramGiam, String loaiGiamGia,
            String kieuApDungStr, List<Integer> productIds,
            BigDecimal giaFrom, BigDecimal giaDen,
            Integer actingTaiKhoanId, String ipAddress) {
        // 1. Validation tên chiến dịch
        if (tenChienDich == null || tenChienDich.trim().isEmpty()) {
            throw new PromotionValidationException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        // Loại bỏ các thẻ HTML để chống tấn công XSS
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new PromotionValidationException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        // 2. Validation ngày (không cho chọn thời gian bắt đầu trong quá khứ khi tạo mới)
        validateCampaignDates(start, end, false);

        // 3. Validation % giảm
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT) {
            throw new PromotionValidationException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến " + PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT + "%!");
        }

        // 4. Resolve danh sách sản phẩm theo kiểu áp dụng
        ApDungKieu kieuApDung = ApDungKieu.fromString(kieuApDungStr);
        List<Integer> finalProductIds;
        if (kieuApDung == ApDungKieu.PRICE_RANGE) {
            // PRICE_RANGE: hệ thống tự tìm sản phẩm theo khoảng giá
            List<SanPham> matchedProducts = findProductsByPriceRange(giaFrom, giaDen);
            finalProductIds = matchedProducts.stream().map(SanPham::getId).toList();
        } else {
            // MANUAL: validate productIds không rỗng và chỉ chứa SP đang bán
            if (productIds == null || productIds.isEmpty()) {
                throw new PromotionValidationException(
                        "Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
            }
            List<SanPham> activeFromIds = sanPhamRepository.findActiveByIdIn(productIds);
            if (activeFromIds.size() != productIds.size()) {
                throw new PromotionValidationException(
                        "Danh sách sản phẩm không hợp lệ hoặc có sản phẩm đã ngừng bán!");
            }
            finalProductIds = productIds;
        }

        // 5. Kiểm tra không chồng đợt giảm giá lên nhau (cùng sản phẩm, cùng thời gian)
        checkCampaignOverlaps(finalProductIds, start, end, null);

        // 6. Lấy thông tin nhân viên thực hiện
        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new PromotionValidationException("Tài khoản đang thực hiện không có thông tin nhân viên!");
        }

        // 7. Tạo và lưu entity
        DotGiamGia dgg = new DotGiamGia();
        dgg.setTenChienDich(sanitizedTen);
        dgg.setNgayBatDau(start);
        dgg.setNgayKetThuc(end);
        dgg.setPhanTramGiam(phanTramGiam);
        dgg.setLoaiGiamGia(loaiGiamGia);
        dgg.setNhanVien(nv);
        dgg.setActive(true);

        // Gán danh sách sản phẩm (quản lý từ phía DotGiamGia trong ManyToMany)
        Set<SanPham> selectedProducts = new HashSet<>(sanPhamRepository.findAllById(finalProductIds));
        dgg.setSanPhams(selectedProducts);

        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        // 9. Ghi audit log: ai tạo, lúc nào, nội dung gì
        writeEditLog(actingTaiKhoanId, "DotGiamGia", saved.getId().longValue(), "INSERT",
                null, formatCampaignState(saved), ipAddress, "Tạo mới đợt giảm giá: " + sanitizedTen);

        if (saved.getActive() != null && saved.getActive()) {
            try {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                newsletterService.sendPromotionEmailAsync(saved.getId());
                            } catch (Exception e) {
                                log.error("[Bản tin khuyến mãi] Lỗi kích hoạt gửi email thông báo sau commit cho đợt giảm giá #{}: {}", saved.getId(), e.getMessage(), e);
                            }
                        }
                    });
                } else {
                    newsletterService.sendPromotionEmailAsync(saved.getId());
                }
            } catch (Exception e) {
                log.error("[Bản tin khuyến mãi] Lỗi thiết lập thông báo email cho đợt giảm giá #{}: {}", saved.getId(), e.getMessage(), e);
            }
        }

        return saved;
    }

    /**
     * Cập nhật thông tin một đợt giảm giá đã tồn tại.
     *
     * <p>
     * Khi lưu thành công, đợt giảm giá tự động được kích hoạt lại
     * ({@code active = true}) dù trước đó có thể đã bị vô hiệu hóa. Đồng thời
     * đồng bộ quan hệ ManyToMany hai chiều với {@link SanPham} để tránh dữ liệu
     * không nhất quán.</p>
     *
     * @param id ID đợt giảm giá cần sửa.
     * @param tenChienDich tên mới của chiến dịch.
     * @param start ngày bắt đầu mới.
     * @param end ngày kết thúc mới.
     * @param phanTramGiam % giảm giá mới.
     * @param loaiGiamGia loại giảm giá mới.
     * @param productIds danh sách sản phẩm mới.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     * @return đợt giảm giá sau khi cập nhật.
     */
    @Transactional
    public DotGiamGia updateDotGiamGia(Integer id, String tenChienDich, LocalDateTime start, LocalDateTime end,
            Integer phanTramGiam, String loaiGiamGia, List<Integer> productIds,
            Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);

        // Validate các trường đầu vào (tương tự create)
        if (tenChienDich == null || tenChienDich.trim().isEmpty()) {
            throw new PromotionValidationException("Tên chiến dịch không được để trống!");
        }
        String cleanTen = tenChienDich.trim();
        String sanitizedTen = org.jsoup.Jsoup.clean(cleanTen, org.jsoup.safety.Safelist.none());
        if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
            throw new PromotionValidationException("Tên chiến dịch phải có độ dài từ 2 đến 100 ký tự!");
        }

        validateCampaignDates(start, end, true);
        if (phanTramGiam == null || phanTramGiam < 1 || phanTramGiam > PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT) {
            throw new PromotionValidationException("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến " + PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT + "%!");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new PromotionValidationException("Vui lòng chọn ít nhất một sản phẩm để áp dụng đợt giảm giá!");
        }
        // Validate productIds chỉ chứa sản phẩm đang bán (chống tamper từ request)
        List<SanPham> activeFromIds = sanPhamRepository.findActiveByIdIn(productIds);
        if (activeFromIds.size() != productIds.size()) {
            throw new PromotionValidationException(
                    "Danh sách sản phẩm không hợp lệ hoặc có sản phẩm đã ngừng bán!");
        }

        // Kiểm tra chồng chéo, loại trừ chính đợt đang sửa (truyền excludeCampaignId = id)
        checkCampaignOverlaps(productIds, start, end, id);

        // Chụp trạng thái cũ trước khi sửa để ghi vào audit log
        String oldState = formatCampaignState(dgg);

        // Đồng bộ ManyToMany hai chiều:
        // - Với SP không còn trong danh sách mới: gỡ đợt này ra khỏi SP đó
        Set<SanPham> selectedProducts = new HashSet<>(sanPhamRepository.findAllById(productIds));
        for (SanPham sp : dgg.getSanPhams()) {
            if (!selectedProducts.contains(sp)) {
                if (sp.getCacDotGiamGia() != null) {
                    sp.getCacDotGiamGia().remove(dgg);
                }
            }
        }
        // - Với SP mới được thêm vào: liên kết đợt này vào SP đó
        for (SanPham sp : selectedProducts) {
            if (!dgg.getSanPhams().contains(sp)) {
                if (sp.getCacDotGiamGia() == null) {
                    sp.setCacDotGiamGia(new HashSet<>());
                }
                sp.getCacDotGiamGia().add(dgg);
            }
        }

        // Cập nhật các trường và lưu
        dgg.setTenChienDich(sanitizedTen);
        dgg.setNgayBatDau(start);
        dgg.setNgayKetThuc(end);
        dgg.setPhanTramGiam(phanTramGiam);
        dgg.setLoaiGiamGia(loaiGiamGia);
        // Lưu lại → tự động kích hoạt lại đợt giảm giá nếu trước đó đã bị tắt
        dgg.setActive(true);

        dgg.getSanPhams().clear();
        dgg.getSanPhams().addAll(selectedProducts);

        DotGiamGia updated = dotGiamGiaDAO.save(dgg);

        // Ghi audit log: trạng thái trước và sau khi sửa
        writeEditLog(actingTaiKhoanId, "DotGiamGia", updated.getId().longValue(), "UPDATE",
                oldState, formatCampaignState(updated), ipAddress, "Cập nhật đợt giảm giá: " + sanitizedTen);

        return updated;
    }

    /**
     * Vô hiệu hóa thủ công một đợt giảm giá (soft disable). Đặt
     * {@code active = false}, đợt giảm giá sẽ không còn hiển thị trên trang sản
     * phẩm. Dữ liệu KHÔNG bị xóa khỏi DB, có thể kích hoạt lại bằng cách lưu
     * lại form sửa.
     *
     * @param id ID đợt giảm giá cần vô hiệu hóa.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     */
    @Transactional
    public void deactivateDotGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);
        String oldState = formatCampaignState(dgg);

        dgg.setActive(false);
        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        writeEditLog(actingTaiKhoanId, "DotGiamGia", id.longValue(), "UPDATE",
                oldState, formatCampaignState(saved), ipAddress, "Vô hiệu hóa đợt giảm giá: " + dgg.getTenChienDich());
    }

    /**
     * Xóa logic (soft delete) một đợt giảm giá. Về kỹ thuật giống
     * {@link #deactivateDotGiamGia} – chỉ đặt {@code active = false}. Hệ thống
     * không xóa cứng để bảo toàn lịch sử đơn hàng đã dùng đợt giảm này. Audit
     * log ghi hành động là "DELETE" để phân biệt với deactivate thông thường.
     *
     * @param id ID đợt giảm giá cần xóa.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     */
    @Transactional
    public void deleteDotGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        DotGiamGia dgg = getDotGiamGiaById(id);
        String oldState = formatCampaignState(dgg);

        dgg.setActive(false); // Soft delete – không xóa khỏi DB
        DotGiamGia saved = dotGiamGiaDAO.save(dgg);

        writeEditLog(actingTaiKhoanId, "DotGiamGia", id.longValue(), "DELETE",
                oldState, formatCampaignState(saved), ipAddress, "Xóa logic đợt giảm giá: " + dgg.getTenChienDich());
    }

    /**
     * Kiểm tra tính hợp lệ của khoảng thời gian đợt giảm giá. Ngày bắt đầu phải
     * có mặt, ngày kết thúc phải có mặt, và ngày bắt đầu phải TRƯỚC ngày kết
     * thúc (không cho bằng nhau).
     *
     * @param start ngày bắt đầu.
     * @param end ngày kết thúc.
     * @throws PromotionValidationException nếu thời gian không hợp lệ.
     */
    // ==========================================
    // PRICE-RANGE HELPERS
    // ==========================================
    /**
     * Parse chuỗi tiền VNĐ nhập từ form về BigDecimal. Chấp nhận các định dạng:
     * "500000", "500.000", "500,000", "1.500.000". Trả về null nếu chuỗi rỗng
     * và allowNull = true.
     *
     * @param valueStr chuỗi nhập từ form.
     * @param fieldName tên trường hiển thị trong thông báo lỗi.
     * @param allowNull true nếu cho phép trường rỗng (trả null).
     * @return giá trị BigDecimal đã parse, hoặc null nếu allowNull và rỗng.
     * @throws PromotionValidationException nếu vi phạm bất kỳ ràng buộc nào.
     */
    public BigDecimal parseVndCurrency(String valueStr, String fieldName, boolean allowNull) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            if (allowNull) {
                return null;
            }
            throw new PromotionValidationException(fieldName + " không được để trống!");
        }

        String trimmed = valueStr.trim();

        // Bắt đầu bằng dấu "-" → số âm
        if (trimmed.startsWith("-")) {
            throw new PromotionValidationException(fieldName + " không được là số âm!");
        }

        // Validate format: chỉ chấp nhận số nguyên hoặc dạng phân cách hàng nghìn
        if (!trimmed.matches("\\d+") && !trimmed.matches("\\d{1,3}([.,]\\d{3})+")) {
            throw new PromotionValidationException(
                    fieldName + " phải là số nguyên VNĐ hợp lệ. Ví dụ: 500000 hoặc 500.000!"
            );
        }

        // Chuẩn hóa: bỏ dấu phân cách hàng nghìn
        String normalized = trimmed.replace(".", "").replace(",", "");

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new PromotionValidationException(fieldName + " phải là số hợp lệ!");
        }
    }

    /**
     * Validate khoảng giá và trả về danh sách sản phẩm đang bán phù hợp.
     * <ul>
     * <li>giaFrom bắt buộc, phải &gt; 0.</li>
     * <li>giaDen tùy chọn; nếu có thì phải &ge; giaFrom.</li>
     * <li>Phải tìm được ít nhất 1 sản phẩm.</li>
     * </ul>
     *
     * @param giaFrom giá tối thiểu (bắt buộc, &gt; 0).
     * @param giaDen giá tối đa (null = không giới hạn trên).
     * @return danh sách {@link SanPham} phù hợp.
     * @throws PromotionValidationException nếu khoảng giá không hợp lệ hoặc
     * không có sản phẩm.
     */
    @Transactional(readOnly = true)
    public List<SanPham> findProductsByPriceRange(BigDecimal giaFrom, BigDecimal giaDen) {
        if (giaFrom == null) {
            throw new PromotionValidationException("Giá từ không được để trống!");
        }
        if (giaFrom.compareTo(BigDecimal.ZERO) == 0) {
            throw new PromotionValidationException("Giá từ phải lớn hơn 0!");
        }
        if (giaFrom.compareTo(BigDecimal.ZERO) < 0) {
            throw new PromotionValidationException("Giá từ không được là số âm!");
        }
        if (giaDen != null && giaDen.compareTo(giaFrom) < 0) {
            throw new PromotionValidationException("Giá đến phải lớn hơn hoặc bằng Giá từ!");
        }
        List<SanPham> result = sanPhamRepository.findActiveByPriceRange(giaFrom, giaDen);
        if (result.isEmpty()) {
            throw new PromotionValidationException(
                    "Không tìm thấy sản phẩm nào đang bán có giá trong khoảng đã nhập. Vui lòng kiểm tra lại khoảng giá.");
        }
        return result;
    }

    private void validateCampaignDates(LocalDateTime start, LocalDateTime end, boolean isUpdate) {
        if (start == null || end == null) {
            throw new PromotionValidationException("Thời gian bắt đầu và kết thúc không được để trống!");
        }
        if (!isUpdate && start.isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new PromotionValidationException("Thời gian bắt đầu không được nằm trong quá khứ!");
        }
        if (!end.isAfter(start)) {
            throw new PromotionValidationException("Ngày bắt đầu phải trước ngày kết thúc!");
        }
    }

    /**
     * Kiểm tra xem các sản phẩm được chọn có đang trong một đợt giảm giá ĐANG
     * HOẠT ĐỘNG nào khác trong khoảng thời gian [start, end] không.
     *
     * <p>
     * Mục đích: ngăn tình trạng một sản phẩm bị áp dụng hai đợt giảm giá chồng
     * nhau cùng lúc, gây ra kết quả giá không xác định.</p>
     *
     * <p>
     * Điều kiện chồng chéo (overlap):
     * {@code campaignStart < end && campaignEnd > start}.</p>
     *
     * @param productIds danh sách ID sản phẩm cần kiểm tra.
     * @param start ngày bắt đầu của đợt mới/đang sửa.
     * @param end ngày kết thúc của đợt mới/đang sửa.
     * @param excludeCampaignId ID đợt đang được chỉnh sửa (để bỏ qua chính nó),
     * truyền {@code null} khi tạo mới.
     * @throws PromotionValidationException nếu phát hiện sản phẩm bị chồng đợt.
     */
    public void checkCampaignOverlaps(List<Integer> productIds, LocalDateTime start, LocalDateTime end, Integer excludeCampaignId) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        List<DotGiamGia> allCampaigns = dotGiamGiaDAO.findAll();
        for (DotGiamGia campaign : allCampaigns) {
            // Bỏ qua đợt đã vô hiệu hóa
            if (!campaign.getActive()) {
                continue;
            }
            // Bỏ qua chính đợt đang được sửa
            if (excludeCampaignId != null && campaign.getId().equals(excludeCampaignId)) {
                continue;
            }

            // Kiểm tra khoảng thời gian có giao nhau không
            boolean overlaps = campaign.getNgayBatDau().isBefore(end) && campaign.getNgayKetThuc().isAfter(start);
            if (overlaps) {
                // Thu thập tên các sản phẩm bị xung đột
                List<String> conflictedProductNames = new ArrayList<>();
                for (SanPham sp : campaign.getSanPhams()) {
                    if (productIds.contains(sp.getId())) {
                        conflictedProductNames.add(sp.getTenSanPham());
                    }
                }

                if (!conflictedProductNames.isEmpty()) {
                    throw new PromotionValidationException(String.format(
                            "Sản phẩm: %s đã được gán cho chiến dịch '%s' đang hoạt động trong khoảng %s - %s. Không được đè đợt giảm giá lên nhau!",
                            String.join(", ", conflictedProductNames),
                            campaign.getTenChienDich(),
                            campaign.getNgayBatDau().format(DATE_FORMATTER),
                            campaign.getNgayKetThuc().format(DATE_FORMATTER)
                    ));
                }
            }
        }
    }

    /**
     * Định dạng trạng thái của đợt giảm giá thành chuỗi để lưu vào audit log.
     * Bao gồm: tên, loại, % giảm, ngày bắt đầu, ngày kết thúc, số SP, trạng
     * thái active.
     *
     * @param dgg đợt giảm giá cần format.
     * @return chuỗi mô tả trạng thái.
     */
    private String formatCampaignState(DotGiamGia dgg) {
        int productCount = dgg.getSanPhams() != null ? dgg.getSanPhams().size() : 0;
        return String.format("Tên: %s, %s Giảm: %d, Bắt đầu: %s, Kết thúc: %s, Số SP: %d, Active: %b",
                dgg.getTenChienDich(),
                "Theo Phần Trăm".equals(dgg.getLoaiGiamGia()) ? "Phần Trăm" : "Loại Khác",
                dgg.getPhanTramGiam(),
                dgg.getNgayBatDau().format(DATE_FORMATTER),
                dgg.getNgayKetThuc().format(DATE_FORMATTER),
                productCount,
                dgg.getActive());
    }

    // ==========================================
    // PHIẾU GIẢM GIÁ (VOUCHER) – CÁC PHƯƠNG THỨC
    // ==========================================
    /**
     * Lấy toàn bộ danh sách phiếu giảm giá (kể cả đã vô hiệu hóa).
     *
     * @return danh sách tất cả {@link PhieuGiamGia}.
     */
    @Transactional(readOnly = true)
    public List<PhieuGiamGia> getAllPhieuGiamGia() {
        return phieuGiamGiaRepository.findAll();
    }

    /**
     * Tìm một phiếu giảm giá theo ID.
     *
     * @param id ID phiếu cần tìm.
     * @return phiếu giảm giá tương ứng.
     * @throws RuntimeException nếu không tìm thấy.
     */
    @Transactional(readOnly = true)
    public PhieuGiamGia getPhieuGiamGiaById(Integer id) {
        return phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá id: " + id));
    }

    /**
     * Tạo mới một phiếu giảm giá.
     *
     * <p>
     * Luồng xử lý:</p>
     * <ol>
     * <li>Chuyển mã phiếu sang CHỮ IN HOA.</li>
     * <li>Validate toàn bộ trường qua {@link #validateVoucherInputs}.</li>
     * <li>Kiểm tra không trùng mã với phiếu đã có trong hệ thống.</li>
     * <li>Với voucher VND: bắt buộc {@code giaTriGiamToiDa = null} (không áp
     * dụng cap).</li>
     * <li>Lưu phiếu và ghi audit log.</li>
     * </ol>
     *
     * @param maPhieu mã phiếu (chuyển thành in hoa tự động).
     * @param giaTri giá trị giảm (% hoặc VNĐ tùy {@code donVi}).
     * @param donVi đơn vị: "%" hoặc "VND".
     * @param start ngày bắt đầu hiệu lực.
     * @param end ngày hết hạn.
     * @param soLuongConLai số lượng phiếu phát hành (phải > 0).
     * @param giaTriDonHangToiThieu giá trị đơn tối thiểu (0 = không yêu cầu).
     * @param loaiGiamGia "Giảm trực tiếp" hoặc "Giảm phần trăm".
     * @param giaTriGiamToiDa trần giảm (chỉ dùng khi {@code donVi = "%"}).
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     * @return phiếu giảm giá vừa tạo.
     * @throws PromotionValidationException nếu dữ liệu không hợp lệ.
     */
    @Transactional
    public PhieuGiamGia createPhieuGiamGia(String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia,
            BigDecimal giaTriGiamToiDa,
            Integer actingTaiKhoanId, String ipAddress) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        // Chuẩn hóa mã phiếu: luôn in hoa để nhất quán khi so sánh
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // 1. Validate toàn bộ đầu vào (isUpdate = false → số lượng phải > 0, ngày bắt đầu không trong quá khứ)
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, false);

        // 2. Kiểm tra mã phiếu không trùng với phiếu nào đã tồn tại
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCase(uppercaseCode)) {
            throw new PromotionValidationException("Mã phiếu giảm giá '" + uppercaseCode + "' đã tồn tại trên hệ thống!");
        }

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv == null) {
            throw new PromotionValidationException("Tài khoản đang thực hiện không có thông tin nhân viên!");
        }

        String normalizedDonVi = donVi.trim();
        String normalizedLoaiGiamGia = loaiGiamGia == null ? null : loaiGiamGia.trim();

        // Với voucher VND (giảm trực tiếp): trần cap không áp dụng → set null
        BigDecimal resolvedCap = "%".equals(normalizedDonVi) ? giaTriGiamToiDa : null;

        // 3. Tạo và lưu entity
        PhieuGiamGia pgg = new PhieuGiamGia();
        pgg.setMaPhieu(uppercaseCode);
        pgg.setGiaTri(giaTri);
        pgg.setDonVi(normalizedDonVi);
        pgg.setNgayBatDau(start);
        pgg.setNgayKetThuc(end);
        pgg.setSoLuongConLai(soLuongConLai);
        pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu);
        pgg.setLoaiGiamGia(normalizedLoaiGiamGia);
        pgg.setGiaTriGiamToiDa(resolvedCap);
        pgg.setNhanVien(nv);
        pgg.setActive(true);

        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        // 4. Ghi audit log
        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", saved.getId().longValue(), "INSERT",
                null, formatVoucherState(saved), ipAddress, "Tạo mới voucher: " + uppercaseCode);

        if (saved.getActive() != null && saved.getActive()) {
            try {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                newsletterService.sendVoucherEmailAsync(saved.getId());
                            } catch (Exception e) {
                                log.error("[Bản tin khuyến mãi] Lỗi kích hoạt gửi email thông báo sau commit cho voucher #{}: {}", saved.getId(), e.getMessage(), e);
                            }
                        }
                    });
                } else {
                    newsletterService.sendVoucherEmailAsync(saved.getId());
                }
            } catch (Exception e) {
                log.error("[Bản tin khuyến mãi] Lỗi thiết lập thông báo email cho voucher #{}: {}", saved.getId(), e.getMessage(), e);
            }
        }

        return saved;
    }

    /**
     * Cập nhật thông tin phiếu giảm giá đã tồn tại.
     *
     * <p>
     * Khi lưu thành công, phiếu tự động được kích hoạt lại
     * ({@code active = true}). Cho phép số lượng = 0 khi sửa (admin muốn ngừng
     * nhận thêm mà không xóa phiếu).</p>
     *
     * @param id ID phiếu cần sửa.
     * @param maPhieu mã phiếu mới.
     * @param giaTri giá trị giảm mới.
     * @param donVi đơn vị mới.
     * @param start ngày bắt đầu mới.
     * @param end ngày hết hạn mới.
     * @param soLuongConLai số lượng mới (có thể = 0 khi sửa).
     * @param giaTriDonHangToiThieu giá trị đơn tối thiểu mới.
     * @param loaiGiamGia loại mới.
     * @param giaTriGiamToiDa trần giảm mới.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     * @return phiếu giảm giá sau khi cập nhật.
     */
    @Transactional
    public PhieuGiamGia updatePhieuGiamGia(Integer id, String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia,
            BigDecimal giaTriGiamToiDa,
            Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);

        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        String uppercaseCode = maPhieu.trim().toUpperCase();

        // Validate – isUpdate = true → số lượng cho phép = 0
        validateVoucherInputs(uppercaseCode, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, true);

        // Kiểm tra trùng mã với phiếu KHÁC (loại trừ chính phiếu đang sửa)
        if (phieuGiamGiaRepository.existsByMaPhieuIgnoreCaseAndIdNot(uppercaseCode, id)) {
            throw new PromotionValidationException("Mã phiếu giảm giá '" + uppercaseCode + "' đã được sử dụng bởi voucher khác!");
        }

        // Chụp trạng thái cũ để ghi audit log
        String oldState = formatVoucherState(pgg);

        String normalizedDonVi = donVi.trim();
        String normalizedLoaiGiamGia = loaiGiamGia == null ? null : loaiGiamGia.trim();

        // Voucher VND không có cap
        BigDecimal resolvedCap = "%".equals(normalizedDonVi) ? giaTriGiamToiDa : null;

        // Cập nhật và lưu – tự động kích hoạt lại phiếu
        pgg.setMaPhieu(uppercaseCode);
        pgg.setGiaTri(giaTri);
        pgg.setDonVi(normalizedDonVi);
        pgg.setNgayBatDau(start);
        pgg.setNgayKetThuc(end);
        pgg.setSoLuongConLai(soLuongConLai);
        pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieu == null ? BigDecimal.ZERO : giaTriDonHangToiThieu);
        pgg.setLoaiGiamGia(normalizedLoaiGiamGia);
        pgg.setGiaTriGiamToiDa(resolvedCap);
        pgg.setActive(true);

        PhieuGiamGia updated = phieuGiamGiaRepository.save(pgg);

        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", updated.getId().longValue(), "UPDATE",
                oldState, formatVoucherState(updated), ipAddress, "Cập nhật voucher: " + uppercaseCode);

        return updated;
    }

    /**
     * Vô hiệu hóa thủ công một phiếu giảm giá ({@code active = false}). Phiếu
     * vẫn còn trong DB nhưng khách không thể dùng dù còn hạn và còn số lượng.
     *
     * @param id ID phiếu cần vô hiệu hóa.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     */
    @Transactional
    public void deactivatePhieuGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);
        String oldState = formatVoucherState(pgg);

        pgg.setActive(false);
        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", id.longValue(), "UPDATE",
                oldState, formatVoucherState(saved), ipAddress, "Vô hiệu hóa voucher: " + pgg.getMaPhieu());
    }

    /**
     * Xóa logic (soft delete) một phiếu giảm giá. Giống
     * {@link #deactivatePhieuGiamGia} về kỹ thuật, nhưng audit log ghi
     * "DELETE". Hệ thống không xóa cứng để bảo toàn lịch sử đơn hàng đã dùng
     * phiếu này.
     *
     * @param id ID phiếu cần xóa.
     * @param actingTaiKhoanId ID tài khoản admin thực hiện.
     * @param ipAddress IP thực hiện yêu cầu.
     */
    @Transactional
    public void deletePhieuGiamGia(Integer id, Integer actingTaiKhoanId, String ipAddress) {
        PhieuGiamGia pgg = getPhieuGiamGiaById(id);
        String oldState = formatVoucherState(pgg);

        pgg.setActive(false); // Soft delete
        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        writeEditLog(actingTaiKhoanId, "PhieuGiamGia", id.longValue(), "DELETE",
                oldState, formatVoucherState(saved), ipAddress, "Xóa logic voucher: " + pgg.getMaPhieu());
    }

    /**
     * Kiểm tra một {@link BigDecimal} có phải số thập phân không (scale > 0 sau
     * khi bỏ số 0 thừa). Voucher chỉ nhận số nguyên (không có phần lẻ) để đơn
     * giản hóa tính toán tiền VNĐ.
     *
     * @param bd giá trị cần kiểm tra.
     * @param errorMessage thông báo lỗi nếu là số thập phân.
     * @throws PromotionValidationException nếu giá trị có phần thập phân.
     */
    private void checkNotDecimal(BigDecimal bd, String errorMessage) {
        if (bd != null && bd.stripTrailingZeros().scale() > 0) {
            throw new PromotionValidationException(errorMessage);
        }
    }

    /**
     * Validate toàn bộ đầu vào khi tạo mới hoặc cập nhật phiếu giảm giá.
     *
     * <p>
     * Kiểm tra các ràng buộc sau (theo thứ tự):</p>
     * <ol>
     * <li>Mã phiếu: không trống, chỉ gồm [A-Z0-9_], độ dài 2–50.</li>
     * <li>Đơn vị: chỉ nhận "%" hoặc "VND".</li>
     * <li>Loại giảm: chỉ nhận "Giảm trực tiếp" hoặc "Giảm phần trăm".</li>
     * <li>Đơn vị và loại phải nhất quán (% ↔ Giảm phần trăm, VND ↔ Giảm trực
     * tiếp).</li>
     * <li>Giá trị giảm: không âm, không thập phân, trong giới hạn cho
     * phép.</li>
     * <li>Ngày: bắt buộc, ngày bắt đầu phải trước kết thúc.</li>
     * <li>Số lượng: khi tạo mới phải > 0; khi sửa cho phép = 0.</li>
     * <li>Giá trị đơn tối thiểu: không âm, không thập phân.</li>
     * <li>Trần giảm (cap): bắt buộc khi voucher %, không được có khi voucher
     * VND.</li>
     * </ol>
     *
     * @param maPhieu mã phiếu đã được in hoa.
     * @param giaTri giá trị giảm.
     * @param donVi đơn vị ("%" hoặc "VND").
     * @param start ngày bắt đầu.
     * @param end ngày kết thúc.
     * @param soLuongConLai số lượng phiếu.
     * @param giaTriDonHangToiThieu giá trị đơn hàng tối thiểu.
     * @param loaiGiamGia phân loại voucher.
     * @param giaTriGiamToiDa trần giảm (chỉ dùng khi %).
     * @param isUpdate {@code true} nếu đang sửa (số lượng = 0 được chấp nhận).
     */
    private void validateVoucherInputs(String maPhieu, BigDecimal giaTri, String donVi,
            LocalDateTime start, LocalDateTime end, Integer soLuongConLai,
            BigDecimal giaTriDonHangToiThieu, String loaiGiamGia, BigDecimal giaTriGiamToiDa, boolean isUpdate) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new PromotionValidationException("Mã phiếu không được để trống!");
        }
        // Mã chỉ gồm chữ in hoa, số và dấu gạch dưới, dài 2–50 ký tự
        if (!maPhieu.matches("^[A-Z0-9_]{2,50}$")) {
            throw new PromotionValidationException("Mã phiếu giảm giá không hợp lệ! Chỉ cho phép ký tự chữ in hoa, số và dấu gạch dưới từ 2 đến 50 ký tự.");
        }
        if (donVi == null || donVi.trim().isEmpty()) {
            throw new PromotionValidationException("Đơn vị giảm giá không được để trống!");
        }
        String normalizedDonVi = donVi.trim();
        if (!"%".equals(normalizedDonVi) && !"VND".equals(normalizedDonVi)) {
            throw new PromotionValidationException("Đơn vị giảm giá không hợp lệ! Chỉ cho phép '%' hoặc 'VND'.");
        }

        if (loaiGiamGia == null || loaiGiamGia.trim().isEmpty()) {
            throw new PromotionValidationException("Loại giảm giá không được để trống!");
        }
        String normalizedLoaiGiamGia = loaiGiamGia.trim();
        boolean isPercentVoucher = "%".equals(normalizedDonVi);
        boolean isPercentType = "Giảm phần trăm".equals(normalizedLoaiGiamGia);
        if (isPercentVoucher != isPercentType) {
            throw new PromotionValidationException("Đơn vị giảm giá và loại giảm giá không nhất quán!");
        }

        // Validate giá trị giảm theo từng loại đơn vị
        if (giaTri == null) {
            throw new PromotionValidationException("Giá trị giảm giá không được để trống!");
        }
        if (isPercentVoucher) {
            checkNotDecimal(giaTri, "Giá trị giảm % phải là số nguyên, không được là số thập phân!");
            if (giaTri.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm % không được là số âm!");
            }
            if (giaTri.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm giá phải lớn hơn 0!");
            }
            if (giaTri.compareTo(new BigDecimal(PromotionValidationConstants.MAX_VOUCHER_PERCENT)) > 0) {
                throw new PromotionValidationException("Giá trị giảm % không được vượt quá " + PromotionValidationConstants.MAX_VOUCHER_PERCENT + "%!");
            }
        } else {
            checkNotDecimal(giaTri, "Giá trị giảm tiền (VND) phải là số nguyên, không được là số thập phân!");
            if (giaTri.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm tiền (VND) không được là số âm!");
            }
            if (giaTri.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm giá phải lớn hơn 0!");
            }
            if (giaTri.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
                throw new PromotionValidationException("Giá trị giảm tiền không được vượt quá 100,000,000 VNĐ!");
            }
        }

        // Validate ngày
        if (start == null || end == null) {
            throw new PromotionValidationException("Thời gian bắt đầu và kết thúc không được để trống!");
        }
        if (!isUpdate && start.isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new PromotionValidationException("Thời gian bắt đầu không được nằm trong quá khứ!");
        }
        if (!end.isAfter(start)) {
            throw new PromotionValidationException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        // Validate số lượng: khi tạo mới phải > 0; khi sửa cho phép = 0
        if (soLuongConLai == null) {
            throw new PromotionValidationException("Số lượng voucher không được để trống!");
        }
        if (isUpdate) {
            if (soLuongConLai < 0) {
                throw new PromotionValidationException("Số lượng voucher không được là số âm!");
            }
        } else {
            if (soLuongConLai <= 0) {
                throw new PromotionValidationException("Số lượng voucher phải lớn hơn 0!");
            }
        }
        if (soLuongConLai > PromotionValidationConstants.MAX_QUANTITY) {
            throw new PromotionValidationException("Số lượng voucher không được vượt quá " + PromotionValidationConstants.MAX_QUANTITY + "!");
        }

        // Validate giá trị đơn tối thiểu
        if (giaTriDonHangToiThieu == null) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được để trống!");
        }
        checkNotDecimal(giaTriDonHangToiThieu, "Giá trị đơn hàng tối thiểu phải là số nguyên, không được là số thập phân!");
        if (giaTriDonHangToiThieu.compareTo(BigDecimal.ZERO) < 0) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được là số âm!");
        }
        if (giaTriDonHangToiThieu.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
            throw new PromotionValidationException("Giá trị đơn hàng tối thiểu không được vượt quá 100,000,000 VNĐ!");
        }

        // Validate trần giảm (cap): bắt buộc với voucher %, không dùng với voucher VND
        if (isPercentVoucher) {
            if (giaTriGiamToiDa == null) {
                throw new PromotionValidationException("Giá trị giảm tối đa là bắt buộc đối với voucher giảm theo phần trăm!");
            }
            checkNotDecimal(giaTriGiamToiDa, "Giá trị giảm tối đa phải là số nguyên, không được là số thập phân!");
            if (giaTriGiamToiDa.compareTo(BigDecimal.ZERO) < 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa không được là số âm!");
            }
            if (giaTriGiamToiDa.compareTo(BigDecimal.ZERO) == 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa phải lớn hơn 0!");
            }
            if (giaTriGiamToiDa.compareTo(PromotionValidationConstants.MAX_VND_VALUE) > 0) {
                throw new PromotionValidationException("Giá trị giảm tối đa không được vượt quá 100,000,000 VNĐ!");
            }
        } else {
            // Voucher VND không được truyền cap – tránh nhầm lẫn
            if (giaTriGiamToiDa != null) {
                throw new PromotionValidationException("Voucher giảm trực tiếp (VND) không được có giá trị giảm tối đa!");
            }
        }
    }

    /**
     * Định dạng trạng thái phiếu giảm giá thành chuỗi để lưu vào audit log. Bao
     * gồm: mã, giá trị, đơn vị, hạn dùng, tối thiểu, tối đa, số lượng, trạng
     * thái active.
     *
     * @param pgg phiếu cần format.
     * @return chuỗi mô tả trạng thái.
     */
    private String formatVoucherState(PhieuGiamGia pgg) {
        String capStr = (pgg.getGiaTriGiamToiDa() == null) ? "Không giới hạn" : pgg.getGiaTriGiamToiDa().toPlainString();
        return String.format("Mã: %s, Giá trị: %s %s, Hạn dùng: %s - %s, Tối thiểu: %s, Tối đa: %s, Số lượng: %d, Active: %b",
                pgg.getMaPhieu(),
                pgg.getGiaTri().toString(),
                pgg.getDonVi(),
                pgg.getNgayBatDau().format(DATE_FORMATTER),
                pgg.getNgayKetThuc().format(DATE_FORMATTER),
                pgg.getGiaTriDonHangToiThieu().toString(),
                capStr,
                pgg.getSoLuongConLai(),
                pgg.getActive());
    }

    // ==========================================
    // GHI AUDIT LOG (DÙNG CHUNG)
    // ==========================================
    /**
     * Ghi một bản ghi vào EditLog (audit trail) thông qua {@link AuditService}.
     *
     * <p>
     * Mỗi thao tác tạo/sửa/xóa khuyến mãi đều gọi method này để lưu lại: ai
     * làm, làm gì, trên bản ghi nào, từ trạng thái nào sang trạng thái nào, từ
     * IP nào.</p>
     *
     * @param actingTaiKhoanId ID tài khoản thực hiện hành động.
     * @param tenBang tên bảng bị tác động ("DotGiamGia" hoặc "PhieuGiamGia").
     * @param idBanGhi ID bản ghi bị tác động.
     * @param hanhDong hành động: "INSERT", "UPDATE" hoặc "DELETE".
     * @param giaTriCu chuỗi mô tả trạng thái trước khi sửa (null khi INSERT).
     * @param giaTriMoi chuỗi mô tả trạng thái sau khi sửa.
     * @param ipAddress địa chỉ IP của người thực hiện.
     * @param ghiChu ghi chú bổ sung (ví dụ: "Tạo mới đợt giảm giá: Flash
     * Sale").
     */
    private void writeEditLog(Integer actingTaiKhoanId, String tenBang, Long idBanGhi, String hanhDong,
            String giaTriCu, String giaTriMoi, String ipAddress, String ghiChu) {
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId).orElse(null);
        if (actingUser != null) {
            auditService.log(actingTaiKhoanId, tenBang, idBanGhi, hanhDong, giaTriCu, giaTriMoi, ipAddress, ghiChu, actingUser.getVaiTro());
        }
    }
}
