package com.smashvn.shop.controller.product;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.product.SanPhamYeuThichService;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class SanPhamYeuThichController {

    private final SanPhamYeuThichService yeuThichService;
    private final SanPhamYeuThichRepository yeuThichRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    // 1. Hiển thị trang Wishlist
    @GetMapping
    public String hienThiWishlist(HttpSession session, Model model) {
        if (!isActiveMember(session)) return "redirect:/user/dang-nhap";
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        model.addAttribute("listWishlist", yeuThichService.layDanhSachWishlist(idNguoiDung));
        return "wishlist"; 
    }

    // 2. API Thêm vào Wishlist (Dùng cho AJAX ở các nút trái tim)
    @PostMapping("/them")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> themVaoWishlist(@RequestParam("idSanPham") Integer idSanPham, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isActiveMember(session)) {
            result.put("status", "chuadangnhap");
            return ResponseEntity.status(401).body(result);
        }
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        try {
            String kq = yeuThichService.themVaoWishlist(idNguoiDung, idSanPham);
            long count = yeuThichRepository.countById_SanPhamId(idSanPham);
            result.put("status", kq);
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "loi");
            result.put("message", "Không thể thực hiện thao tác yêu thích. Vui lòng thử lại!");
            return ResponseEntity.ok(result);
        }
    }

    // 3. Xóa 1 sản phẩm
    @GetMapping("/xoa/{id}")
    public String xoaSanPham(@PathVariable("id") Integer idSanPham, HttpSession session) {
        if (isActiveMember(session)) {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            yeuThichService.xoaSanPham(idNguoiDung, idSanPham);
        }
        return "redirect:/wishlist";
    }

    // 4. Xóa sạch
    @GetMapping("/xoa-tat-ca")
    public String xoaTatCa(HttpSession session) {
        if (isActiveMember(session)) {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            yeuThichService.xoaTatCa(idNguoiDung);
        }
        return "redirect:/wishlist";
    }
 // API Xóa sản phẩm yêu thích bằng AJAX (Không reload)
    @GetMapping("/api/xoa/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaSanPhamAjax(@PathVariable("id") Integer idSanPham, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        if (!isActiveMember(session)) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.status(401).body(response);
        }
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        
        try {
            yeuThichService.xoaSanPham(idNguoiDung, idSanPham);
            response.put("trangThai", "ok");
        } catch (Exception e) {
            response.put("trangThai", "loi");
        }
        return ResponseEntity.ok(response);
    }

    private boolean isActiveMember(HttpSession session) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return false;
        }
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return false;
        }
        com.smashvn.shop.entity.TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return tk != null
                && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }
}
