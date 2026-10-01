package com.smashvn.shop.dto.product;

import java.io.Serializable;
import java.math.BigDecimal;

public record TopProductDTO(
    Integer productId,
    String productName,
    String image,
    String categoryName,
    Long soldQuantity,
    BigDecimal revenue,
    Double percentage
) implements Serializable {

    public TopProductDTO(
            Integer productId,
            String productName,
            String image,
            String categoryName,
            Long soldQuantity,
            BigDecimal revenue) {
        this(productId, productName, image, categoryName, soldQuantity, revenue, 0.0);
    }

    public TopProductDTO(
            Integer productId,
            String productName,
            String image,
            String categoryName,
            Long soldQuantity,
            Double revenue) {
        this(productId, productName, image, categoryName, soldQuantity, revenue != null ? BigDecimal.valueOf(revenue) : BigDecimal.ZERO, 0.0);
    }

    public TopProductDTO withPercentage(Double percentage) {
        return new TopProductDTO(
            this.productId,
            this.productName,
            this.image,
            this.categoryName,
            this.soldQuantity,
            this.revenue,
            percentage != null ? percentage : 0.0
        );
    }
}
