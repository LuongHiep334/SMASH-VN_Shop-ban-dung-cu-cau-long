package com.smashvn.shop.util;

public class PhoneUtils {

    /**
     * Normalize phone number:
     * - Trim
     * - Remove spaces, dashes, dots
     * - Convert "+84" and "84" (when 11 characters) prefixes to "0"
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        String cleaned = phone.trim().replaceAll("[\\s\\-\\.]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        return cleaned;
    }

    /**
     * Validate strictly against standard Vietnamese mobile phone format:
     * ^(03|05|07|08|09)\d{8}$
     */
    public static boolean isValid(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^(03|05|07|08|09)\\d{8}$");
    }
}
