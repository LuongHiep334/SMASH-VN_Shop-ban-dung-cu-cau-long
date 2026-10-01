package com.smashvn.shop.entity;

public enum PaymentMethod {
    COD("cod"),
    SEPAY("sepay"),
    ZALOPAY("zalopay"); // Kept for backward compatibility

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
