package com.smashvn.shop.util;

import java.math.BigDecimal;

public final class PromotionValidationConstants {
    private PromotionValidationConstants() {}

    public static final int MAX_CAMPAIGN_DISCOUNT_PERCENT = 40;
    public static final int MAX_VOUCHER_PERCENT = 100;
    public static final BigDecimal MAX_VND_VALUE = new BigDecimal("100000000"); // 100,000,000 VNĐ
    public static final int MAX_QUANTITY = 1000000; // 1,000,000 vouchers
}
