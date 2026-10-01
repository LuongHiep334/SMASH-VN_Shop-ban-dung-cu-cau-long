package com.smashvn.shop.controller.admin;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.CommentViolationLog;
import com.smashvn.shop.service.product.DanhGiaService;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.CommentViolationLogRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminDanhGiaController {

    private final DanhGiaService danhGiaService;
    private final CommentViolationLogRepository commentViolationLogRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;

    // Xem danh sách đánh giá của hệ thống
    @GetMapping("/admin/danh-gia")
    public String hienThiDanhSachDanhGia(Model model, HttpSession session) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        List<DanhGia> listDanhGia = danhGiaService.layTatCaDanhGia();
        model.addAttribute("listDanhGia", listDanhGia);
        return "admin/danhgia-list";
    }

    // Ẩn bình luận (văn bản)
    @PostMapping("/admin/danh-gia/an-binh-luan/{id}")
    public String anBinhLuan(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.anBinhLuan(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã ẩn nội dung bình luận thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Hiển thị lại bình luận
    @PostMapping("/admin/danh-gia/hien-binh-luan/{id}")
    public String hienBinhLuan(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.hienBinhLuan(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã phục hồi hiển thị bình luận thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Ẩn hình ảnh
    @PostMapping("/admin/danh-gia/an-hinh-anh/{id}")
    public String anHinhAnh(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.anHinhAnh(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã ẩn hình ảnh đánh giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Hiển thị lại hình ảnh
    @PostMapping("/admin/danh-gia/hien-hinh-anh/{id}")
    public String hienHinhAnh(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.hienHinhAnh(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã phục hồi hiển thị hình ảnh thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Xóa mềm đánh giá
    @PostMapping("/admin/danh-gia/xoa/{id}")
    public String xoaMemDanhGia(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            danhGiaService.xoaMemDanhGia(id, adminId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa mềm đánh giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    // Xem danh sách vi phạm bình luận
    @GetMapping("/admin/danh-gia/vi-pham")
    public String hienThiDanhSachViPham(Model model, HttpSession session) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        List<CommentViolationLog> listViPham = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        if (listViPham == null) {
            listViPham = Collections.emptyList();
        }

        model.addAttribute("listViPham", listViPham);
        return "admin/vipham-list"; // Trỏ đến vipham-list.html
    }

    // Gỡ khóa bình luận thủ công
    @PostMapping("/admin/danh-gia/vi-pham/go-khoa/{taiKhoanId}")
    public String goKhoaBinhLuan(@PathVariable("taiKhoanId") Integer taiKhoanId, 
                                 jakarta.servlet.http.HttpServletRequest request,
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        String vaiTro = (String) session.getAttribute("vaiTro");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
            String oldVal = tk.getNgayKhoaBinhLuanDen() != null ? tk.getNgayKhoaBinhLuanDen().toString() : "null";
            tk.setNgayKhoaBinhLuanDen(null);
            taiKhoanRepository.save(tk);

            // Ghi log audit
            auditService.log(adminId, "TaiKhoan", tk.getId().longValue(), "UPDATE", 
                    oldVal, "null", request.getRemoteAddr(), "Gỡ khóa bình luận thủ công bởi admin.", vaiTro);

            redirectAttributes.addFlashAttribute("successMsg", "Đã gỡ khóa bình luận thành công cho tài khoản " + tk.getUsername());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia/vi-pham";
    }

    // Reset bộ đếm vi phạm thủ công
    @PostMapping("/admin/danh-gia/vi-pham/reset-vi-pham/{taiKhoanId}")
    public String resetViPhamBinhLuan(@PathVariable("taiKhoanId") Integer taiKhoanId, 
                                      jakarta.servlet.http.HttpServletRequest request,
                                      HttpSession session, 
                                      RedirectAttributes redirectAttributes) {
        Integer adminId = (Integer) session.getAttribute("idNguoiDung");
        String vaiTro = (String) session.getAttribute("vaiTro");
        if (adminId == null) {
            return "redirect:/admin/dang-nhap";
        }

        try {
            TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
            int soLanViPhamHienTai = tk.getSoLanNhacNhoViPham() != null ? tk.getSoLanNhacNhoViPham() : 0;
            String oldVal = String.valueOf(soLanViPhamHienTai);
            tk.setSoLanNhacNhoViPham(0);
            tk.setNgayViPhamGanNhat(null);
            taiKhoanRepository.save(tk);

            // Ghi log audit
            auditService.log(adminId, "TaiKhoan", tk.getId().longValue(), "UPDATE", 
                    oldVal, "0", request.getRemoteAddr(), "Reset bộ đếm vi phạm thủ công bởi admin.", vaiTro);

            redirectAttributes.addFlashAttribute("successMsg", "Đã reset bộ đếm vi phạm thành công cho tài khoản " + tk.getUsername());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia/vi-pham";
    }
}
