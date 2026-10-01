package com.smashvn.shop.entity;

public enum PaymentStatus {
    PENDING("pending"),
    PAID("paid"),
    FAILED("failed"),
    REFUNDED("refunded"),
    PAID_RECEIVED_AFTER_CANCEL("paid_received_after_cancel"),
    AMOUNT_MISMATCH("amount_mismatch");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
