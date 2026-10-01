package com.smashvn.shop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.product.DanhMucService;
import com.smashvn.shop.service.product.ThuocTinhService;
import com.smashvn.shop.service.product.ThuongHieuService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Controller
@RequestMapping("/admin/danh-muc")
@RequiredArgsConstructor
@Slf4j
public class AdminDanhMucController {

    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final SanPhamRepository sanPhamRepository;
    private final DanhMucService danhMucService;
    private final ThuongHieuService thuongHieuService;
    private final ThuocTinhService thuocTinhService;

    // ----------------------------------------------------------------
    // GET: list page
    // ----------------------------------------------------------------

    @GetMapping
    public String hienThiTrangQuanLy(Model model) {
        model.addAttribute("listDanhMuc", danhMucRepository.findAll());
        model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
        model.addAttribute("listThuocTinh", thuocTinhService.getAllThuocTinh());
        return "admin/danhmuc-list";
    }

    // ----------------------------------------------------------------
    // CATEGORY endpoints
    // ----------------------------------------------------------------

    @PostMapping("/them")
    public String themDanhMuc(
            @RequestParam(value = "tenDanhMuc", required = false) String tenDanhMuc,
            @RequestParam(value = "thuocTinhIds", required = false) List<Integer> thuocTinhIds,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            danhMucService.themDanhMuc(tenDanhMuc, thuocTinhIds);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục mới thành công!");
            return "redirect:/admin/danh-muc";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loiDanhMuc", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            model.addAttribute("listThuocTinh", thuocTinhService.getAllThuocTinh());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/sua/{id}")
    public String suaDanhMuc(
            @PathVariable("id") Integer id,
            @RequestParam(value = "tenDanhMuc", required = false) String tenDanhMuc,
            @RequestParam(value = "thuocTinhIds", required = false) List<Integer> thuocTinhIds,
            @RequestParam(value = "capNhatThuocTinh", defaultValue = "false") boolean capNhatThuocTinh,
            RedirectAttributes redirectAttributes) {
        try {
            danhMucService.suaDanhMuc(id, tenDanhMuc, thuocTinhIds, capNhatThuocTinh);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("[ADMIN] Lỗi khi cập nhật danh mục id={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi hệ thống khi cập nhật danh mục. Vui lòng thử lại!");
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping({"/an/{id}", "/xoa/{id}"})
    public String anHoacHienDanhMuc(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            com.smashvn.shop.entity.DanhMuc dm = danhMucService.anHoacHienDanhMuc(id);
            if (Boolean.TRUE.equals(dm.getTrangThai())) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã hiển thị danh mục trên giao diện người dùng!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn danh mục khỏi giao diện người dùng thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    // ----------------------------------------------------------------
    // BRAND (ThuongHieu) endpoints
    // ----------------------------------------------------------------

    @PostMapping("/thuong-hieu/them")
    public String themThuongHieu(
            @RequestParam(value = "tenThuongHieu", required = false) String tenThuongHieu,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            thuongHieuService.themThuongHieu(tenThuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thương hiệu mới thành công!");
            return "redirect:/admin/danh-muc";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loiThuongHieu", e.getMessage());
            model.addAttribute("listDanhMuc", danhMucRepository.findAll());
            model.addAttribute("listThuongHieu", thuongHieuRepository.findAll());
            model.addAttribute("listThuocTinh", thuocTinhService.getAllThuocTinh());
            return "admin/danhmuc-list";
        }
    }

    @PostMapping("/thuong-hieu/sua/{id}")
    public String suaThuongHieu(
            @PathVariable("id") Integer id,
            @RequestParam(value = "tenThuongHieu", required = false) String tenThuongHieu,
            RedirectAttributes redirectAttributes) {
        try {
            thuongHieuService.suaThuongHieu(id, tenThuongHieu);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping({"/thuong-hieu/an/{id}", "/thuong-hieu/xoa/{id}"})
    public String anHoacHienThuongHieu(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            com.smashvn.shop.entity.ThuongHieu th = thuongHieuService.anHoacHienThuongHieu(id);
            if (Boolean.TRUE.equals(th.getTrangThai())) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã hiển thị thương hiệu trên giao diện người dùng!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn thương hiệu khỏi giao diện người dùng thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }
}
