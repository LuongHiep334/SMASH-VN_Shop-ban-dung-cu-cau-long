package com.smashvn.shop.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

/**
 * DTO hứng toàn bộ dữ liệu Form thêm mới sản phẩm và sinh biến thể tự động từ Admin UI.
 * Bao gồm thông tin sản phẩm gốc, cấu hình tài chính mặc định, ảnh chính, ảnh theo màu và danh sách các biến thể indexed.
 */
@Data
public class SanPhamCreateRequest {
    private String tenSanPham;
    private Integer idDanhMuc;
    private Integer idThuongHieu;
    private String moTa;

    private BigDecimal giaNhapDefault;
    private BigDecimal giaBanDefault;
    private Integer soLuongTonDefault;

    private MultipartFile fileAnh;
    private String mucCang;

    private List<MultipartFile> colorImages = new ArrayList<>();
    private List<BienTheCreateRequest> variants = new ArrayList<>();
}
