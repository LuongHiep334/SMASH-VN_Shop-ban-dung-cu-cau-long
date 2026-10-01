package com.smashvn.shop.controller.user;

import java.util.Locale;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.exception.AccountLockedException;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.exception.InvalidPasswordException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;
import com.smashvn.shop.util.LoginIdentifierClassifier;
import com.smashvn.shop.util.LoginIdentifierClassifier.LoginIdentifierType;
import com.smashvn.shop.util.LoginIdentifierClassifier.NormalizedLoginIdentifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.smashvn.shop.security.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserDangNhapController {

    private final UserDangNhapService userDangNhapService;
    private final KhachHangRepository khachHangRepository;
    private final LoginRateLimiter loginRateLimiter;

    // Hiển thị form đăng nhập
    @GetMapping("/dang-nhap")
    public String hienThiFormFormDangNhap(
            @RequestParam(value = "loi", required = false) String loi,
            @RequestParam(value = "thanhcong", required = false) String thanhcong,
            Model model) {
        if (loi != null) {
            model.addAttribute("loi", loi);
        }
        if (thanhcong != null) {
            model.addAttribute("thanhcong", thanhcong);
        }
        return "signin";
    }

    // Xử lý khi bấm nút "Đăng nhập"
    @PostMapping("/dang-nhap")
    public String xuLyDangNhap(@RequestParam("username") String username,
            @RequestParam("matKhau") String matKhau,
            HttpServletRequest request,
            HttpSession session,
            Model model, HttpServletResponse httpResponse) {
        String ip = request.getRemoteAddr();

        // Sanitize và Trim inputs
        String sanitizedUsername = sanitizeInput(username);
        String trimmedUsername = (sanitizedUsername != null) ? sanitizedUsername.trim() : "";
        model.addAttribute("usernameNhap", trimmedUsername);

        // Kiểm tra từng trường để người dùng biết chính xác nội dung cần sửa.
        boolean invalidInput = false;
        NormalizedLoginIdentifier normalizedIdentifier = null;
        if (trimmedUsername.isEmpty()) {
            model.addAttribute("usernameError", "Vui lòng nhập email hoặc số điện thoại.");
            invalidInput = true;
        } else {
            try {
                normalizedIdentifier = LoginIdentifierClassifier.classifyAndNormalize(trimmedUsername);
                if (normalizedIdentifier.type() == LoginIdentifierType.USERNAME) {
                    model.addAttribute("usernameError", identifierFormatError(trimmedUsername));
                    invalidInput = true;
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("usernameError", identifierFormatError(trimmedUsername));
                invalidInput = true;
            }
        }

        if (matKhau == null || matKhau.isEmpty()) {
            model.addAttribute("passwordError", "Vui lòng nhập mật khẩu.");
            invalidInput = true;
        }

        if (invalidInput) {
            log.warn("[SECURITY_EVENT] INVALID_LOGIN_INPUT: IP: {}, Username: {}", ip, trimmedUsername);
            return "signin";
        }

        String normalizedUsername = normalizedIdentifier.value();
        String loginLimitKey = buildLoginLimitKey(normalizedUsername);

        // 1. Kiểm tra giới hạn số lần thử theo tài khoản, không khóa theo trình duyệt/IP
        if (loginRateLimiter.isBlocked(loginLimitKey)) {
            model.addAttribute("loi", "Tài khoản tạm thời bị khóa đăng nhập do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
            return "signin";
        }

        try {
            TaiKhoan tkDangNhap = userDangNhapService.kiemTraDangNhap(normalizedUsername, matKhau);

            // Chỉ cho phép KH
            String userRole = tkDangNhap.getVaiTro();
            if (!"KH".equals(userRole)) {
                loginRateLimiter.loginFailed(loginLimitKey);
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_CUSTOMER_LOGIN_ATTEMPT: Username: {}, IP: {}", trimmedUsername, ip);
                model.addAttribute("loi", "Tài khoản này không được phép đăng nhập tại trang khách hàng.");
                return "signin";
            }

            // Đăng nhập thành công -> Reset bộ đếm rate limiter
            loginRateLimiter.loginSucceeded(loginLimitKey);

            // Renew session ID, CSRF token and both representations of the account identity.
            LoginSession.establish(request, httpResponse, tkDangNhap);
            session = request.getSession(true);

            if (tkDangNhap.getTrangThaiTaiKhoan() == AccountStatus.GUEST) {
                // Chỉ tạo quyền đặt mật khẩu chính thức; chưa cấp session Member.
                session.setAttribute("pendingPasswordSetupAccountId", tkDangNhap.getId());
                session.setAttribute("temporaryPasswordVerified", true);
                session.setAttribute("isGuestView", true);
                session.setAttribute("guestCheckoutEmail", tkDangNhap.getUsername());
                session.setAttribute("nguoiDungDangNhap", tkDangNhap.getUsername());
                session.setAttribute("idNguoiDung", tkDangNhap.getId());
                session.setAttribute("vaiTro", tkDangNhap.getVaiTro());
                session.removeAttribute("activeRole");

                KhachHang guest = khachHangRepository.findByTaiKhoan_Id(tkDangNhap.getId());
                session.setAttribute("tenHienThi", guest != null
                        ? guest.getHoKh() + " " + guest.getTenKh()
                        : "Khách hàng");
                return "redirect:/user/setup-password";
            }


            // Lưu thông tin vào Session (giữ nguyên kiểu dữ liệu String cho nguoiDungDangNhap)
            session.setAttribute("nguoiDungDangNhap", tkDangNhap.getUsername());
            session.setAttribute("idNguoiDung", tkDangNhap.getId());
            session.setAttribute("vaiTro", tkDangNhap.getVaiTro());
            session.setAttribute("activeRole", "KH");

            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tkDangNhap.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                session.setAttribute("tenHienThi", "Khách hàng");
            }

            // Đăng nhập thành công, chuyển hướng về trang chủ
            return "redirect:/";

        } catch (AccountNotFoundException e) {
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] ACCOUNT_NOT_FOUND: Username: {}, IP: {}", trimmedUsername, ip);
            String message = normalizedIdentifier.type() == LoginIdentifierType.EMAIL
                    ? "Email này chưa được đăng ký."
                    : "Số điện thoại này chưa được đăng ký.";
            model.addAttribute("usernameError", message);
            return "signin";
        } catch (InvalidPasswordException e) {
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] INVALID_PASSWORD: Username: {}, IP: {}", trimmedUsername, ip);
            model.addAttribute("passwordError", e.getMessage());
            return "signin";
        } catch (AccountLockedException e) {
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] LOCKED_ACCOUNT_LOGIN: Username: {}, IP: {}", trimmedUsername, ip);
            model.addAttribute("loi", e.getMessage());
            return "signin";
        } catch (RuntimeException e) {
            loginRateLimiter.loginFailed(loginLimitKey);
            log.warn("[SECURITY_EVENT] FAILED_LOGIN: Username: {}, IP: {}, Lỗi: {}", trimmedUsername, ip, e.getMessage());
            model.addAttribute("loi", "Không thể đăng nhập lúc này. Vui lòng thử lại sau.");
            return "signin";
        }
    }

    private String identifierFormatError(String value) {
        if (value != null && value.contains("@")) {
            return "Định dạng email không hợp lệ.";
        }
        if (value != null && value.trim().matches("^[+0-9].*")) {
            return "Số điện thoại Việt Nam không hợp lệ.";
        }
        return "Vui lòng nhập email hoặc số điện thoại hợp lệ.";
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Loại bỏ thẻ HTML/Script cơ bản để tránh XSS/injection thô sơ
        return input.replaceAll("<[^>]*>", "");
    }

    private String buildLoginLimitKey(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    // Đăng xuất
    @GetMapping("/dang-xuat")
    public String xuLyDangXuat(HttpSession session) {
        if (session != null) {
            session.invalidate(); // Xóa toàn bộ dữ liệu trong Session
        }
        SecurityContextHolder.clearContext(); // Xóa context của Spring Security
        return "redirect:/user/dang-nhap";
    }

    @GetMapping("/google-success")
    public String googleSuccess(org.springframework.security.core.Authentication authentication, HttpServletRequest request, HttpSession session, HttpServletResponse httpResponse) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) return "redirect:/user/dang-nhap";
        // Rút trích Email và Tên từ Google
        String email = oauth2Token.getPrincipal().getAttribute("email");
        String name = oauth2Token.getPrincipal().getAttribute("name");
        String ip = request.getRemoteAddr();

        try {
            // Xử lý tạo/lấy tài khoản từ DB
            TaiKhoan tk = userDangNhapService.xuLyDangNhapGoogle(email, name);

            String userRole = tk.getVaiTro();
            if (!"KH".equals(userRole)) {
                LoginSession.clearAuthentication(request, httpResponse);
                log.warn("[SECURITY_EVENT] UNAUTHORIZED_GOOGLE_LOGIN_ATTEMPT: Email: {}, IP: {}", email, ip);
                return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode("Tài khoản này không được phép đăng nhập tại trang khách hàng.", java.nio.charset.StandardCharsets.UTF_8);
            }

            // Apply the same local session transition after OAuth verification.
            LoginSession.establish(request, httpResponse, tk);
            session = request.getSession(true);

            if (tk.getTrangThaiTaiKhoan() == AccountStatus.GUEST) {
                session.setAttribute("pendingPasswordSetupAccountId", tk.getId());
                session.setAttribute("temporaryPasswordVerified", true);
                session.setAttribute("isGuestView", true);
                session.setAttribute("guestCheckoutEmail", tk.getUsername());
                session.setAttribute("nguoiDungDangNhap", tk.getUsername());
                session.setAttribute("idNguoiDung", tk.getId());
                session.setAttribute("vaiTro", tk.getVaiTro());
                session.removeAttribute("activeRole");
                return "redirect:/user/setup-password";
            }

            // ÉP VÀO SESSION CỤC BỘ (Giúp Giỏ hàng và Dashboard nhận diện được user)
            session.setAttribute("nguoiDungDangNhap", tk.getUsername());
            session.setAttribute("idNguoiDung", tk.getId());
            session.setAttribute("vaiTro", tk.getVaiTro());
            session.setAttribute("activeRole", "KH");

            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                session.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
            } else {
                session.setAttribute("tenHienThi", name != null ? name : "Người dùng Google");
            }

            return "redirect:/";
        } catch (RuntimeException e) {
            LoginSession.clearAuthentication(request, httpResponse);
            log.warn("[SECURITY_EVENT] FAILED_LOGIN: Google Email: {}, IP: {}, Lỗi: {}", email, ip, e.getMessage());
            return "redirect:/user/dang-nhap?loi=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
