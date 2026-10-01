package com.smashvn.shop.controller.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.product.DanhGiaService;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final DanhGiaService danhGiaService;
    private final ProductAvailabilityService productAvailabilityService;

    @GetMapping("/san-pham/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String hienThiChiTietSanPham(@PathVariable("id") Integer id, Model model, HttpSession session) {
        
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm này!"));
        
        if (!productAvailabilityService.isProductPublished(sanPham)) {
            return "redirect:/shop?loi=" + java.net.URLEncoder.encode("Sản phẩm này hiện đã ngưng hiển thị tại cửa hàng!", java.nio.charset.StandardCharsets.UTF_8);
        }
        
        populateProductDetailModel(sanPham, model, session, false);
        return "product-detail";
    }

    public void populateProductDetailModel(SanPham sanPham, Model model, HttpSession session, boolean isPreview) {
        Integer id = sanPham.getId();
        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findActiveBySanPham_Id(id);
        if (isPreview && danhSachChiTiet.isEmpty()) {
            danhSachChiTiet = sanPhamChiTietRepository.findBySanPham_Id(id);
        }
        var customerPrice = com.smashvn.shop.dto.product.CustomerPriceSummary.from(sanPham, danhSachChiTiet);
        // Admin preview may intentionally include only inactive variants; keep their reference price visible.
        if (isPreview && customerPrice.giaBan() == null && !danhSachChiTiet.isEmpty()) {
            var reference = danhSachChiTiet.get(0);
            customerPrice = new com.smashvn.shop.dto.product.CustomerPriceSummary(reference.getGiaBan(),
                    sanPham.getGiaSauGiam(reference.getGiaBan()), sanPham.getActiveGiamGiaPhanTram(),
                    reference.getSoLuongTon() > 0, false);
        }
        model.addAttribute("customerPrice", customerPrice);
        String anhDaiDien = danhSachChiTiet.isEmpty() ? "" : danhSachChiTiet.get(0).getHinhAnhUrl();

        // 1. Lấy danh sách Màu Sắc không trùng lặp
        java.util.Set<String> listMauSac = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMauSac)
                .collect(java.util.stream.Collectors.toSet());
        
        // 2. Lấy danh sách Size (Trọng lượng / Kích thước) không trùng lặp
        java.util.Set<String> listKichThuoc = danhSachChiTiet.stream()
                .map(ct -> ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank() ? ct.getTrongLuong() : ct.getKichThuoc())
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> listMucCang = danhSachChiTiet.stream()
                .map(SanPhamChiTiet::getMucCang)
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        // 3. Tạo một list "gọn nhẹ" (Map) chỉ chứa đúng các thông tin JS cần thiết để tránh lỗi đệ quy
        int phanTram = sanPham.getActiveGiamGiaPhanTram();
        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            Map<String, String> attributes = ct.getSanPhamChiTietThuocTinhs().stream()
                    .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null)
                    .collect(Collectors.toMap(tt -> tt.getThuocTinh().getTenThuocTinh(), tt -> tt.getGiaTri(), (a, b) -> a));
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) attributes.putIfAbsent("Màu sắc", ct.getMauSac().trim());
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) attributes.putIfAbsent("Kích thước", ct.getKichThuoc().trim());
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) attributes.putIfAbsent("Trọng lượng", ct.getTrongLuong().trim());
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) attributes.putIfAbsent("Sức căng", ct.getMucCang().trim());
            map.put("attributes", attributes);
            map.put("phanLoai", ct.getPhanLoaiHienThi());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("kichThuoc", ct.getKichThuoc());
            map.put("mucCang", ct.getMucCang());
            map.put("giaBan", ct.getGiaBan());
            map.put("giaSauGiam", sanPham.getGiaSauGiam(ct.getGiaBan()));
            map.put("phanTramGiam", phanTram);
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhUrl());
            return map;
        }).collect(Collectors.toList());

        Map<String, java.util.Set<String>> allAttributes = new java.util.LinkedHashMap<>();
        Map<String, java.util.Set<String>> dynamicAttributes = new java.util.LinkedHashMap<>();

        for (SanPhamChiTiet ct : danhSachChiTiet) {
            // 1. Thuộc tính từ bảng EAV (SanPhamChiTietThuocTinh)
            if (ct.getSanPhamChiTietThuocTinhs() != null) {
                for (SanPhamChiTietThuocTinh scttt : ct.getSanPhamChiTietThuocTinhs()) {
                    if (scttt.getThuocTinh() != null && scttt.getGiaTri() != null && !scttt.getGiaTri().isBlank()) {
                        String attrName = scttt.getThuocTinh().getTenThuocTinh().trim();
                        String attrVal = scttt.getGiaTri().trim();

                        allAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);

                        if (isSelectableAttribute(sanPham.getDanhMuc(), attrName)) {
                            dynamicAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);
                        }
                    }
                }
            }

            // 2. Cột thuộc tính trực tiếp (mauSac, kichThuoc, trongLuong, mucCang)
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) {
                String val = ct.getMauSac().trim();
                allAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(sanPham.getDanhMuc(), "Màu sắc")) {
                    dynamicAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) {
                String val = ct.getKichThuoc().trim();
                allAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(sanPham.getDanhMuc(), "Kích thước")) {
                    dynamicAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) {
                String val = ct.getTrongLuong().trim();
                allAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(sanPham.getDanhMuc(), "Trọng lượng")) {
                    dynamicAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) {
                String val = ct.getMucCang().trim();
                allAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(val);
                if (isSelectableAttribute(sanPham.getDanhMuc(), "Sức căng")) {
                    dynamicAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(val);
                }
            }
        }

        com.smashvn.shop.constant.CategoryType catType = sanPham.getDanhMuc() != null
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(sanPham.getDanhMuc())
                : com.smashvn.shop.constant.CategoryType.OTHER;
        ensureCategoryRequiredAttributes(catType, sanPham, dynamicAttributes, allAttributes, listBienTheJS);

        boolean inWishlist = false;
        Integer idTaiKhoan = activeMemberAccountId(session);
        if (idTaiKhoan != null) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
            if (kh != null) {
                inWishlist = wishlistRepository.existsById_KhachHangIdAndId_SanPhamId(kh.getId(), id);
            }
        }

        boolean sanPhamKhaDung = isPreview || productAvailabilityService.isProductPublished(sanPham);
        boolean daMuaHang = false;
        boolean daDanhGia = false;
        DanhGia oldDanhGia = null;
        boolean biKhoaBinhLuan = false;
        java.time.LocalDateTime ngayKhoaDen = null;

        if (idTaiKhoan != null) {
            daMuaHang = danhGiaService.daMuaSanPham(idTaiKhoan, id);
            Optional<DanhGia> dgOpt = danhGiaService.layDanhGiaDaCo(idTaiKhoan, id);
            if (dgOpt.isPresent()) {
                daDanhGia = true;
                oldDanhGia = dgOpt.get();
            }

            TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan).orElse(null);
            if (tk != null && tk.getNgayKhoaBinhLuanDen() != null && tk.getNgayKhoaBinhLuanDen().isAfter(java.time.LocalDateTime.now())) {
                biKhoaBinhLuan = true;
                ngayKhoaDen = tk.getNgayKhoaBinhLuanDen();
            }
        }

        List<DanhGia> listDanhGia = danhGiaService.layDanhSachDanhGiaTheoSanPham(id);

        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("listMucCang", listMucCang);
        model.addAttribute("allAttributes", allAttributes);
        model.addAttribute("dynamicAttributes", dynamicAttributes);
        model.addAttribute("sp", sanPham);
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        model.addAttribute("listBienTheJS", listBienTheJS);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("soLuongYeuThich", wishlistRepository.countById_SanPhamId(id));

        model.addAttribute("previewMode", isPreview);
        model.addAttribute("sanPhamKhaDung", sanPhamKhaDung);
        model.addAttribute("daMuaHang", daMuaHang);
        model.addAttribute("daDanhGia", daDanhGia);
        model.addAttribute("oldDanhGia", oldDanhGia);
        model.addAttribute("biKhoaBinhLuan", biKhoaBinhLuan);
        model.addAttribute("ngayKhoaDen", ngayKhoaDen);
        model.addAttribute("listDanhGia", listDanhGia);

        model.addAttribute("totalDanhGia", sanPham.getSoDanhGia());
        model.addAttribute("avgRating", sanPham.getDiemTrungBinh());

        List<SanPham> relatedProducts = sanPhamRepository.findByDanhMucId(sanPham.getDanhMuc().getId()).stream()
                .filter(p -> !p.getId().equals(id) && (p.getTrangThai() == null || "dang_ban".equals(p.getTrangThai())))
                .limit(8)
                .collect(Collectors.toList());
        if (relatedProducts.isEmpty()) {
            relatedProducts = sanPhamRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(id) && (p.getTrangThai() == null || "dang_ban".equals(p.getTrangThai())))
                    .limit(8)
                    .collect(Collectors.toList());
        }
        model.addAttribute("relatedProducts", relatedProducts);
    }

    @PostMapping("/san-pham/{id}/danh-gia")
    public String guiDanhGia(@PathVariable("id") Integer idSanPham,
                             @RequestParam("rating") Double soSao,
                             @RequestParam("comment") String binhLuan,
                             @RequestParam(value = "fileAnh", required = false) List<MultipartFile> files,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer idTaiKhoan = activeMemberAccountId(session);
        if (idTaiKhoan == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng kích hoạt tài khoản thành viên để thực hiện đánh giá sản phẩm.");
            return "redirect:/user/dang-nhap";
        }

        try {
            boolean moderated = danhGiaService.themHoacCapNhatDanhGia(idTaiKhoan, idSanPham, soSao, binhLuan, files);
            redirectAttributes.addFlashAttribute("successMsg", moderated
                    ? "Bình luận của bạn đã được đăng. Một số nội dung không phù hợp đã được hệ thống tự động ẩn."
                    : "Gửi đánh giá sản phẩm thành công!");
        } catch (Exception e) {
            log.error("[REVIEW_SUBMIT_ERROR] Failed to save review: {}", e.getMessage());
            String message = e instanceof IllegalArgumentException || e instanceof IllegalStateException
                    ? e.getMessage()
                    : "Không thể gửi đánh giá lúc này. Vui lòng thử lại sau.";
            redirectAttributes.addFlashAttribute("errorMsg", message);
        }
        return "redirect:/san-pham/" + idSanPham + "#pd-rev";
    }

    @GetMapping("/modal/quick-look/{id}")
    public String layQuickLookModal(@PathVariable("id") Integer id, Model model, HttpSession session) {
        SanPham sanPham = sanPhamRepository.findById(id).orElse(null);
        if (!productAvailabilityService.isProductPublished(sanPham)) {
            return "layout/modals :: quick-look-fragment";
        }

        List<SanPhamChiTiet> danhSachChiTiet = sanPhamChiTietRepository.findActiveBySanPham_Id(id);
        String anhDaiDien = !danhSachChiTiet.isEmpty() ? danhSachChiTiet.get(0).getHinhAnhUrl() : null;

        List<String> listMauSac = danhSachChiTiet.stream().map(SanPhamChiTiet::getMauSac).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> listKichThuoc = danhSachChiTiet.stream().map(SanPhamChiTiet::getKichThuoc).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> listMucCang = danhSachChiTiet.stream().map(SanPhamChiTiet::getMucCang).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());

        List<Map<String, Object>> listBienTheJS = danhSachChiTiet.stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            Map<String, String> attributes = ct.getSanPhamChiTietThuocTinhs().stream()
                    .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null)
                    .collect(Collectors.toMap(tt -> tt.getThuocTinh().getTenThuocTinh(), tt -> tt.getGiaTri(), (a, b) -> a));
            if (ct.getMauSac() != null && !ct.getMauSac().isBlank()) attributes.putIfAbsent("Màu sắc", ct.getMauSac().trim());
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank()) attributes.putIfAbsent("Kích thước", ct.getKichThuoc().trim());
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank()) attributes.putIfAbsent("Trọng lượng", ct.getTrongLuong().trim());
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank()) attributes.putIfAbsent("Sức căng", ct.getMucCang().trim());
            map.put("attributes", attributes);
            map.put("phanLoai", ct.getPhanLoaiHienThi());
            map.put("mauSac", ct.getMauSac());
            map.put("trongLuong", ct.getTrongLuong());
            map.put("kichThuoc", ct.getKichThuoc());
            map.put("mucCang", ct.getMucCang());
            map.put("giaBan", ct.getGiaBan());
            map.put("giaSauGiam", sanPham.getGiaSauGiam(ct.getGiaBan()));
            map.put("soLuongTon", ct.getSoLuongTon());
            map.put("hinhAnhSanPham", ct.getHinhAnhUrl());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, java.util.Set<String>> dynamicAttributes = new java.util.LinkedHashMap<>();
        for (SanPhamChiTiet ct : danhSachChiTiet) {
            if (ct.getSanPhamChiTietThuocTinhs() != null) {
                for (SanPhamChiTietThuocTinh scttt : ct.getSanPhamChiTietThuocTinhs()) {
                    if (scttt.getThuocTinh() != null && scttt.getGiaTri() != null && !scttt.getGiaTri().isBlank()) {
                        String attrName = scttt.getThuocTinh().getTenThuocTinh().trim();
                        if (isSelectableAttribute(sanPham.getDanhMuc(), attrName)) {
                            String attrVal = scttt.getGiaTri().trim();
                            dynamicAttributes.computeIfAbsent(attrName, k -> new java.util.LinkedHashSet<>()).add(attrVal);
                        }
                    }
                }
            }

            if (ct.getMauSac() != null && !ct.getMauSac().isBlank() && isSelectableAttribute(sanPham.getDanhMuc(), "Màu sắc")) {
                dynamicAttributes.computeIfAbsent("Màu sắc", k -> new java.util.LinkedHashSet<>()).add(ct.getMauSac().trim());
            }
            if (ct.getKichThuoc() != null && !ct.getKichThuoc().isBlank() && isSelectableAttribute(sanPham.getDanhMuc(), "Kích thước")) {
                dynamicAttributes.computeIfAbsent("Kích thước", k -> new java.util.LinkedHashSet<>()).add(ct.getKichThuoc().trim());
            }
            if (ct.getTrongLuong() != null && !ct.getTrongLuong().isBlank() && isSelectableAttribute(sanPham.getDanhMuc(), "Trọng lượng")) {
                dynamicAttributes.computeIfAbsent("Trọng lượng", k -> new java.util.LinkedHashSet<>()).add(ct.getTrongLuong().trim());
            }
            if (ct.getMucCang() != null && !ct.getMucCang().isBlank() && isSelectableAttribute(sanPham.getDanhMuc(), "Sức căng")) {
                dynamicAttributes.computeIfAbsent("Sức căng", k -> new java.util.LinkedHashSet<>()).add(ct.getMucCang().trim());
            }
        }

        com.smashvn.shop.constant.CategoryType catTypeQuick = sanPham.getDanhMuc() != null
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(sanPham.getDanhMuc())
                : com.smashvn.shop.constant.CategoryType.OTHER;
        ensureCategoryRequiredAttributes(catTypeQuick, sanPham, dynamicAttributes, new java.util.LinkedHashMap<>(), listBienTheJS);

        boolean inWishlist = false;
        Integer idTaiKhoan = activeMemberAccountId(session);
        if (idTaiKhoan != null) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
            if (kh != null) {
                inWishlist = wishlistRepository.existsById_KhachHangIdAndId_SanPhamId(kh.getId(), id);
            }
        }

        model.addAttribute("spQuickLook", sanPham); 
        model.addAttribute("listChiTiet", danhSachChiTiet);
        model.addAttribute("anhDaiDien", anhDaiDien);
        model.addAttribute("listMauSac", listMauSac);
        model.addAttribute("listKichThuoc", listKichThuoc);
        model.addAttribute("listMucCang", listMucCang);
        model.addAttribute("dynamicAttributes", dynamicAttributes);
        model.addAttribute("listBienTheJS", listBienTheJS);
        model.addAttribute("inWishlist", inWishlist);
        model.addAttribute("soLuongYeuThich", wishlistRepository.countById_SanPhamId(id));
        
        return "layout/modals :: quick-look-fragment"; 
    }

    private Integer activeMemberAccountId(HttpSession session) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return null;
        }
        Object accountId = session.getAttribute("idNguoiDung");
        if (!(accountId instanceof Integer idTaiKhoan)) {
            return null;
        }
        TaiKhoan account = taiKhoanRepository.findById(idTaiKhoan).orElse(null);
        return account != null
                && account.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && "hoat_dong".equalsIgnoreCase(account.getTrangThai())
                        ? idTaiKhoan
                        : null;
    }

    private boolean isSelectableAttribute(DanhMuc dm, String attrName) {
        if (attrName == null || attrName.isBlank()) return false;
        String norm = attrName.toLowerCase().trim();

        com.smashvn.shop.constant.CategoryType catType = (dm != null)
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(dm)
                : com.smashvn.shop.constant.CategoryType.OTHER;

        switch (catType) {
            case VOT:
                // Vợt cầu lông: Cần chọn Màu sắc và Size (Trọng lượng 3U, 4U, 5U...)
                return norm.contains("màu") || norm.contains("color")
                        || norm.contains("trọng lượng") || norm.contains("weight")
                        || norm.equalsIgnoreCase("size") || norm.contains("kích thước");

            case GIAY:
            case TRANG_PHUC:
                // Giày, Áo, Quần: Cần chọn Màu sắc và Size (Kích thước)
                return norm.contains("màu") || norm.contains("color")
                        || norm.equalsIgnoreCase("size") || norm.contains("kích thước");

            case BALO:
                // Balo và Túi cầu lông: Cần chọn Màu sắc
                return norm.contains("màu") || norm.contains("color");

            case CUOC:
            case QUAN_CAN:
            case BANG_QUAN:
            case HOP_CAU:
                // Cước, Quấn cán, Băng quấn, Hộp cầu: KHÔNG CẦN CHỌN THUỘC TÍNH
                return false;

            default:
                return norm.contains("màu") || norm.contains("color")
                        || norm.equalsIgnoreCase("size") || norm.contains("kích thước")
                        || norm.contains("trọng lượng");
        }
    }

    private void ensureCategoryRequiredAttributes(
            com.smashvn.shop.constant.CategoryType catType,
            SanPham sanPham,
            Map<String, java.util.Set<String>> dynamicAttributes,
            Map<String, java.util.Set<String>> allAttributes,
            List<Map<String, Object>> listBienTheJS) {

        // Cước, Quấn cán, Băng quấn, Hộp cầu: KHÔNG CẦN CHỌN THUỘC TÍNH
        if (catType == com.smashvn.shop.constant.CategoryType.CUOC
                || catType == com.smashvn.shop.constant.CategoryType.QUAN_CAN
                || catType == com.smashvn.shop.constant.CategoryType.BANG_QUAN
                || catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            dynamicAttributes.clear();
            return;
        }

        String title = sanPham != null && sanPham.getTenSanPham() != null ? sanPham.getTenSanPham() : "";
        String titleLower = title.toLowerCase();

        String detectedColor = null;
        if (titleLower.contains("đỏ")) detectedColor = "Đỏ";
        else if (titleLower.contains("xanh")) detectedColor = "Xanh";
        else if (titleLower.contains("đen")) detectedColor = "Đen";
        else if (titleLower.contains("trắng")) detectedColor = "Trắng";
        else if (titleLower.contains("vàng")) detectedColor = "Vàng";
        else if (titleLower.contains("cam")) detectedColor = "Cam";
        else if (titleLower.contains("hồng")) detectedColor = "Hồng";
        else if (titleLower.contains("tím")) detectedColor = "Tím";
        else if (titleLower.contains("xám")) detectedColor = "Xám";

        boolean hasColor = dynamicAttributes.keySet().stream().anyMatch(k -> {
            String nk = k.toLowerCase();
            return nk.contains("màu") || nk.contains("color");
        });

        boolean hasSizeOrWeight = dynamicAttributes.keySet().stream().anyMatch(k -> {
            String nk = k.toLowerCase();
            return nk.contains("trọng lượng") || nk.contains("weight") || nk.contains("kích thước") || nk.equals("size");
        });

        switch (catType) {
            case VOT:
                // Vợt cầu lông: Cần Màu sắc và Size (Trọng lượng 4U, 3U)
                if (!hasSizeOrWeight) {
                    dynamicAttributes.put("Trọng lượng", new java.util.LinkedHashSet<>(java.util.List.of("4U", "3U")));
                }
                if (!hasColor) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor != null ? detectedColor : "Mặc định")));
                }
                break;

            case GIAY:
                // Giày cầu lông: Cần Màu sắc và Size (39, 40, 41, 42, 43)
                if (!hasSizeOrWeight) {
                    dynamicAttributes.put("Kích thước", new java.util.LinkedHashSet<>(java.util.List.of("39", "40", "41", "42", "43")));
                }
                if (!hasColor) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor != null ? detectedColor : "Mặc định")));
                }
                break;

            case TRANG_PHUC:
                // Áo / Quần: Cần Màu sắc và Size (S, M, L, XL)
                if (!hasSizeOrWeight) {
                    dynamicAttributes.put("Kích thước", new java.util.LinkedHashSet<>(java.util.List.of("S", "M", "L", "XL")));
                }
                if (!hasColor) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor != null ? detectedColor : "Mặc định")));
                }
                break;

            case BALO:
                // Balo / Túi: Cần Màu sắc
                if (!hasColor) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor != null ? detectedColor : "Mặc định")));
                }
                break;

            default:
                if (!hasColor && !hasSizeOrWeight) {
                    dynamicAttributes.put("Màu sắc", new java.util.LinkedHashSet<>(java.util.List.of(detectedColor != null ? detectedColor : "Mặc định")));
                }
                break;
        }

        if (allAttributes != null) {
            allAttributes.putAll(dynamicAttributes);
        }

        if (listBienTheJS != null) {
            for (Map<String, Object> map : listBienTheJS) {
                @SuppressWarnings("unchecked")
                Map<String, String> attrs = (Map<String, String>) map.get("attributes");
                if (attrs == null) {
                    attrs = new java.util.HashMap<>();
                    map.put("attributes", attrs);
                }
                for (Map.Entry<String, java.util.Set<String>> entry : dynamicAttributes.entrySet()) {
                    String firstVal = entry.getValue().iterator().next();
                    attrs.putIfAbsent(entry.getKey(), firstVal);
                }
            }
        }
    }
}
