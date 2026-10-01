package com.smashvn.shop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.admin.AdminBienTheService;

import com.smashvn.shop.repository.SanPhamChiTietRepository;

@Controller
@RequestMapping("/admin/san-pham/{idSanPham}/bien-the") // Link động chứa ID sản phẩm
@RequiredArgsConstructor
public class AdminBienTheController {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final AdminBienTheService adminBienTheService;
    private final com.smashvn.shop.service.inventory.InventoryLotService inventoryLotService;


    // 1. Hiển thị Trang Quản lý Biến thể (Gồm cả Form Thêm + Bảng Danh sách)
    @GetMapping
    public String hienThiTrangBienThe(@PathVariable("idSanPham") Integer idSanPham, Model model) {
        SanPham sp = sanPhamRepository.findById(idSanPham).orElseThrow();
        model.addAttribute("sp", sp);
        model.addAttribute("danhSachBienThe", adminBienTheService.layDanhSachBienThe(idSanPham));
        model.addAttribute("categoryType",
                com.smashvn.shop.constant.CategoryType.fromDanhMuc(sp.getDanhMuc()).name());
        return "admin/bienthe-list";
    }

    // 2. Hứng dữ liệu khi Admin bấm "LƯU BIẾN THỂ MỚI"
    @PostMapping("/them")
    public String xuLyThemBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                  @RequestParam(value = "giaBan", required = false) BigDecimal giaBan,
                                  @RequestParam(value = "giaNhap", required = false) BigDecimal giaNhap,
                                  @RequestParam(value = "soLuongTon", required = false) Integer soLuongTon,
                                  @RequestParam(value = "mauSac", required = false) String mauSac,
                                  @RequestParam(value = "trongLuong", required = false) String trongLuong,
                                  @RequestParam(value = "kichThuoc", required = false) String kichThuoc,
                                  @RequestParam(value = "mucCang", required = false) String mucCang,
                                  @RequestParam(value = "fileAnh", required = false) MultipartFile fileAnh,
                                  jakarta.servlet.http.HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            adminBienTheService.themBienThe(idSanPham, giaBan, giaNhap, soLuongTon, mauSac, trongLuong, kichThuoc, mucCang, fileAnh, idNguoiDung);
            redirectAttributes.addFlashAttribute("success", "Thêm biến thể mới thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi thêm biến thể!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    // 3. Ẩn một biến thể khỏi khách hàng (xóa mềm)
    @GetMapping("/xoa/{idBienThe}")
    public String xuLyXoaBienThe(@PathVariable("idSanPham") Integer idSanPham, 
                                 @PathVariable("idBienThe") Integer idBienThe,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.xoaBienThe(idBienThe);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn biến thể khỏi khách hàng thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể ẩn biến thể này.");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    @GetMapping("/mo-ban-lai/{idBienThe}")
    public String xuLyMoBanLaiBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                      @PathVariable("idBienThe") Integer idBienThe,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminBienTheService.moBanLaiBienThe(idBienThe);
            redirectAttributes.addFlashAttribute("success", "Đã mở bán lại biến thể thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể mở bán lại biến thể này.");
        }
        return "redirect:/admin/san-pham/sua/" + idSanPham;
    }
    
    // 5. Xử lý Cập nhật Biến Thể
    @PostMapping("/sua/{idBienThe}")
    public String xuLySuaBienThe(@PathVariable("idSanPham") Integer idSanPham,
                                 @PathVariable("idBienThe") Integer idBienThe,
                                 @RequestParam(value = "giaBan", required = false) BigDecimal giaBan,
                                 @RequestParam(value = "soLuongTon", required = false) Integer soLuongTon,
                                 @RequestParam(value = "mauSac", required = false) String mauSac,
                                 @RequestParam(value = "trongLuong", required = false) String trongLuong,
                                 @RequestParam(value = "kichThuoc", required = false) String kichThuoc,
                                 @RequestParam(value = "mucCang", required = false) String mucCang,
                                 @RequestParam(value = "fileAnh", required = false) MultipartFile fileAnh,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (idBienThe != null && idBienThe == 0) {
                // Cập nhật đồng loạt giá bán và số lượng cho tất cả biến thể của sản phẩm
                List<com.smashvn.shop.entity.SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
                if (variants.isEmpty()) {
                    throw new IllegalArgumentException("Sản phẩm chưa có biến thể nào để cập nhật.");
                }
                boolean updated = false;
                for (com.smashvn.shop.entity.SanPhamChiTiet bt : variants) {
                    if (giaBan != null && giaBan.compareTo(BigDecimal.ZERO) > 0) {
                        bt.setGiaBan(giaBan);
                        updated = true;
                    }
                    sanPhamChiTietRepository.save(bt);
                }
                if (updated) {
                    redirectAttributes.addFlashAttribute("success", "Cập nhật đồng loạt tất cả biến thể thành công!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng nhập giá bán hợp lệ (> 0) để cập nhật đồng loạt.");
                }

                return "redirect:/admin/san-pham/sua/" + idSanPham;
            }

            adminBienTheService.capNhatBienThe(idBienThe, giaBan, soLuongTon, mauSac, trongLuong, kichThuoc, mucCang, fileAnh);
            redirectAttributes.addFlashAttribute("success", "Cập nhật biến thể thành công!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi cập nhật biến thể!");
            return "redirect:/admin/san-pham/sua/" + idSanPham;
        }
    }

    // 6. API Nhập Hàng Cho Biến Thể Đã Tồn Tại
    @PostMapping("/nhap-lo")
    public String xuLyNhapLoMoi(@PathVariable("idSanPham") Integer idSanPham,
                                @RequestParam("representativeSpctId") Integer representativeSpctId,
                                @RequestParam("soLuongNhap") Integer soLuongNhap,
                                @RequestParam("giaNhap") BigDecimal giaNhap,
                                @RequestParam(value = "ghiChu", required = false) String ghiChu,
                                jakarta.servlet.http.HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            inventoryLotService.nhapLoMoi(representativeSpctId, soLuongNhap, giaNhap, idNguoiDung, ghiChu);
            redirectAttributes.addFlashAttribute("success", "Nhập hàng thành công và đã cập nhật giá vốn bình quân!");
            return "redirect:/admin/san-pham/sua/" + idSanPham + "?tab=lo";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nhập hàng: " + e.getMessage());
            return "redirect:/admin/san-pham/sua/" + idSanPham + "?tab=lo";
        }
    }

    // 7. API Lấy Chi Tiết Phếu Nhập Dùng Cho Modal Admin
    @org.springframework.web.bind.annotation.GetMapping("/phieu-nhap/{idPhieuNhap}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getChiTietPhieuNhap(@PathVariable("idPhieuNhap") Integer idPhieuNhap) {
        try {
            com.smashvn.shop.dto.inventory.PhieuNhapDetailDTO dto = inventoryLotService.getPhieuNhapDetail(idPhieuNhap);
            return org.springframework.http.ResponseEntity.ok(dto);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // 8. API Lấy Lịch Sử Nhập Hàng & Summary Cho 1 Biến Thể Cụ Thể
    @org.springframework.web.bind.annotation.GetMapping("/bien-the/{idSpct}/lich-su-nhap")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getLichSuNhapBySpct(@PathVariable("idSpct") Integer idSpct) {
        try {
            com.smashvn.shop.dto.inventory.BienTheImportSummaryDTO summary = inventoryLotService.getSummaryBySpct(idSpct);
            java.util.List<com.smashvn.shop.dto.inventory.PhieuNhapChiTietDTO> history = inventoryLotService.getLichSuPhieuNhapBySpct(idSpct);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("summary", summary, "history", history));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

}
