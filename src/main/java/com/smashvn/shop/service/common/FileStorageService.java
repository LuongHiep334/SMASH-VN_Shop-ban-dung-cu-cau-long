package com.smashvn.shop.service.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.path:uploads}")
    private String uploadPathConfig;

    private final Tika tika = new Tika();

    /**
     * Tải lên danh sách hình ảnh một cách an toàn và trả về danh sách tên tệp ngẫu nhiên
     */
    public List<String> saveReviewImages(List<MultipartFile> files) throws Exception {
        return saveImages(files, "reviews");
    }

    public List<String> saveBlogImages(List<MultipartFile> files) throws Exception {
        return saveImages(files, "blog");
    }

    /**
     * Tải lên danh sách hình ảnh một cách an toàn và lưu vào thư mục được chỉ định
     */
    public List<String> saveImages(List<MultipartFile> files, String folderName) throws Exception {
        List<String> savedFileNames = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return savedFileNames;
        }

        // Loại bỏ các tệp rỗng/không được chọn
        List<MultipartFile> activeFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                activeFiles.add(file);
            }
        }

        if (activeFiles.isEmpty()) {
            return savedFileNames;
        }

        // 1. Kiểm tra tối đa 5 hình ảnh
        if (activeFiles.size() > 5) {
            throw new IllegalArgumentException("Bạn chỉ được tải lên tối đa 5 hình ảnh.");
        }

        // 2. Kiểm tra tổng dung lượng tải lên (tối đa 20 MB)
        long totalSize = 0;
        for (MultipartFile file : activeFiles) {
            totalSize += file.getSize();
        }
        if (totalSize > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("Tổng dung lượng các hình ảnh tải lên không được vượt quá 20MB.");
        }

        // 3. Tiến hành xử lý từng tệp
        for (MultipartFile file : activeFiles) {
            // Kiểm tra dung lượng đơn lẻ (tối đa 5 MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Kích thước mỗi hình ảnh không được vượt quá 5MB.");
            }

            // Kiểm tra đuôi mở rộng
            String origName = file.getOriginalFilename();
            String ext = "";
            if (origName != null && origName.contains(".")) {
                ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
            }
            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("webp")) {
                throw new IllegalArgumentException("Định dạng tệp không hợp lệ! Chỉ cho phép JPG, JPEG, PNG, WEBP.");
            }

            // Kiểm tra MIME type thực tế bằng Apache Tika
            try (InputStream is = file.getInputStream()) {
                String mimeType = tika.detect(is);
                if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp"))) {
                    throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ! MIME type không được chấp nhận.");
                }
            }

            // Kiểm tra nội dung hình ảnh thực tế bằng ImageIO (ngăn chặn chèn mã độc/giả mạo exe)
            try (InputStream is = file.getInputStream()) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);
                if (image == null) {
                    throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
            }

            // Tạo tên file UUID ngẫu nhiên tránh trùng lặp
            String secureFileName = UUID.randomUUID().toString() + "." + ext;

            // Xác định thư mục lưu trữ và chống Path Traversal
            Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
            Path targetFolder = rootUploadPath.resolve(folderName).normalize();
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            Path targetFilePath = targetFolder.resolve(secureFileName).normalize().toAbsolutePath();
            if (!targetFilePath.startsWith(targetFolder.toAbsolutePath().normalize())) {
                throw new SecurityException("Đường dẫn tải lên tệp không hợp lệ.");
            }

            // Lưu tệp lên đĩa
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.error("[UPLOAD_FAILURE] Failed to save image to {}: {}", folderName, e.getMessage());
                // Xóa các file đã upload trước đó trong lượt này nếu bị lỗi giữa chừng
                for (String name : savedFileNames) {
                    deleteImage(name, folderName);
                }
                throw new RuntimeException("Không thể lưu hình ảnh. Vui lòng thử lại sau.", e);
            }

            savedFileNames.add(secureFileName);
        }

        return savedFileNames;
    }

    public void deleteFiles(List<String> fileNames, String folderName) {
        if (fileNames == null || fileNames.isEmpty()) return;
        for (String name : fileNames) {
            deleteImage(name, folderName);
        }
    }

    /**
     * Xóa tệp hình ảnh vật lý trên đĩa
     */
    public void deleteImage(String fileName, String folderName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        try {
            Path rootUploadPath = Paths.get(uploadPathConfig != null ? uploadPathConfig : "uploads").toAbsolutePath().normalize();
            Path targetFolder = rootUploadPath.resolve(folderName).normalize();
            Path filePath = targetFolder.resolve(fileName).normalize().toAbsolutePath();

            // Chống xóa ngoài thư mục cho phép (Path Traversal)
            if (filePath.startsWith(targetFolder.toAbsolutePath().normalize())) {
                Files.deleteIfExists(filePath);
                log.info("[FILE_CLEANUP] Deleted image file: {}", filePath);
            }
        } catch (Exception e) {
            log.error("[FILE_CLEANUP_ERROR] Failed to delete image file {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Tải lên và xác thực video bằng chứng đổi/trả một cách an toàn.
     * Chỉ chấp nhận đúng 1 video hợp lệ (MP4, WEBM, MOV), dung lượng tối đa 50MB.
     * Lưu vào thư mục uploads/returns/{orderId}/{uuid}.ext
     */
    public List<String> storeReturnEvidenceVideos(MultipartFile[] files, Integer orderId) throws Exception {
        if (orderId == null) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        }

        List<MultipartFile> activeFiles = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty() && file.getSize() > 0) {
                    activeFiles.add(file);
                }
            }
        }

        if (activeFiles.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng đính kèm video bằng chứng.");
        }

        if (activeFiles.size() > 1) {
            throw new IllegalArgumentException("Mỗi yêu cầu chỉ được đính kèm một video bằng chứng.");
        }

        MultipartFile file = activeFiles.get(0);

        // 1. Kiểm tra dung lượng tối đa 50MB
        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Video bằng chứng không được vượt quá 50MB.");
        }

        // 2. Kiểm tra phần mở rộng file (extension)
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
        }
        List<String> allowedExtensions = List.of("mp4", "webm", "mov");
        if (!allowedExtensions.contains(ext)) {
            throw new IllegalArgumentException("Video bằng chứng chỉ hỗ trợ MP4, WEBM hoặc MOV.");
        }

        // 3. Kiểm tra MIME type thực tế qua Apache Tika (chống giả mạo file .exe/.sh thành .mp4)
        List<String> allowedMimeTypes = List.of(
                "video/mp4",
                "video/webm",
                "video/quicktime",
                "video/x-matroska"
        );
        try (InputStream is = file.getInputStream()) {
            String detectedMime = tika.detect(is);
            if (detectedMime == null || (!allowedMimeTypes.contains(detectedMime.toLowerCase()) && !detectedMime.toLowerCase().startsWith("video/"))) {
                throw new IllegalArgumentException("Video bằng chứng chỉ hỗ trợ MP4, WEBM hoặc MOV.");
            }
        }

        // 4. Phân giải đường dẫn lưu trữ và chống Path Traversal
        Path rootUploadPath = Paths.get(uploadPathConfig != null ? uploadPathConfig : "uploads").toAbsolutePath().normalize();
        Path targetFolder = rootUploadPath.resolve("returns").resolve(String.valueOf(orderId)).normalize();
        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        if (!targetFolder.startsWith(rootUploadPath)) {
            throw new SecurityException("Đường dẫn tải lên tệp không hợp lệ.");
        }

        String secureFileName = UUID.randomUUID().toString() + "." + ext;
        Path targetFilePath = targetFolder.resolve(secureFileName).normalize().toAbsolutePath();
        if (!targetFilePath.startsWith(targetFolder)) {
            throw new SecurityException("Đường dẫn tải lên tệp không hợp lệ.");
        }

        // 5. Lưu file vào đĩa
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("[UPLOAD_FAILURE] Failed to save return evidence video for order #{}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể lưu video bằng chứng. Vui lòng thử lại sau.", e);
        }

        String relativePath = "/uploads/returns/" + orderId + "/" + secureFileName;
        return List.of(relativePath);
    }

    /**
     * Dọn dẹp / xóa file bằng chứng đổi trả khi xảy ra lỗi
     */
    public void deleteReturnFiles(List<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return;
        }
        Path rootUploadPath = Paths.get(uploadPathConfig != null ? uploadPathConfig : "uploads").toAbsolutePath().normalize();
        for (String relPath : relativePaths) {
            try {
                if (relPath != null && relPath.startsWith("/uploads/")) {
                    String subPath = relPath.substring("/uploads/".length());
                    Path filePath = rootUploadPath.resolve(subPath).normalize().toAbsolutePath();
                    if (filePath.startsWith(rootUploadPath)) {
                        Files.deleteIfExists(filePath);
                        log.info("[RETURN_FILE_CLEANUP] Deleted return evidence file: {}", filePath);
                    }
                }
            } catch (Exception e) {
                log.warn("[RETURN_FILE_CLEANUP_ERROR] Failed to delete return file {}: {}", relPath, e.getMessage());
            }
        }
    }
}
