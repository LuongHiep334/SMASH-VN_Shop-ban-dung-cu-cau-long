package com.smashvn.shop.entity;

public enum RefundStatus {
    PENDING("PENDING", "Chờ hoàn tiền"),
    COMPLETED("COMPLETED", "Đã hoàn tiền");

    private final String value;
    private final String label;

    RefundStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
