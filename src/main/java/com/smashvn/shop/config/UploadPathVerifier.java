package com.smashvn.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class UploadPathVerifier {

    @Value("${app.upload.path}")
    private String uploadPathStr;

    @PostConstruct
    public void verifyUploadPath() {
        log.info("Bắt đầu kiểm tra cấu hình thư mục tải lên: {}", uploadPathStr);
        
        if (uploadPathStr == null || uploadPathStr.trim().isEmpty()) {
            log.error("Cấu hình 'app.upload.path' bắt buộc phải được thiết lập và không được trống!");
            throw new IllegalStateException("Cấu hình 'app.upload.path' bắt buộc phải được thiết lập và không được trống!");
        }

        Path path = Paths.get(uploadPathStr).toAbsolutePath().normalize();
        log.info("Đường dẫn tuyệt đối sau khi phân giải: {}", path);

        // 2. Thử tạo thư mục trước nếu chưa tồn tại
        if (!Files.exists(path)) {
            try {
                log.info("Thư mục upload chưa tồn tại. Tiến hành tạo thư mục: {}", path);
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("Không thể tạo thư mục upload: {}. Lỗi: {}", path, e.getMessage());
                throw new IllegalStateException("Không thể tạo thư mục upload: " + path + ". Lỗi: " + e.getMessage(), e);
            }
        }

        // 3. Kiểm tra quyền ghi
        if (!Files.isWritable(path)) {
            log.error("Thư mục upload không có quyền ghi (Not Writable): {}", path);
            throw new IllegalStateException("Thư mục upload không có quyền ghi (Not Writable): " + path);
        }

        log.info("Xác minh cấu hình thư mục tải lên THÀNH CÔNG: {}", path);
    }
}
