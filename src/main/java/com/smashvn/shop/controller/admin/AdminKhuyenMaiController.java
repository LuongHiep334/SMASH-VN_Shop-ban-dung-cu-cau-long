package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.exception.PromotionValidationException;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.admin.AdminKhuyenMaiService;
import com.smashvn.shop.util.PromotionValidationConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Controller xử lý các yêu cầu HTTP liên quan đến QUẢN LÝ KHUYẾN MÃI của admin.
 *
 * <p>
 * Base URL: {@code /admin/khuyen-mai}</p>
 *
 * <p>
 * Bao gồm hai nhóm endpoint:</p>
 * <ul>
 * <li><b>Đợt giảm giá (Campaign)</b> – CRUD tại {@code /dot-giam-gia/**}</li>
 * <li><b>Phiếu giảm giá (Voucher)</b> – CRUD tại
 * {@code /phieu-giam-gia/**}</li>
 * </ul>
 *
 * <p>
 * Controller này KHÔNG tự validate logic nghiệp vụ – mọi validation được ủy
 * quyền cho {@link AdminKhuyenMaiService}. Controller chỉ:
 * <ol>
 * <li>Chuyển đổi chuỗi từ form sang kiểu dữ liệu phù hợp (parse).</li>
 * <li>Gọi service xử lý.</li>
 * <li>Điều hướng tới trang thành công hoặc hiển thị lại form với thông báo
 * lỗi.</li>
 * </ol>
 * </p>
 */
@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class AdminKhuyenMaiController {

    /*
     * Tóm tắt các nhóm phương thức:
     *  - viewThem*, viewSua*      : GET – hiển thị form thêm/sửa (đẩy dữ liệu vào model).
     *  - processThem*, processSua*: POST – nhận form, parse, gọi service, redirect hoặc hiển thị lỗi.
     *  - processDeactivate*       : POST – vô hiệu hóa thủ công (active = false).
     *  - processDelete*           : POST – xóa logic (soft delete, cũng set active = false).
     *  - parseAndValidateInteger  : helper parse chuỗi → Integer, kiểm tra giới hạn min/max.
     *  - parseAndValidateBigDecimal: helper parse chuỗi → BigDecimal, kiểm tra giới hạn.
     */
    private final AdminKhuyenMaiService adminKhuyenMaiService;
    private final SanPhamRepository sanPhamRepository;

    // ==========================================
    // ĐỢT GIẢM GIÁ (CAMPAIGN) – ENDPOINTS
    // ==========================================
    /**
     * [GET] Hiển thị form TẠO MỚI đợt giảm giá. Đẩy danh sách tất cả sản phẩm
     * vào model để hiển thị checkbox chọn sản phẩm.
     *
     * @param model Spring MVC model.
     * @return tên template {@code admin/dotgiamgia-add}.
     */
    @GetMapping("/dot-giam-gia/them")
    public String viewThemDotGiamGia(Model model) {
        // Dùng findAllActiveProducts() thay vì findAll() để không hiển thị SP ngừng bán
        model.addAttribute("sanPhams", sanPhamRepository.findAllActiveProducts());
        return "admin/dotgiamgia-add";
    }

    /**
     * [GET] API xem trước sản phẩm phù hợp theo khoảng giá (AJAX). Trả về JSON
     * với: - count: tổng số sản phẩm tìm thấy. - sanPhams: tối đa 10 sản phẩm
     * đầu (id + tenSanPham). HTTP 400 kèm {"error": "..."} nếu dữ liệu không
     * hợp lệ.
     *
     * @param giaFromStr Giá từ (có thể rỗng, sẽ trả lỗi 400).
     * @param giaDenStr Giá đến (tùy chọn, null = không giới hạn trên).
     * @return ResponseEntity chứa JSON.
     */
    @GetMapping("/dot-giam-gia/preview-by-price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewByPrice(
            @RequestParam(value = "giaFrom", required = false) String giaFromStr,
            @RequestParam(value = "giaDen", required = false) String giaDenStr) {
        try {
            BigDecimal giaFrom = adminKhuyenMaiService.parseVndCurrency(giaFromStr, "Giá từ", false);
            BigDecimal giaDen = adminKhuyenMaiService.parseVndCurrency(giaDenStr, "Giá đến", true);
            List<SanPham> list = adminKhuyenMaiService.findProductsByPriceRange(giaFrom, giaDen);
            int count = list.size();
            List<Map<String, Object>> preview = list.stream()
                    .limit(10)
                    .map(sp -> Map.<String, Object>of("id", sp.getId(), "tenSanPham", sp.getTenSanPham()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("count", count, "sanPhams", preview));
        } catch (PromotionValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * [POST] Xử lý form TẠO MỚI đợt giảm giá.
     *
     * <p>
     * Luồng:</p>
     * <ol>
     * <li>Lấy ID tài khoản admin từ session và IP từ request.</li>
     * <li>Parse chuỗi ngày → {@link LocalDateTime}.</li>
     * <li>Parse và validate % giảm qua {@link #parseAndValidateInteger}.</li>
     * <li>Gọi {@code adminKhuyenMaiService.createDotGiamGia()} để lưu.</li>
     * <li>Thành công → redirect kèm param {@code ?themChienDichThanhCong}.</li>
     * <li>Thất bại → hiển thị lại form với thông báo lỗi và dữ liệu người dùng
     * đã nhập.</li>
     * </ol>
     *
     * @param tenChienDich tên chiến dịch từ form.
     * @param ngayBatDauStr ngày bắt đầu dạng chuỗi ISO (yyyy-MM-ddTHH:mm).
     * @param ngayKetThucStr ngày kết thúc dạng chuỗi ISO.
     * @param phanTramGiamStr % giảm dạng chuỗi.
     * @param loaiGiamGia loại giảm giá.
     * @param productIds danh sách ID sản phẩm được chọn.
     * @param session HTTP session (lấy idNguoiDung).
     * @param request HTTP request (lấy remote IP).
     * @param model Spring MVC model.
     * @return redirect hoặc tên template form.
     */
    @PostMapping("/dot-giam-gia/them")
    public String processThemDotGiamGia(
            @RequestParam("tenChienDich") String tenChienDich,
            @RequestParam(value = "ngayBatDau", required = false) String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("phanTramGiam") String phanTramGiamStr,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            @RequestParam(value = "kieuApDung", defaultValue = "MANUAL") String kieuApDungStr,
            @RequestParam(value = "productIds", required = false) List<Integer> productIds,
            @RequestParam(value = "giaFrom", required = false) String giaFromStr,
            @RequestParam(value = "giaDen", required = false) String giaDenStr,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            // Parse chuỗi ngày ISO thành LocalDateTime (null nếu rỗng)
            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            // Parse và validate % giảm: phải là số nguyên, không âm, trong phạm vi cho phép
            Integer phanTramGiam = parseAndValidateInteger(phanTramGiamStr, "Phần trăm giảm giá", 1, PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT, false);

            // Parse giaFrom/giaDen CHỈ khi kiểu PRICE_RANGE
            // Nếu MANUAL thì không parse để tránh lỗi khi các ô này rỗng
            BigDecimal giaFrom = null;
            BigDecimal giaDen = null;
            if ("PRICE_RANGE".equalsIgnoreCase(kieuApDungStr)) {
                giaFrom = adminKhuyenMaiService.parseVndCurrency(giaFromStr, "Giá từ", false);
                giaDen = adminKhuyenMaiService.parseVndCurrency(giaDenStr, "Giá đến", true);
            }

            adminKhuyenMaiService.createDotGiamGia(tenChienDich, start, end, phanTramGiam, loaiGiamGia,
                    kieuApDungStr, productIds, giaFrom, giaDen, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?themChienDichThanhCong";
        } catch (Exception e) {
            // Lỗi xảy ra → hiển thị lại form với thông báo lỗi và giữ lại dữ liệu đã nhập
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("sanPhams", sanPhamRepository.findAllActiveProducts());
            model.addAttribute("tenChienDich", tenChienDich);
            model.addAttribute("ngayBatDau", ngayBatDauStr);
            model.addAttribute("ngayKetThuc", ngayKetThucStr);
            model.addAttribute("phanTramGiam", phanTramGiamStr);
            model.addAttribute("loaiGiamGia", loaiGiamGia);
            model.addAttribute("kieuApDung", kieuApDungStr);
            model.addAttribute("giaFrom", giaFromStr);
            model.addAttribute("giaDen", giaDenStr);
            model.addAttribute("selectedProductIds", productIds);
            return "admin/dotgiamgia-add";
        }
    }

    /**
     * [GET] Hiển thị form CHỈNH SỬA đợt giảm giá theo ID. Load thông tin đợt
     * giảm giá và danh sách sản phẩm đang được chọn vào model.
     *
     * @param id ID đợt giảm giá cần sửa (lấy từ URL path).
     * @param model Spring MVC model.
     * @return tên template {@code admin/dotgiamgia-edit}, hoặc redirect về danh
     * sách nếu lỗi.
     */
    @GetMapping("/dot-giam-gia/sua/{id}")
    public String viewSuaDotGiamGia(@PathVariable("id") Integer id, Model model) {
        try {
            DotGiamGia dgg = adminKhuyenMaiService.getDotGiamGiaById(id);
            List<SanPham> activeProducts = sanPhamRepository.findAllActiveProducts();
            // Phát hiện SP ngưng bán đang gắn với chiến dịch này
            Set<Integer> activeIds = activeProducts.stream()
                    .map(SanPham::getId).collect(Collectors.toSet());
            List<String> discontinuedNames = dgg.getSanPhams().stream()
                    .filter(sp -> !activeIds.contains(sp.getId()))
                    .map(SanPham::getTenSanPham)
                    .collect(Collectors.toList());
            if (!discontinuedNames.isEmpty()) {
                model.addAttribute("canhBaoNgungBan",
                        "Một số sản phẩm trong chiến dịch hiện đã ngừng bán và sẽ không được hiển thị trong danh sách chọn: "
                        + String.join(", ", discontinuedNames));
            }
            model.addAttribute("campaign", dgg);
            model.addAttribute("sanPhams", activeProducts);
            // Danh sách ID sản phẩm đang được gán cho đợt này (chỉ giữ lại SP đang bán)
            model.addAttribute("selectedProductIds", dgg.getSanPhams().stream()
                    .filter(sp -> activeIds.contains(sp.getId()))
                    .map(SanPham::getId).collect(Collectors.toList()));
            return "admin/dotgiamgia-edit";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * [POST] Xử lý form CHỈNH SỬA đợt giảm giá. Tương tự
     * {@link #processThemDotGiamGia} nhưng gọi {@code updateDotGiamGia}. Khi có
     * lỗi, nạp lại thông tin hiện tại từ DB rồi ghi đè bằng dữ liệu người dùng
     * vừa nhập để hiển thị lại form với dữ liệu cũ cùng thông báo lỗi.
     *
     * @param id ID đợt giảm giá cần sửa.
     * @param tenChienDich tên chiến dịch mới.
     * @param ngayBatDauStr ngày bắt đầu mới (ISO string).
     * @param ngayKetThucStr ngày kết thúc mới (ISO string).
     * @param phanTramGiamStr % giảm mới (string).
     * @param loaiGiamGia loại giảm giá mới.
     * @param productIds danh sách sản phẩm mới.
     * @param session HTTP session.
     * @param request HTTP request.
     * @param model Spring MVC model.
     * @return redirect hoặc tên template form.
     */
    @PostMapping("/dot-giam-gia/sua/{id}")
    public String processSuaDotGiamGia(
            @PathVariable("id") Integer id,
            @RequestParam("tenChienDich") String tenChienDich,
            @RequestParam(value = "ngayBatDau", required = false) String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("phanTramGiam") String phanTramGiamStr,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            @RequestParam(value = "productIds", required = false) List<Integer> productIds,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            Integer phanTramGiam = parseAndValidateInteger(phanTramGiamStr, "Phần trăm giảm giá", 1, PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT, false);

            adminKhuyenMaiService.updateDotGiamGia(id, tenChienDich, start, end, phanTramGiam, loaiGiamGia, productIds, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?suaChienDichThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            // Nạp lại từ DB để có dữ liệu gốc, rồi ghi đè bằng dữ liệu người dùng vừa nhập
            DotGiamGia dgg = adminKhuyenMaiService.getDotGiamGiaById(id);
            dgg.setTenChienDich(tenChienDich);
            try {
                dgg.setNgayBatDau(ngayBatDauStr == null || ngayBatDauStr.isEmpty() ? null : LocalDateTime.parse(ngayBatDauStr));
            } catch (Exception ignored) {
            }
            try {
                dgg.setNgayKetThuc(ngayKetThucStr == null || ngayKetThucStr.isEmpty() ? null : LocalDateTime.parse(ngayKetThucStr));
            } catch (Exception ignored) {
            }
            try {
                dgg.setPhanTramGiam(phanTramGiamStr == null || phanTramGiamStr.isEmpty() ? null : Integer.parseInt(phanTramGiamStr));
            } catch (Exception ignored) {
            }
            dgg.setLoaiGiamGia(loaiGiamGia);
            model.addAttribute("campaign", dgg);
            model.addAttribute("sanPhams", sanPhamRepository.findAllActiveProducts());
            model.addAttribute("selectedProductIds", productIds);
            return "admin/dotgiamgia-edit";
        }
    }

    /**
     * [POST] Vô hiệu hóa một đợt giảm giá (đặt active = false). Đợt giảm giá
     * vẫn còn trong DB, có thể kích hoạt lại khi sửa và lưu. Redirect về danh
     * sách kèm param thành công hoặc lỗi.
     *
     * @param id ID đợt giảm giá cần vô hiệu hóa.
     * @param session HTTP session.
     * @param request HTTP request.
     * @return redirect URL.
     */
    @PostMapping("/dot-giam-gia/deactivate/{id}")
    public String processDeactivateDotGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deactivateDotGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?deactivateChienDichThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * [POST] Xóa logic (soft delete) một đợt giảm giá. Kỹ thuật giống
     * deactivate (đặt active = false), nhưng audit log ghi là "DELETE".
     *
     * @param id ID đợt giảm giá cần xóa.
     * @param session HTTP session.
     * @param request HTTP request.
     * @return redirect URL.
     */
    @PostMapping("/dot-giam-gia/delete/{id}")
    public String processDeleteDotGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deleteDotGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?xoaChienDichThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ==========================================
    // PHIẾU GIẢM GIÁ (VOUCHER) – ENDPOINTS
    // ==========================================
    /**
     * [GET] Hiển thị form TẠO MỚI phiếu giảm giá.
     *
     * @param model Spring MVC model.
     * @return tên template {@code admin/phieugiamgia-add}.
     */
    @GetMapping("/phieu-giam-gia/them")
    public String viewThemPhieuGiamGia(Model model) {
        return "admin/phieugiamgia-add";
    }

    /**
     * [POST] Xử lý form TẠO MỚI phiếu giảm giá.
     *
     * <p>
     * Luồng:</p>
     * <ol>
     * <li>Parse chuỗi ngày → {@link LocalDateTime}.</li>
     * <li>Parse và validate các trường số (giaTri, soLuongConLai,
     * giaTriDonHangToiThieu, giaTriGiamToiDa).</li>
     * <li>Gọi {@code adminKhuyenMaiService.createPhieuGiamGia()} để lưu.</li>
     * <li>Thành công → redirect {@code ?themPhieuThanhCong}.</li>
     * <li>Thất bại → hiển thị lại form với lỗi và dữ liệu đã nhập.</li>
     * </ol>
     *
     * @param maPhieu mã phiếu từ form.
     * @param giaTriStr giá trị giảm (string).
     * @param donVi đơn vị: "%" hoặc "VND".
     * @param ngayBatDauStr ngày bắt đầu (ISO string).
     * @param ngayKetThucStr ngày hết hạn (ISO string).
     * @param soLuongConLaiStr số lượng phiếu (string).
     * @param giaTriDonHangToiThieuStr giá trị đơn tối thiểu (string, optional).
     * @param giaTriGiamToiDaStr trần giảm tối đa (string, optional – bắt buộc
     * khi donVi=%).
     * @param loaiGiamGia loại giảm giá.
     * @param session HTTP session.
     * @param request HTTP request.
     * @param model Spring MVC model.
     * @return redirect hoặc tên template form.
     */
    @PostMapping("/phieu-giam-gia/them")
    public String processThemPhieuGiamGia(
            @RequestParam("maPhieu") String maPhieu,
            @RequestParam("giaTri") String giaTriStr,
            @RequestParam("donVi") String donVi,
            @RequestParam(value = "ngayBatDau", required = false) String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("soLuongConLai") String soLuongConLaiStr,
            @RequestParam(value = "giaTriDonHangToiThieu", required = false) String giaTriDonHangToiThieuStr,
            @RequestParam(value = "giaTriGiamToiDa", required = false) String giaTriGiamToiDaStr,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            // Parse và validate từng trường số:
            // giaTri: phải ≥ 1, trong giới hạn MAX_VND_VALUE (hoặc MAX_VOUCHER_PERCENT nếu %)
            BigDecimal giaTri = parseAndValidateBigDecimal(giaTriStr, "Giá trị giảm giá", BigDecimal.ONE, PromotionValidationConstants.MAX_VND_VALUE, false);
            // soLuongConLai: phải ≥ 1 khi tạo mới
            Integer soLuongConLai = parseAndValidateInteger(soLuongConLaiStr, "Số lượng voucher", 1, PromotionValidationConstants.MAX_QUANTITY, false);
            // giaTriDonHangToiThieu: có thể = 0 (không yêu cầu tối thiểu), allowZero = true
            BigDecimal giaTriDonHangToiThieu = parseAndValidateBigDecimal(giaTriDonHangToiThieuStr, "Giá trị đơn hàng tối thiểu", BigDecimal.ZERO, PromotionValidationConstants.MAX_VND_VALUE, true);
            // giaTriGiamToiDa: bắt buộc > 0 với voucher %, không bắt buộc với VND
            BigDecimal giaTriGiamToiDa = parseAndValidateBigDecimal(giaTriGiamToiDaStr, "Giá trị giảm tối đa", BigDecimal.ONE, PromotionValidationConstants.MAX_VND_VALUE, false);

            adminKhuyenMaiService.createPhieuGiamGia(maPhieu, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?themPhieuThanhCong";
        } catch (Exception e) {
            // Giữ lại dữ liệu đã nhập để hiển thị lại form, tránh user phải nhập lại từ đầu
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("maPhieu", maPhieu);
            model.addAttribute("giaTri", giaTriStr);
            model.addAttribute("donVi", donVi);
            model.addAttribute("ngayBatDau", ngayBatDauStr);
            model.addAttribute("ngayKetThuc", ngayKetThucStr);
            model.addAttribute("soLuongConLai", soLuongConLaiStr);
            model.addAttribute("giaTriDonHangToiThieu", giaTriDonHangToiThieuStr);
            model.addAttribute("giaTriGiamToiDa", giaTriGiamToiDaStr);
            model.addAttribute("loaiGiamGia", loaiGiamGia);
            return "admin/phieugiamgia-add";
        }
    }

    /**
     * [GET] Hiển thị form CHỈNH SỬA phiếu giảm giá theo ID. Load thông tin
     * phiếu vào model để pre-fill form.
     *
     * @param id ID phiếu cần sửa.
     * @param model Spring MVC model.
     * @return tên template {@code admin/phieugiamgia-edit}, hoặc redirect nếu
     * lỗi.
     */
    @GetMapping("/phieu-giam-gia/sua/{id}")
    public String viewSuaPhieuGiamGia(@PathVariable("id") Integer id, Model model) {
        try {
            PhieuGiamGia pgg = adminKhuyenMaiService.getPhieuGiamGiaById(id);
            model.addAttribute("voucher", pgg);
            return "admin/phieugiamgia-edit";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * [POST] Xử lý form CHỈNH SỬA phiếu giảm giá. Tương tự
     * {@link #processThemPhieuGiamGia} nhưng gọi {@code updatePhieuGiamGia}.
     * Khác biệt: số lượng cho phép = 0 khi sửa ({@code allowZero = true}). Khi
     * lỗi, nạp lại entity từ DB rồi ghi đè để giữ lại dữ liệu người dùng vừa
     * nhập.
     *
     * @param id ID phiếu cần sửa.
     * @param maPhieu mã phiếu mới.
     * @param giaTriStr giá trị giảm mới (string).
     * @param donVi đơn vị mới.
     * @param ngayBatDauStr ngày bắt đầu mới (ISO string).
     * @param ngayKetThucStr ngày hết hạn mới (ISO string).
     * @param soLuongConLaiStr số lượng mới (string, có thể = 0).
     * @param giaTriDonHangToiThieuStr giá trị đơn tối thiểu mới (optional).
     * @param giaTriGiamToiDaStr trần giảm mới (optional).
     * @param loaiGiamGia loại mới.
     * @param session HTTP session.
     * @param request HTTP request.
     * @param model Spring MVC model.
     * @return redirect hoặc tên template form.
     */
    @PostMapping("/phieu-giam-gia/sua/{id}")
    public String processSuaPhieuGiamGia(
            @PathVariable("id") Integer id,
            @RequestParam("maPhieu") String maPhieu,
            @RequestParam("giaTri") String giaTriStr,
            @RequestParam("donVi") String donVi,
            @RequestParam(value = "ngayBatDau", required = false) String ngayBatDauStr,
            @RequestParam("ngayKetThuc") String ngayKetThucStr,
            @RequestParam("soLuongConLai") String soLuongConLaiStr,
            @RequestParam(value = "giaTriDonHangToiThieu", required = false) String giaTriDonHangToiThieuStr,
            @RequestParam(value = "giaTriGiamToiDa", required = false) String giaTriGiamToiDaStr,
            @RequestParam("loaiGiamGia") String loaiGiamGia,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            LocalDateTime start = (ngayBatDauStr == null || ngayBatDauStr.isEmpty()) ? null : LocalDateTime.parse(ngayBatDauStr);
            LocalDateTime end = (ngayKetThucStr == null || ngayKetThucStr.isEmpty()) ? null : LocalDateTime.parse(ngayKetThucStr);

            BigDecimal giaTri = parseAndValidateBigDecimal(giaTriStr, "Giá trị giảm giá", BigDecimal.ONE, PromotionValidationConstants.MAX_VND_VALUE, false);
            // Khi sửa: allowZero = true vì admin có thể muốn set số lượng về 0 để ngừng nhận
            Integer soLuongConLai = parseAndValidateInteger(soLuongConLaiStr, "Số lượng voucher", 0, PromotionValidationConstants.MAX_QUANTITY, true);
            BigDecimal giaTriDonHangToiThieu = parseAndValidateBigDecimal(giaTriDonHangToiThieuStr, "Giá trị đơn hàng tối thiểu", BigDecimal.ZERO, PromotionValidationConstants.MAX_VND_VALUE, true);
            BigDecimal giaTriGiamToiDa = parseAndValidateBigDecimal(giaTriGiamToiDaStr, "Giá trị giảm tối đa", BigDecimal.ONE, PromotionValidationConstants.MAX_VND_VALUE, false);

            adminKhuyenMaiService.updatePhieuGiamGia(id, maPhieu, giaTri, donVi, start, end, soLuongConLai, giaTriDonHangToiThieu, loaiGiamGia, giaTriGiamToiDa, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?suaPhieuThanhCong";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            // Nạp lại entity từ DB rồi ghi đè bằng dữ liệu người dùng vừa nhập để hiển thị form
            PhieuGiamGia pgg = adminKhuyenMaiService.getPhieuGiamGiaById(id);
            pgg.setMaPhieu(maPhieu);
            try {
                pgg.setGiaTri(giaTriStr == null || giaTriStr.isEmpty() ? null : new BigDecimal(giaTriStr));
            } catch (Exception ignored) {
            }
            pgg.setDonVi(donVi);
            try {
                pgg.setNgayBatDau(ngayBatDauStr == null || ngayBatDauStr.isEmpty() ? null : LocalDateTime.parse(ngayBatDauStr));
            } catch (Exception ignored) {
            }
            try {
                pgg.setNgayKetThuc(ngayKetThucStr == null || ngayKetThucStr.isEmpty() ? null : LocalDateTime.parse(ngayKetThucStr));
            } catch (Exception ignored) {
            }
            try {
                pgg.setSoLuongConLai(soLuongConLaiStr == null || soLuongConLaiStr.isEmpty() ? null : Integer.parseInt(soLuongConLaiStr));
            } catch (Exception ignored) {
            }
            try {
                pgg.setGiaTriDonHangToiThieu(giaTriDonHangToiThieuStr == null || giaTriDonHangToiThieuStr.isEmpty() ? null : new BigDecimal(giaTriDonHangToiThieuStr));
            } catch (Exception ignored) {
            }
            try {
                pgg.setGiaTriGiamToiDa(giaTriGiamToiDaStr == null || giaTriGiamToiDaStr.isEmpty() ? null : new BigDecimal(giaTriGiamToiDaStr));
            } catch (Exception ignored) {
            }
            pgg.setLoaiGiamGia(loaiGiamGia);
            model.addAttribute("voucher", pgg);
            return "admin/phieugiamgia-edit";
        }
    }

    /**
     * [POST] Vô hiệu hóa một phiếu giảm giá (đặt active = false).
     *
     * @param id ID phiếu cần vô hiệu hóa.
     * @param session HTTP session.
     * @param request HTTP request.
     * @return redirect URL.
     */
    @PostMapping("/phieu-giam-gia/deactivate/{id}")
    public String processDeactivatePhieuGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deactivatePhieuGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?deactivatePhieuThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * [POST] Xóa logic (soft delete) một phiếu giảm giá.
     *
     * @param id ID phiếu cần xóa.
     * @param session HTTP session.
     * @param request HTTP request.
     * @return redirect URL.
     */
    @PostMapping("/phieu-giam-gia/delete/{id}")
    public String processDeletePhieuGiamGia(@PathVariable("id") Integer id, HttpSession session, HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();
            adminKhuyenMaiService.deletePhieuGiamGia(id, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/khuyen-mai?xoaPhieuThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/khuyen-mai?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ==========================================
    // HELPER – PARSE VÀ VALIDATE SỐ LIỆU
    // ==========================================
    /**
     * Parse chuỗi đầu vào từ form thành {@link Integer} và kiểm tra các ràng
     * buộc.
     *
     * <p>
     * Kiểm tra theo thứ tự:</p>
     * <ol>
     * <li>Không được rỗng.</li>
     * <li>Không được chứa dấu chấm hoặc phẩy (phải là số nguyên).</li>
     * <li>Phải parse được thành Integer hợp lệ.</li>
     * <li>Không được âm.</li>
     * <li>Nếu {@code !allowZero}: không được bằng 0.</li>
     * <li>Phải ≥ {@code min} (nếu min != null).</li>
     * <li>Phải ≤ {@code max} (nếu max != null).</li>
     * </ol>
     *
     * @param valueStr chuỗi cần parse.
     * @param fieldName tên trường hiển thị trong thông báo lỗi.
     * @param min giá trị tối thiểu (null = không giới hạn dưới).
     * @param max giá trị tối đa (null = không giới hạn trên).
     * @param allowZero {@code true} nếu giá trị 0 được chấp nhận.
     * @return giá trị Integer đã parse và validate.
     * @throws PromotionValidationException nếu vi phạm bất kỳ ràng buộc nào.
     */
    private Integer parseAndValidateInteger(String valueStr, String fieldName, Integer min, Integer max, boolean allowZero) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            throw new PromotionValidationException(fieldName + " không được để trống!");
        }
        String trimmed = valueStr.trim();
        // Từ chối số thập phân (ví dụ: "20.5", "10,0")
        if (trimmed.contains(".") || trimmed.contains(",")) {
            throw new PromotionValidationException(fieldName + " phải là số nguyên, không được là số thập phân!");
        }

        Integer val;
        try {
            val = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new PromotionValidationException(fieldName + " phải là số nguyên hợp lệ!");
        }

        if (val < 0) {
            throw new PromotionValidationException(fieldName + " không được là số âm!");
        }
        if (val == 0 && !allowZero) {
            throw new PromotionValidationException(fieldName + " phải lớn hơn 0!");
        }
        if (min != null && val < min) {
            throw new PromotionValidationException(fieldName + " không được nhỏ hơn " + min + "!");
        }
        if (max != null && val > max) {
            throw new PromotionValidationException(fieldName + " không được lớn hơn " + max + "!");
        }
        return val;
    }

    /**
     * Parse chuỗi đầu vào từ form thành {@link BigDecimal} và kiểm tra các ràng
     * buộc.
     *
     * <p>
     * Trả về {@code null} nếu chuỗi rỗng hoặc null (dùng cho trường
     * optional).</p>
     *
     * <p>
     * Kiểm tra theo thứ tự (nếu không rỗng):</p>
     * <ol>
     * <li>Không được chứa dấu chấm hoặc phẩy (phải là số nguyên VNĐ).</li>
     * <li>Phải parse được thành BigDecimal hợp lệ.</li>
     * <li>Không được âm.</li>
     * <li>Nếu {@code !allowZero}: không được bằng 0.</li>
     * <li>Phải ≥ {@code min} (nếu min != null).</li>
     * <li>Phải ≤ {@code max} (nếu max != null).</li>
     * </ol>
     *
     * @param valueStr chuỗi cần parse (rỗng/null → trả về null).
     * @param fieldName tên trường hiển thị trong thông báo lỗi.
     * @param min giá trị tối thiểu (null = không giới hạn dưới).
     * @param max giá trị tối đa (null = không giới hạn trên).
     * @param allowZero {@code true} nếu giá trị 0 được chấp nhận.
     * @return giá trị BigDecimal đã parse và validate, hoặc {@code null} nếu
     * chuỗi rỗng.
     * @throws PromotionValidationException nếu vi phạm bất kỳ ràng buộc nào.
     */
    private BigDecimal parseAndValidateBigDecimal(String valueStr, String fieldName, BigDecimal min, BigDecimal max, boolean allowZero) {
        // Trường optional: chuỗi rỗng → null (service sẽ xử lý null tùy nghiệp vụ)
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = valueStr.trim();
        // Browser có thể gửi BigDecimal nguyên dưới dạng 199999.00/199999,00.
        // Chấp nhận phần thập phân toàn số 0, nhưng vẫn từ chối số lẻ thật sự.
        if (trimmed.contains(".") || trimmed.contains(",")) {
            if (!trimmed.matches("-?\\d+[\\.,]0{1,2}")) {
                throw new PromotionValidationException(fieldName + " phải là số nguyên, không được là số thập phân!");
            }
            trimmed = trimmed.replace(',', '.').split("\\.")[0];
        }

        if (trimmed.contains(".") || trimmed.contains(",")) {
            throw new PromotionValidationException(fieldName + " phải là số nguyên, không được là số thập phân!");
        }

        BigDecimal val;
        try {
            val = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            throw new PromotionValidationException(fieldName + " phải là số hợp lệ!");
        }

        if (val.compareTo(BigDecimal.ZERO) < 0) {
            throw new PromotionValidationException(fieldName + " không được là số âm!");
        }
        if (val.compareTo(BigDecimal.ZERO) == 0 && !allowZero) {
            throw new PromotionValidationException(fieldName + " phải lớn hơn 0!");
        }
        if (min != null && val.compareTo(min) < 0) {
            throw new PromotionValidationException(fieldName + " không được nhỏ hơn " + min.toPlainString() + "!");
        }
        if (max != null && val.compareTo(max) > 0) {
            throw new PromotionValidationException(fieldName + " không được lớn hơn " + max.toPlainString() + "!");
        }
        return val;
    }
}
