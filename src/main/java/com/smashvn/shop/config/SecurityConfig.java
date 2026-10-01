package com.smashvn.shop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Kích hoạt lại bảo mật CSRF
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/user/dang-xuat",
                        "/admin/dang-xuat",
                        "/admin/don-hang/approve-refund/**",
                        "/admin/don-hang/reject-refund/**",
                        "/api/payment/zalopay/callback",
                        "/api/payment/sepay/ipn",
                        "/api/ghn/webhook",
                        "/api/ghn/admin/**",
                        "/api/chat/**",
                        "/api/chatbot/**",
                        "/api/newsletter/subscribe",
                        "/api/newsletter/unsubscribe-ajax"
                ))
                // Cấu hình các Header bảo mật nâng cao
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.sameOrigin());
                    headers.referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                })
                .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập công khai trang đăng nhập/đăng xuất admin và tài nguyên tĩnh
                .requestMatchers("/admin/dang-nhap", "/admin/dang-xuat").permitAll()
                .requestMatchers("/admin/nhan-vien/approve-lock/**", "/admin/nhan-vien/reject-lock/**").permitAll()
                .requestMatchers("/admin/don-hang/approve-refund/**", "/admin/don-hang/reject-refund/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/webfonts/**", "/vendor/**").permitAll()
                .requestMatchers("/user/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                .requestMatchers("/api/chatbot/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/attributes").hasAnyRole("QL", "NV")
                // Phân quyền chi tiết cho Admin/Staff theo Backend Enforcement (cả endpoint gốc và sub-paths)
                .requestMatchers("/admin/nguoi-dung", "/admin/nguoi-dung/**").hasRole("QL")
                .requestMatchers("/admin/nhan-vien", "/admin/nhan-vien/**").hasRole("QL")
                .requestMatchers("/admin/blog/publish/**", "/admin/blog/delete/**", "/admin/blogs/publish/**", "/admin/blogs/delete/**").hasRole("QL")
                .requestMatchers("/admin/blog", "/admin/blog/**", "/admin/blogs", "/admin/blogs/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/moderation/keywords", "/admin/moderation/keywords/**").hasRole("QL")
                .requestMatchers("/admin/thong-ke", "/admin/thong-ke/**").hasRole("QL")
                .requestMatchers("/admin/don-hang", "/admin/don-hang/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/san-pham", "/admin/san-pham/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/danh-muc", "/admin/danh-muc/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/pos", "/admin/pos/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/khach-hang", "/admin/khach-hang/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/transactions", "/admin/transactions/**").hasAnyRole("QL", "NV")
                .requestMatchers("/admin/danh-gia", "/admin/danh-gia/**").hasAnyRole("QL", "NV")
                // Phase 1 – Kho San Pham Loi: ca QL va NV deu truy cap duoc
                .requestMatchers("/admin/kho-san-pham-loi", "/admin/kho-san-pham-loi/**").hasAnyRole("QL", "NV")
                // Các trang quản trị còn lại chỉ dành cho QL
                .requestMatchers("/admin", "/admin/**").hasRole("QL")
                // Tất cả các request khác đều được permit
                .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestedWith = request.getHeader("X-Requested-With");
                    String accept = request.getHeader("Accept");
                    String uri = request.getRequestURI();
                    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)
                            || (accept != null && accept.contains("application/json"))
                            || uri.contains("-json")
                            || uri.contains("/api/")) {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/dang-nhap");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String requestedWith = request.getHeader("X-Requested-With");
                    String accept = request.getHeader("Accept");
                    String uri = request.getRequestURI();
                    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)
                            || (accept != null && accept.contains("application/json"))
                            || uri.contains("-json")
                            || uri.contains("/api/")) {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"Bạn không có quyền thực hiện thao tác này.\"}");
                    } else {
                        String ip = request.getRemoteAddr();
                        Integer idNguoiDung = (Integer) request.getSession().getAttribute("idNguoiDung");
                        if (accessDeniedException instanceof org.springframework.security.web.csrf.CsrfException) {
                            log.warn("[SECURITY_EVENT] CSRF_DENY: IP: {}, URL: {}, UserID: {}", ip, uri, idNguoiDung);
                        } else {
                            log.warn("[SECURITY_EVENT] ACCESS_DENY: IP: {}, URL: {}, UserID: {}", ip, uri, idNguoiDung);
                        }
                        request.getSession().setAttribute("errorMsg", "Bạn không có quyền truy cập vào chức năng này!");
                        response.sendRedirect(request.getContextPath() + "/admin/don-hang");
                    }
                })
                )
                .oauth2Login(oauth2 -> oauth2
                .loginPage("/user/dang-nhap") // Trỏ về trang đăng nhập của bạn
                .defaultSuccessUrl("/user/google-success", true) // Thành công thì gọi về API này
                .failureHandler((req, resp, exception) -> {
                    log.warn("[SECURITY_EVENT] GOOGLE_LOGIN_FAILURE: {}", exception.getMessage());
                    req.getSession().setAttribute("loi", "Đăng nhập bằng tài khoản Google không thành công. Vui lòng thử lại!");
                    resp.sendRedirect(req.getContextPath() + "/user/dang-nhap");
                })
                );

        return http.build();
    }

    // Đăng ký UploadSecurityFilter riêng biệt cho đường dẫn uploads
    @Bean
    public FilterRegistrationBean<UploadSecurityFilter> uploadSecurityFilterRegistration() {
        FilterRegistrationBean<UploadSecurityFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new UploadSecurityFilter());
        registrationBean.addUrlPatterns("/uploads/*");
        return registrationBean;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
