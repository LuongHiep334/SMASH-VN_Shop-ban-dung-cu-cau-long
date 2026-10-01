package com.smashvn.shop.dto.order;

import java.io.Serializable;

public record OperationalInsightDTO(
    String type,
    String title,
    String message,
    String icon
) implements Serializable {}
