package com.smashvn.shop.controller.admin;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.smashvn.shop.security.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDangNhapController {

    private final UserDangNhapService userDangNhapService;
    private final NhanVienRepository nhanVienRepository;
    private final LoginRateLimiter loginRateLimiter;

    // Hiển thị form đăng nhập Admin
    @GetMapping("/dang-nhap")
    public String hienThiFormDangNhapAdmin(@RequestParam(value = "loi", required = false) String loi, Model model, HttpSession session) {
        // Nếu đã đăng nhập quyền quản trị rồi thì redirect thẳng vào dashboard
        if (session != null && session.getAttribute("idNguoiDung") != null) {
            String vaiTro = (String) session.getAttribute("vaiTro");
            if ("QL".equals(vaiTro)) {
                return "redirect:/admin/all";
            } else if ("NV".equals(vaiTro)) {
                return "redirect:/admin/don-hang";
            }
        }
        if (loi != null) {
            model.addAttribute("loi", loi);
        }
        return "admin/signin";
    }

    // Xử lý khi bấm nút "Đăng nhập" Admin
    @PostMapping("/dang-nhap")
    public String xuLyDangNhapAdmin(@RequestParam("username") String username,
            @RequestParam("matKhau") String matKhau,
            HttpServletRequest request,
            HttpSession session,
            Model model, HttpServletResponse httpResponse) {
        String ip = request.getRemoteAddr();

        // 1. Kiểm tra giới hạn số lần thử (Rate limiting)
        if (loginRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Tài khoản tạm thời bị khóa đăng nhập do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
            return "admin/signin";
        }

        try {
            TaiKhoan tkDangNhap = userDangNhapService.kiemTraDangNhap(username, matKhau);

            // 2. Kiểm tra vai trò: Chỉ cho phép QL và NV
            String userRole = tkDangNhap.getVaiTro();
            if (!"NV".equals(userRole) && !"QL".equals(userRole)) {
                loginRateLimiter.loginFailed(ip);
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_ADMIN_LOGIN_ATTEMPT: Username: {}, IP: {}", username, ip);
                model.addAttribute("loi", "Tài khoản không có quyền truy cập trang quản trị!");
                return "admin/signin";
            }

            // Đăng nhập thành công -> Reset bộ đếm rate limiter
            loginRateLimiter.loginSucceeded(ip);

            // 3. Chống Session Fixation
            LoginSession.establish(request, httpResponse, tkDangNhap);
            session = request.getSession(true);

            // Default active role for admin login
            String vaiTro = "QL".equals(userRole) ? "QL" : "NV";

            // Lưu thông tin vào Session
            session.setAttribute("nguoiDungDangNhap", tkDangNhap.getUsername());
            session.setAttribute("idNguoiDung", tkDangNhap.getId());
            session.setAttribute("vaiTro", tkDangNhap.getVaiTro());
            session.setAttribute("activeRole", vaiTro);

            // Tìm tên hiển thị của nhân viên
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(tkDangNhap.getId());
            if (nv != null) {
                session.setAttribute("tenHienThi", nv.getHoTenNv());
            } else {
                session.setAttribute("tenHienThi", "QL".equals(vaiTro) ? "Quản lý hệ thống" : "Nhân viên hệ thống");
            }


            log.info("[SECURITY_EVENT] ADMIN_LOGIN_SUCCESS: Username: {}, VaiTro: {}, IP: {}", username, vaiTro, ip);

            // Đăng nhập thành công, chuyển hướng theo vai trò
            if ("QL".equals(vaiTro)) {
                return "redirect:/admin/all";
            } else {
                return "redirect:/admin/don-hang";
            }

        } catch (RuntimeException e) {
            loginRateLimiter.loginFailed(ip);
            log.warn("[SECURITY_EVENT] FAILED_ADMIN_LOGIN: Username: {}, IP: {}, Lỗi: {}", username, ip, e.getMessage());

            model.addAttribute("loi", e.getMessage());
            return "admin/signin";
        }
    }

    // Đăng xuất Admin
    @GetMapping("/dang-xuat")
    public String xuLyDangXuatAdmin(HttpSession session) {
        if (session != null) {
            session.invalidate(); // Xóa toàn bộ dữ liệu trong Session
        }
        SecurityContextHolder.clearContext(); // Xóa context của Spring Security
        return "redirect:/admin/dang-nhap";
    }
}
