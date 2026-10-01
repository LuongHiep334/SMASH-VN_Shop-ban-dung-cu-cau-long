package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import org.springframework.stereotype.Service;

/**
 * Nguồn quyết định duy nhất cho trạng thái công khai và khả năng mua sản phẩm.
 */
@Service
public class ProductAvailabilityService {

    public boolean isProductPublished(SanPham product) {
        return product != null
                && Boolean.TRUE.equals(product.getTrangThaiValue())
                && product.getDanhMuc() != null
                && Boolean.TRUE.equals(product.getDanhMuc().getTrangThai())
                && product.getThuongHieu() != null
                && Boolean.TRUE.equals(product.getThuongHieu().getTrangThai());
    }

    public boolean isVariantPublished(SanPhamChiTiet variant) {
        return variant != null
                && Boolean.TRUE.equals(variant.getTrangThaiValue())
                && isProductPublished(variant.getSanPham());
    }

    public boolean isVariantPurchasable(SanPhamChiTiet variant, int requestedQuantity) {
        if (requestedQuantity <= 0 || !isVariantPublished(variant)) {
            return false;
        }
        Integer stock = variant.getSoLuongTon();
        return stock != null && stock >= requestedQuantity;
    }
}
