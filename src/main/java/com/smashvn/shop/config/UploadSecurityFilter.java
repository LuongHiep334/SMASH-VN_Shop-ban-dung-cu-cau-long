package com.smashvn.shop.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UploadSecurityFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // Chống giả mạo MIME type
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // CSP Sandbox cô lập hoàn toàn mã script, chỉ cho phép load tĩnh (default-src 'none')
        httpResponse.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
        chain.doFilter(request, response);
    }
}
