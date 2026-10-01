package com.smashvn.shop.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.security.RegisterRateLimiter;
import com.smashvn.shop.service.user.UserDangKyService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDangKyController {

    private final UserDangKyService taiKhoanService;
    private final RegisterRateLimiter registerRateLimiter;

    // Hiển thị form đăng ký (Giao diện)
    @GetMapping("/dang-ky")
    public String hienThiTrangDangKy(@RequestParam(value = "username", required = false) String username, Model model) {
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "signup"; // Sẽ tìm file dang-ky.html trong thư mục templates
    }

    // Xử lý khi người dùng bấm nút Submit form
    @PostMapping("/dang-ky")
    public String xuLyDangKy(@RequestParam("username") String username,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
            HttpServletRequest request,
            Model model) {

        String ip = request.getRemoteAddr();

        // 1. Kiểm tra giới hạn số lần thử (Rate limiting)
        if (registerRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Hành động bị chặn tạm thời do đăng ký quá nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.");
            model.addAttribute("username", username);
            return "signup";
        }

        // Sanitize và Trim inputs
        String sanitizedUsername = sanitizeInput(username);
        String trimmedUsername = (sanitizedUsername != null) ? sanitizedUsername.trim() : "";

        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (matKhau == null || matKhau.isEmpty()) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("passwordError", "Mật khẩu không được để trống!");
            model.addAttribute("username", trimmedUsername);
            return "signup";
        }
        if (!matKhau.equals(xacNhanMatKhau)) {
            registerRateLimiter.registerFailed(ip);
            model.addAttribute("confirmError", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("username", trimmedUsername);
            return "signup";
        }

        try {
            // Gọi Service để lưu
            taiKhoanService.dangKy(trimmedUsername, matKhau);

            // Thành công -> Reset bộ đếm
            registerRateLimiter.registerSucceeded(ip);

            // Nếu thành công, chuyển hướng người dùng sang trang Đăng nhập
            return "redirect:/user/dang-nhap?thanhcong";

        } catch (RuntimeException e) {
            // Thất bại -> Tăng bộ đếm và hiển thị lỗi
            registerRateLimiter.registerFailed(ip);
            addValidationError(model, e.getMessage());
            model.addAttribute("username", trimmedUsername);
            return "signup";
        }
    }

    private void addValidationError(Model model, String message) {
        String safeMessage = (message == null || message.isBlank())
                ? "Không thể tạo tài khoản lúc này. Vui lòng thử lại sau."
                : message;
        String normalizedMessage = safeMessage.toLowerCase(java.util.Locale.ROOT);

        if (normalizedMessage.startsWith("mật khẩu")) {
            model.addAttribute("passwordError", safeMessage);
        } else if (normalizedMessage.contains("email")
                || normalizedMessage.contains("số điện thoại")
                || normalizedMessage.contains("tên miền")) {
            model.addAttribute("usernameError", safeMessage);
        } else {
            model.addAttribute("loi", safeMessage);
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }
}
