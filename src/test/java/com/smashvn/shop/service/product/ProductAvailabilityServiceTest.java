package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.ThuongHieu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAvailabilityServiceTest {

    private final ProductAvailabilityService service = new ProductAvailabilityService();
    private DanhMuc category;
    private ThuongHieu brand;
    private SanPham product;
    private SanPhamChiTiet variant;

    @BeforeEach
    void setUp() {
        category = new DanhMuc();
        category.setTrangThai(true);

        brand = new ThuongHieu();
        brand.setTrangThai(true);

        product = new SanPham();
        product.setDanhMuc(category);
        product.setThuongHieu(brand);
        product.setTrangThai("dang_ban");

        variant = new SanPhamChiTiet();
        variant.setSanPham(product);
        variant.setTrangThai("dang_ban");
        variant.setSoLuongTon(5);
    }

    @Test
    void activeHierarchyIsPublishedAndPurchasableWithinStock() {
        assertTrue(service.isProductPublished(product));
        assertTrue(service.isVariantPublished(variant));
        assertTrue(service.isVariantPurchasable(variant, 5));
        assertFalse(service.isVariantPurchasable(variant, 6));
    }

    @Test
    void hiddenCategoryMakesProductAndVariantUnavailable() {
        category.setTrangThai(false);

        assertFalse(service.isProductPublished(product));
        assertFalse(service.isVariantPublished(variant));
        assertFalse(service.isVariantPurchasable(variant, 1));
    }

    @Test
    void hiddenBrandMakesProductAndVariantUnavailable() {
        brand.setTrangThai(false);

        assertFalse(service.isProductPublished(product));
        assertFalse(service.isVariantPublished(variant));
    }

    @Test
    void inactiveVariantAndInvalidQuantityCannotBePurchased() {
        variant.setTrangThai("ngung_kinh_doanh");
        assertFalse(service.isVariantPurchasable(variant, 1));

        variant.setTrangThai("dang_ban");
        assertFalse(service.isVariantPurchasable(variant, 0));
    }
}
