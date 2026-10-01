package com.smashvn.shop.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.smashvn.shop.service.user.UserDangNhapService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final UserDangNhapService userDangNhapService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        // Bỏ qua kiểm tra cho các trang đăng nhập/đăng xuất admin để tránh vòng lặp chuyển hướng
        if ("/admin/dang-nhap".equals(path) || "/admin/dang-xuat".equals(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);

        // Nếu chưa đăng nhập session, để Spring Security xử lý phân quyền và chuyển hướng
        if (session == null || session.getAttribute("idNguoiDung") == null) {
            return true;
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        // Kiểm tra tài khoản qua Cache dịch vụ để xem trạng thái có bị khóa không
        String trangThai = userDangNhapService.layTrangThaiTaiKhoan(idNguoiDung);
        if (!"hoat_dong".equals(trangThai) && !"cho_khoa".equals(trangThai)) {
            session.invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/admin/dang-nhap?loi=" + URLEncoder.encode("Tài khoản của bạn đã bị khóa.", StandardCharsets.UTF_8.toString()));
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && modelAndView != null) {
            String warningMsg = (String) session.getAttribute("warningMsg");
            if (warningMsg != null) {
                modelAndView.addObject("warningMsg", warningMsg);
                session.removeAttribute("warningMsg");
            }
        }
    }
}
