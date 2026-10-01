package com.smashvn.shop.util;

public final class RacketSpecUtils {

    private RacketSpecUtils() {
    }

    public static String sanitizeRecommendedTension(String mucCang) {
        if (mucCang == null) {
            return "";
        }

        String cleaned = mucCang.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > 50) {
            throw new IllegalArgumentException("Sức căng khuyến nghị không được vượt quá 50 ký tự.");
        }
        if (cleaned.matches(".*[<>].*") || cleaned.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw new IllegalArgumentException("Sức căng khuyến nghị chứa ký tự không hợp lệ.");
        }

        return cleaned;
    }
}
