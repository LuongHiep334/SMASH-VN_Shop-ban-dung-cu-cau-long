package com.smashvn.shop.dto.order;

import java.math.BigDecimal;

public class GeneralMetricsDTO {
    private final Long totalOrders;
    private final Long successfulOrders;
    private final Long cancelledOrders;
    private final BigDecimal totalRevenue;
    private final Double avgOrderValue;
    private final Long totalProductsSold;

    public GeneralMetricsDTO(
            Object totalOrders, 
            Object successfulOrders, 
            Object cancelledOrders, 
            Object totalRevenue, 
            Object avgOrderValue, 
            Object totalProductsSold) {
        this.totalOrders = toLong(totalOrders);
        this.successfulOrders = toLong(successfulOrders);
        this.cancelledOrders = toLong(cancelledOrders);
        this.totalRevenue = toBigDecimal(totalRevenue);
        this.avgOrderValue = toDouble(avgOrderValue);
        this.totalProductsSold = toLong(totalProductsSold);
    }

    private static Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0L;
    }

    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        return BigDecimal.ZERO;
    }

    private static Double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return 0.0;
    }

    // Record-style getters to maintain compatibility with existing service/templates
    public Long totalOrders() {
        return totalOrders;
    }

    public Long successfulOrders() {
        return successfulOrders;
    }

    public Long cancelledOrders() {
        return cancelledOrders;
    }

    public BigDecimal totalRevenue() {
        return totalRevenue;
    }

    public Double avgOrderValue() {
        return avgOrderValue;
    }

    public Long totalProductsSold() {
        return totalProductsSold;
    }

    // Standard JavaBeans getters for Jackson JSON serialization
    public Long getTotalOrders() {
        return totalOrders;
    }

    public Long getSuccessfulOrders() {
        return successfulOrders;
    }

    public Long getCancelledOrders() {
        return cancelledOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public Double getAvgOrderValue() {
        return avgOrderValue;
    }

    public Long getTotalProductsSold() {
        return totalProductsSold;
    }
}
