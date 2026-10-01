package com.smashvn.shop.service.product;

import com.smashvn.shop.dto.product.CustomerPriceSummary;
import com.smashvn.shop.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomerPriceSummaryTest {
    private SanPhamChiTiet variant(int price, int stock, boolean active) {
        return SanPhamChiTiet.builder().giaBan(BigDecimal.valueOf(price)).soLuongTon(stock).trangThaiValue(active).build();
    }

    @Test void prefersStockAndIgnoresInactiveCheapVariant() {
        SanPham product = SanPham.builder().build();
        var summary = CustomerPriceSummary.from(product, List.of(variant(10, 10, false),
                variant(100, 0, true), variant(300, 4, true), variant(200, 2, true)));
        assertEquals(new BigDecimal("200"), summary.giaBan());
        assertTrue(summary.conHang());
        assertTrue(summary.nhieuMucGia());
    }

    @Test void allSoldOutStillShowsLowestActivePriceAndStatus() {
        var summary = CustomerPriceSummary.from(SanPham.builder().build(),
                List.of(variant(300, 0, true), variant(200, 0, true)));
        assertEquals(new BigDecimal("200"), summary.giaSauGiam());
        assertFalse(summary.conHang());
    }

    @Test void emptyCatalogVariantDoesNotInventZeroPrice() {
        var summary = CustomerPriceSummary.from(SanPham.builder().build(), List.of());
        assertNull(summary.giaBan());
        assertNull(summary.giaSauGiam());
        assertFalse(summary.conHang());
    }

    @Test void appliesOnlyCurrentEnabledPromotionAndPreservesZeroPrice() {
        DotGiamGia active = promotion(25, true, -1, 1);
        SanPham product = SanPham.builder().cacDotGiamGia(Set.of(active, promotion(90, true, -2, -1),
                promotion(80, false, -1, 1), promotion(60, true, 1, 2))).build();
        var summary = CustomerPriceSummary.from(product, List.of(variant(1000000, 3, true)));
        assertEquals(new BigDecimal("750000.00"), summary.giaSauGiam());
        assertEquals(25, summary.giamGia());
        assertFalse(summary.nhieuMucGia());
        assertEquals(0, CustomerPriceSummary.from(product, List.of(variant(0, 1, true))).giaSauGiam().signum());
    }

    private DotGiamGia promotion(int percent, boolean enabled, int start, int end) {
        DotGiamGia campaign = new DotGiamGia();
        campaign.setPhanTramGiam(percent);
        campaign.setActive(enabled);
        campaign.setNgayBatDau(LocalDateTime.now().plusDays(start));
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(end));
        return campaign;
    }
}
