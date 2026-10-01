package com.smashvn.shop.specification;

import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

final class CustomerPriceExpressions {
    private CustomerPriceExpressions() { }

    static Predicate availableOrAllSoldOut(Root<SanPham> product, Root<SanPhamChiTiet> variant,
                                          CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Integer> available = query.subquery(Integer.class);
        Root<SanPhamChiTiet> other = available.from(SanPhamChiTiet.class);
        available.select(cb.literal(1)).where(cb.equal(other.get("sanPham"), product),
                cb.isTrue(other.get("trangThaiValue")), cb.gt(other.get("soLuongTon"), 0));
        return cb.or(cb.gt(variant.get("soLuongTon"), 0), cb.not(cb.exists(available)));
    }

    static Expression<BigDecimal> discounted(Expression<BigDecimal> basePrice, Root<SanPham> product,
                                              CriteriaQuery<?> query, CriteriaBuilder cb) {
        LocalDateTime now = LocalDateTime.now();
        Subquery<Integer> promotion = query.subquery(Integer.class);
        Root<DotGiamGia> campaign = promotion.from(DotGiamGia.class);
        Join<DotGiamGia, SanPham> applied = campaign.join("sanPhams");
        promotion.select(cb.max(campaign.get("phanTramGiam"))).where(cb.equal(applied, product),
                cb.or(cb.isNull(campaign.get("trangThai")), cb.isTrue(campaign.get("trangThai"))),
                cb.or(cb.isNull(campaign.get("ngayBatDau")), cb.lessThanOrEqualTo(campaign.get("ngayBatDau"), now)),
                cb.or(cb.isNull(campaign.get("ngayKetThuc")), cb.greaterThanOrEqualTo(campaign.get("ngayKetThuc"), now)));
        Expression<BigDecimal> multiplier = cb.diff(BigDecimal.valueOf(100), cb.coalesce(promotion, 0).as(BigDecimal.class));
        Expression<BigDecimal> amount = cb.prod(cb.prod(basePrice, multiplier), new BigDecimal("0.01"));
        return cb.function("round", BigDecimal.class, amount, cb.literal(2));
    }

    static Expression<BigDecimal> representative(Root<SanPham> product, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<BigDecimal> base = query.subquery(BigDecimal.class);
        Root<SanPhamChiTiet> variant = base.from(SanPhamChiTiet.class);
        base.select(cb.min(variant.get("giaBan"))).where(cb.equal(variant.get("sanPham"), product),
                cb.isTrue(variant.get("trangThaiValue")), availableOrAllSoldOut(product, variant, query, cb));
        // Apply the product-wide discount outside MIN (SQL Server disallows a subquery inside an aggregate).
        return discounted(base, product, query, cb);
    }
}
