package com.smashvn.shop.util;

public class ValidationUtils {

    public static final String PHONE_REGEX = "^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$";
    
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "[EMPTY]";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? trimmed.substring(atIndex) : "");
        }
        String namePart = trimmed.substring(0, atIndex);
        String domainPart = trimmed.substring(atIndex);
        return namePart.charAt(0) + "***" + domainPart;
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "[EMPTY]";
        }
        return "[MASKED]";
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "[EMPTY]";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 2);
    }
}
