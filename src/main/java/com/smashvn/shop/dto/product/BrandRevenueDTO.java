package com.smashvn.shop.dto.product;

import java.io.Serializable;
import java.math.BigDecimal;

public record BrandRevenueDTO(
    Integer brandId,
    String brandName,
    Long soldQuantity,
    BigDecimal revenue,
    Double percentage
) implements Serializable {

    public BrandRevenueDTO(
            Integer brandId,
            String brandName,
            Long soldQuantity,
            BigDecimal revenue) {
        this(brandId, brandName, soldQuantity, revenue, 0.0);
    }

    public BrandRevenueDTO(
            Integer brandId,
            String brandName,
            Long soldQuantity,
            Double revenue) {
        this(brandId, brandName, soldQuantity, revenue != null ? BigDecimal.valueOf(revenue) : BigDecimal.ZERO, 0.0);
    }

    public BrandRevenueDTO withPercentage(Double percentage) {
        return new BrandRevenueDTO(
            this.brandId,
            this.brandName,
            this.soldQuantity,
            this.revenue,
            percentage != null ? percentage : 0.0
        );
    }
}
