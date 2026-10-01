package com.smashvn.shop.dto.order;

import java.io.Serializable;

public record GrowthMetricDTO(
    Double currentValue,
    Double previousValue,
    Double percentageChange,
    String formattedChange,
    String direction,
    Boolean isNew
) implements Serializable {}
