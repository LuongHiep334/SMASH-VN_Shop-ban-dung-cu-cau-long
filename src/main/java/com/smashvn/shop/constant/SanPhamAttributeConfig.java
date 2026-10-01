package com.smashvn.shop.constant;

import java.util.List;
import java.util.Set;

/**
 * Class cấu hình chứa danh sách Whitelist các thuộc tính sản phẩm hợp lệ (Màu sắc, Trọng lượng vợt, Size giày, Size áo).
 * Dùng để kiểm tra hợp lệ dữ liệu đầu vào (Validation Backend) và đổ danh sách tùy chọn lên giao diện Admin.
 */
public final class SanPhamAttributeConfig {

    public static final List<String> WHITELIST_TRONG_LUONG_VOT = List.of("3U", "4U", "5U");
    
    public static final List<String> WHITELIST_KICH_THUOC_GIAY = List.of(
        "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46"
    );
    
    public static final List<String> WHITELIST_KICH_THUOC_TRANG_PHUC = List.of(
        "XS", "S", "M", "L", "XL", "2XL", "3XL"
    );

    public static final Set<String> ALLOWED_TRONG_LUONG_VOT = Set.of("3U", "4U", "5U");
    
    public static final Set<String> ALLOWED_KICH_THUOC_GIAY = Set.of(
        "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46"
    );
    
    public static final Set<String> ALLOWED_KICH_THUOC_TRANG_PHUC = Set.of(
        "XS", "S", "M", "L", "XL", "2XL", "3XL"
    );

    public static final List<String> DEFAULT_MAU_SAC = List.of(
        "Đỏ", "Xanh dương", "Xanh lá", "Đen", "Trắng", "Vàng", "Cam", "Tím"
    );

    private SanPhamAttributeConfig() {}
}
