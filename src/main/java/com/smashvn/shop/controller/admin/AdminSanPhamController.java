package com.smashvn.shop.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminBienTheService;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.service.inventory.InventoryLotService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/san-pham")
@RequiredArgsConstructor
public class AdminSanPhamController {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final AdminSanPhamService adminSanPhamService;
    private final AdminBienTheService adminBienTheService;
    private final InventoryLotService inventoryLotService;
    private final com.smashvn.shop.controller.product.SanPhamController sanPhamController;

    @GetMapping
    public String hienThiDanhSach(Model model) {
        model.addAttribute("danhSachSanPham", sanPhamRepository.findAllByOrderByIdDesc());
        return "admin/sanpham-list";
    }

    @GetMapping("/them")
    public String hienThiFormThem(Model model) {
        populateFormModel(model);
        return "admin/sanpham-add";
    }

    @PostMapping("/them")
    public String xuLyThemSanPham(
            @org.springframework.web.bind.annotation.ModelAttribute SanPhamCreateRequest requestDto,
            HttpServletRequest request,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.themSanPhamVaBienThe(
                    requestDto,
                    idNguoiDung,
                    request.getRemoteAddr()
            );
            redirectAttributes.addFlashAttribute("success", "Thêm mới sản phẩm '" + requestDto.getTenSanPham() + "' thành công!");
            return "redirect:/admin/san-pham";
        } catch (Exception e) {
            String cleanMsg = e.getMessage();
            if (cleanMsg == null || cleanMsg.isBlank()
                    || cleanMsg.contains("java.lang")
                    || cleanMsg.contains("Unresolved compilation")
                    || cleanMsg.contains("Handler dispatch")
                    || cleanMsg.contains("NullPointerException")
                    || cleanMsg.contains("could not execute statement")) {
                cleanMsg = "Không thể thêm sản phẩm do lỗi xử lý hệ thống. Vui lòng thử lại!";
            }
            redirectAttributes.addFlashAttribute("loi", cleanMsg);
            redirectAttributes.addFlashAttribute("error", cleanMsg);
            redirectAttributes.addFlashAttribute("errorMsg", cleanMsg);
            return "redirect:/admin/san-pham/them";
        }
    }

    private void populateFormModel(Model model) {
        List<com.smashvn.shop.entity.DanhMuc> activeCategories = danhMucRepository.findByTrangThaiTrue();
        model.addAttribute("listDanhMuc", activeCategories);
        model.addAttribute("listThuongHieu", thuongHieuRepository.findByTrangThaiTrue());

        java.util.Map<Integer, String> categoryTypes = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.DanhMuc dm : activeCategories) {
            com.smashvn.shop.constant.CategoryType type = com.smashvn.shop.constant.CategoryType.fromDanhMuc(dm);
            categoryTypes.put(dm.getId(), type.name());
        }
        model.addAttribute("categoryTypes", categoryTypes);

        model.addAttribute("listMauSac", com.smashvn.shop.constant.SanPhamAttributeConfig.DEFAULT_MAU_SAC);
        model.addAttribute("listTrongLuong", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_TRONG_LUONG_VOT);
        model.addAttribute("listKichThuocGiay", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_GIAY);
        model.addAttribute("listKichThuocTrangPhuc", com.smashvn.shop.constant.SanPhamAttributeConfig.WHITELIST_KICH_THUOC_TRANG_PHUC);
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model) {
        SanPham sp = sanPhamRepository.findById(id).orElseThrow();
        model.addAttribute("sp", sp);

        List<com.smashvn.shop.entity.DanhMuc> activeCategories = danhMucRepository.findByTrangThaiTrue();
        if (sp.getDanhMuc() != null && Boolean.FALSE.equals(sp.getDanhMuc().getTrangThai())) {
            if (!activeCategories.contains(sp.getDanhMuc())) {
                activeCategories.add(sp.getDanhMuc());
            }
        }
        model.addAttribute("listDanhMuc", activeCategories);

        List<com.smashvn.shop.entity.ThuongHieu> activeBrands = thuongHieuRepository.findByTrangThaiTrue();
        if (sp.getThuongHieu() != null && Boolean.FALSE.equals(sp.getThuongHieu().getTrangThai())) {
            if (!activeBrands.contains(sp.getThuongHieu())) {
                activeBrands.add(sp.getThuongHieu());
            }
        }
        model.addAttribute("listThuongHieu", activeBrands);

        // Tải 3 Tab dữ liệu (Thông tin SP, Biến thể gom nhóm, Lô hàng)
        model.addAttribute("danhSachBienThe", adminBienTheService.layDanhSachBienThe(id));
        model.addAttribute("groupVariants", inventoryLotService.calculateAggregatedVariants(id));
        model.addAttribute("lotSummaries", inventoryLotService.calculateLotSummaries(id));
        model.addAttribute("lichSuNhapHang", inventoryLotService.getLichSuNhapHang(id));
        model.addAttribute("categoryAttributes", sp.getDanhMuc().getThuocTinhList());

        model.addAttribute("categoryType",
                com.smashvn.shop.constant.CategoryType.fromDanhMuc(sp.getDanhMuc()).name());

        return "admin/sanpham-edit";
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaSanPham(@PathVariable("id") Integer idSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("idDanhMuc") Integer idDanhMuc,
            @RequestParam("idThuongHieu") Integer idThuongHieu,
            @RequestParam("moTa") String moTa,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.capNhatSanPham(idSanPham, tenSanPham, idDanhMuc, idThuongHieu, moTa, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin sản phẩm thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật: " + e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    @PostMapping({"/ngung-hien-thi/{id}", "/xoa/{id}"})
    public String xuLyNgungHienThiSanPham(@PathVariable("id") Integer id, 
                                          @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                          HttpSession session, 
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        String target = (redirectUrl != null && !redirectUrl.isBlank()) ? redirectUrl : "redirect:/admin/san-pham";
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.ngungHienThi(id, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", "Đã ngưng hiển thị sản phẩm thành công!");
            return target.startsWith("redirect:") ? target : "redirect:" + target;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi ngưng hiển thị: " + e.getMessage());
            return target.startsWith("redirect:") ? target : "redirect:" + target;
        }
    }

    @PostMapping({"/dang-ban/{id}", "/mo-ban-lai/{id}"})
    public String xuLyDangBanSanPham(@PathVariable("id") Integer id, 
                                     @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                     HttpSession session, 
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String target = (redirectUrl != null && !redirectUrl.isBlank()) ? redirectUrl : "redirect:/admin/san-pham";
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminSanPhamService.dangBan(id, idNguoiDung, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", "Đã đăng bán sản phẩm thành công!");
            return target.startsWith("redirect:") ? target : "redirect:" + target;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return target.startsWith("redirect:") ? target : "redirect:" + target;
        }
    }

    @GetMapping("/xem-truoc/{id}")
    public String xemTruocSanPham(@PathVariable("id") Integer id, Model model, HttpSession session) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm này!"));
        sanPhamController.populateProductDetailModel(sp, model, session, true);
        return "product-detail";
    }

    // ─── API: lịch sử nhập hàng theo biến thể ───────────────────────────────
    @GetMapping("/bien-the/{idSpct}/lich-su-nhap")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getLichSuNhapBySpct(
            @PathVariable("idSpct") Integer idSpct) {
        try {
            var summary = inventoryLotService.getSummaryBySpct(idSpct);
            var history = inventoryLotService.getLichSuPhieuNhapBySpct(idSpct);
            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of("summary", summary, "history", history));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ─── API: chi tiết phiếu nhập ────────────────────────────────────────────
    @GetMapping("/phieu-nhap/{idPhieuNhap}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getChiTietPhieuNhap(
            @PathVariable("idPhieuNhap") Integer idPhieuNhap) {
        try {
            var dto = inventoryLotService.getPhieuNhapDetail(idPhieuNhap);
            return org.springframework.http.ResponseEntity.ok(dto);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
