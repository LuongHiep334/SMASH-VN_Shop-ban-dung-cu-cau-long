package com.smashvn.shop.dto.product;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Display-only price: prefer purchasable variants; never changes checkout pricing. */
public record CustomerPriceSummary(BigDecimal giaBan, BigDecimal giaSauGiam,
                                   int giamGia, boolean conHang, boolean nhieuMucGia) {
    public static CustomerPriceSummary from(SanPham product, List<SanPhamChiTiet> variants) {
        // Initialize the existing promotion collection inside the caller's read-only transaction.
        if (product.getCacDotGiamGia() != null) product.getCacDotGiamGia().size();
        int discount = product.getActiveGiamGiaPhanTram();
        List<SanPhamChiTiet> active = variants == null ? List.of() : variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getTrangThaiValue()) && v.getGiaBan() != null).toList();
        List<SanPhamChiTiet> stocked = active.stream().filter(v -> v.getSoLuongTon() > 0).toList();
        List<SanPhamChiTiet> priced = stocked.isEmpty() ? active : stocked;
        SanPhamChiTiet cheapest = priced.stream().min(Comparator.comparing(SanPhamChiTiet::getGiaBan)).orElse(null);
        return new CustomerPriceSummary(cheapest == null ? null : cheapest.getGiaBan(),
                cheapest == null ? null : product.getGiaSauGiam(cheapest.getGiaBan()), discount,
                !stocked.isEmpty(), priced.stream().map(v -> v.getGiaBan().stripTrailingZeros()).distinct().count() > 1);
    }
}
