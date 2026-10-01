package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.SanPhamChiTiet;
import java.math.BigDecimal;

public interface PricingService {
    BigDecimal calculateCurrentSellingPrice(SanPhamChiTiet spct);
    PriceSnapshot buildPriceSnapshot(SanPhamChiTiet spct);
    BigDecimal calculateLineTotal(SanPhamChiTiet spct, Integer quantity);
}
