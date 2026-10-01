package com.smashvn.shop.controller.user;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.smashvn.shop.service.user.UserQuenMatKhauService;
import com.smashvn.shop.security.ForgotPasswordRateLimiter;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserQuenMatKhauController {

    private final UserQuenMatKhauService quenMatKhauService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String configuredBaseUrl;

    private String resolveBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty()) {
            return configuredBaseUrl.trim().replaceAll("/+$", "");
        }
        try {
            return org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromContextPath(request)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            if ((scheme.equalsIgnoreCase("http") && serverPort == 80) || (scheme.equalsIgnoreCase("https") && serverPort == 443)) {
                return scheme + "://" + serverName + contextPath;
            }
            return scheme + "://" + serverName + ":" + serverPort + contextPath;
        }
    }

    // 1. Hiển thị trang nhập Email
    @GetMapping("/quen-mat-khau")
    public String hienThiTrangQuenMK(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email != null && !email.trim().isEmpty() && model != null) {
            model.addAttribute("email", email.trim());
        }
        return "lost-password"; 
    }

    public String hienThiTrangQuenMK() {
        return hienThiTrangQuenMK(null, null);
    }

    // 1.5 API AJAX Quên mật khẩu dùng cho Modal Checkout
    @PostMapping("/checkout/api/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forgotPasswordAjax(@RequestParam("email") String email, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String ip = request.getRemoteAddr();

        if (forgotPasswordRateLimiter.isBlocked(ip)) {
            response.put("success", false);
            response.put("message", "Hành động bị chặn tạm thời do yêu cầu quá nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.");
            return ResponseEntity.ok(response);
        }

        String sanitizedEmail = sanitizeInput(email);
        String trimmedEmail = (sanitizedEmail != null) ? sanitizedEmail.trim() : "";

        if (trimmedEmail.isEmpty()) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            response.put("success", false);
            response.put("message", "Email không được để trống!");
            return ResponseEntity.ok(response);
        }
        if (trimmedEmail.length() > 100) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            response.put("success", false);
            response.put("message", "Email không được vượt quá 100 ký tự!");
            return ResponseEntity.ok(response);
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            response.put("success", false);
            response.put("message", "Định dạng email không hợp lệ!");
            return ResponseEntity.ok(response);
        }

        try {
            String appUrl = resolveBaseUrl(request);
            quenMatKhauService.guiYeuCauKhoiPhuc(trimmedEmail, appUrl);
            forgotPasswordRateLimiter.forgotPasswordSucceeded(ip);
            response.put("success", true);
            response.put("message", "Đã gửi link khôi phục mật khẩu tới email " + trimmedEmail + ". Vui lòng kiểm tra hộp thư!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // 2. Xử lý Gửi Email
    @PostMapping("/quen-mat-khau")
    public String xuLyQuenMK(@RequestParam("email") String email, HttpServletRequest request, Model model) {
        String ip = request.getRemoteAddr();

        // Kiểm tra Rate Limiting
        if (forgotPasswordRateLimiter.isBlocked(ip)) {
            model.addAttribute("loi", "Hành động bị chặn tạm thời do yêu cầu quá nhiều lần liên tiếp. Vui lòng thử lại sau 15 phút.");
            return "lost-password";
        }

        // Sanitize và Trim email
        String sanitizedEmail = sanitizeInput(email);
        String trimmedEmail = (sanitizedEmail != null) ? sanitizedEmail.trim() : "";

        // Validate email format and length
        if (trimmedEmail.isEmpty()) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Email không được để trống!");
            return "lost-password";
        }
        if (trimmedEmail.length() > 100) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Email không được vượt quá 100 ký tự!");
            return "lost-password";
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", "Định dạng email không hợp lệ!");
            return "lost-password";
        }

        try {
            String appUrl = resolveBaseUrl(request);
            quenMatKhauService.guiYeuCauKhoiPhuc(trimmedEmail, appUrl);
            forgotPasswordRateLimiter.forgotPasswordSucceeded(ip);
            model.addAttribute("thongBao", "Đường link khôi phục mật khẩu đã được gửi vào Email của bạn. Vui lòng kiểm tra hộp thư!");
        } catch (RuntimeException e) {
            forgotPasswordRateLimiter.forgotPasswordFailed(ip);
            model.addAttribute("loi", e.getMessage());
        }
        return "lost-password";
    }

    // 3. Khách click vào Link trong Email -> Hiển thị form nhập Pass mới
    @GetMapping("/dat-lai-mat-khau")
    public String hienThiTrangDatLaiMK(@RequestParam("token") String token, Model model) {
        try {
            quenMatKhauService.kiemTraToken(token); // Nếu lỗi sẽ văng ra Exception
            model.addAttribute("token", token);
            return "reset-password"; 
        } catch (RuntimeException e) {
            model.addAttribute("loi", e.getMessage());
            return "lost-password"; // Lỗi thì đá về lại trang Quên MK
        }
    }

    // 4. Khách bấm lưu Pass mới
    @PostMapping("/dat-lai-mat-khau")
    public String xuLyDatLaiMK(@RequestParam("token") String token, 
                               @RequestParam("matKhauMoi") String matKhauMoi, 
                               @RequestParam("xacNhanMatKhau") String xacNhanMatKhau, 
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                               Model model) {
        
        // Validate password strength: 8-30 chars, contains both letter and digit, no spaces
        if (matKhauMoi == null || matKhauMoi.isEmpty()) {
            model.addAttribute("loi", "Mật khẩu mới không được để trống!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (matKhauMoi.length() < 8 || matKhauMoi.length() > 30) {
            model.addAttribute("loi", "Mật khẩu phải dài từ 8 đến 30 ký tự!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (matKhauMoi.contains(" ") || matKhauMoi.contains("\t") || matKhauMoi.contains("\n") || matKhauMoi.contains("\r")) {
            model.addAttribute("loi", "Mật khẩu không được chứa khoảng trắng!");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (!matKhauMoi.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            model.addAttribute("loi", "Mật khẩu phải chứa cả chữ và số!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Kiểm tra 2 mật khẩu có khớp nhau không
        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            quenMatKhauService.datLaiMatKhau(token, matKhauMoi);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Khôi phục mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/user/dang-nhap";
        } catch (RuntimeException e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "");
    }
}