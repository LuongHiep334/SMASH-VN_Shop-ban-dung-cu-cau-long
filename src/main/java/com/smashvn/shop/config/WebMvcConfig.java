package com.smashvn.shop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    @Value("${app.upload.path:uploads}")
    private String uploadPathConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Đọc ảnh từ cùng thư mục đã cấu hình cho các service lưu file.
        Path uploadDir = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        String uploadUri = uploadDir.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        // Map đường dẫn URL bắt đầu bằng /uploads/ tới thư mục vật lý
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/dang-nhap", "/admin/dang-xuat", "/css/**", "/js/**", "/images/**", "/uploads/**", "/webfonts/**");
    }
}
