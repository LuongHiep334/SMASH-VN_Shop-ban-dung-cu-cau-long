package com.smashvn.shop.dto.product;

import java.io.Serializable;

public record SlowMovingProductDTO(
    Integer productId,
    String productName,
    String categoryName,
    String image,
    Long stockQuantity,
    Long soldQuantity,
    String warningLevel,
    String warningBadge
) implements Serializable {}
