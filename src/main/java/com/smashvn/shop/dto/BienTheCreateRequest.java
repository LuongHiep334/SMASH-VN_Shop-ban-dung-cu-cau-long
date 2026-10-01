package com.smashvn.shop.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * DTO đại diện cho dữ liệu của từng biến thể chi tiết (variants[i]) khi gửi từ Form thêm mới sản phẩm.
 * Hỗ trợ danh sách thuộc tính động (attributes) để tương thích với mô hình EAV mới.
 */
@Data
public class BienTheCreateRequest {
    private String mauSac;
    private String trongLuong;
    private String kichThuoc;
    private BigDecimal giaNhap;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private Integer colorIndex;

    private List<AttributeValueRequest> attributes = new ArrayList<>();

    public String getMauSac() {
        if (mauSac != null && !mauSac.isBlank()) return mauSac;
        return getAttributeValueByName("Màu sắc");
    }

    public String getKichThuoc() {
        if (kichThuoc != null && !kichThuoc.isBlank()) return kichThuoc;
        return getAttributeValueByName("Kích thước");
    }

    public String getTrongLuong() {
        if (trongLuong != null && !trongLuong.isBlank()) return trongLuong;
        return getAttributeValueByName("Trọng lượng");
    }

    public String getAttributeValueByName(String name) {
        if (attributes == null || name == null) return null;
        return attributes.stream()
                .filter(a -> a.getAttributeName() != null && name.equalsIgnoreCase(a.getAttributeName()))
                .map(AttributeValueRequest::getValue)
                .findFirst()
                .orElse(null);
    }
}
