package com.smashvn.shop.util;

/**
 * Utility helper chuyên trách sinh Business Code cho Sản Phẩm (maSanPham)
 * và Biến Thể (sku) đảm bảo tính duy nhất, ổn định và không bị phụ thuộc vào thuộc tính thay đổi.
 */
public final class ProductCodeAndSkuGenerator {

    private ProductCodeAndSkuGenerator() {
        // Utility class, không khởi tạo instance
    }

    /**
     * Sinh mã sản phẩm theo định dạng: SP{id dạng tối thiểu 6 chữ số}
     * Ví dụ:
     * - id = 1 -> SP000001
     * - id = 123456 -> SP123456
     * - id = 1234567 -> SP1234567 (không bị truncate)
     */
    public static String generateProductCode(Integer productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Không thể sinh mã sản phẩm khi productId = null");
        }
        if (productId <= 0) {
            throw new IllegalArgumentException("productId phải là số nguyên dương (> 0)");
        }
        return String.format("SP%06d", productId);
    }

    /**
     * Sinh SKU biến thể theo định dạng: {maSanPham}-V{variantId dạng tối thiểu 6 chữ số}
     * Ví dụ:
     * - maSanPham = "SP000125", variantId = 750 -> SP000125-V000750
     * - maSanPham = "SP000125", variantId = 1234567 -> SP000125-V1234567 (không bị truncate)
     */
    public static String generateVariantSku(String maSanPham, Integer variantId) {
        if (maSanPham == null || maSanPham.isBlank()) {
            throw new IllegalArgumentException("Không thể sinh SKU vì mã sản phẩm bị null hoặc trống");
        }
        if (variantId == null) {
            throw new IllegalArgumentException("Không thể sinh SKU khi variantId = null");
        }
        if (variantId <= 0) {
            throw new IllegalArgumentException("variantId phải là số nguyên dương (> 0)");
        }
        return maSanPham.trim() + "-V" + String.format("%06d", variantId);
    }
}
