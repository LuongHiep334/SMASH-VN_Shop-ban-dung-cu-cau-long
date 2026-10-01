package com.smashvn.shop.util;

import java.util.Locale;

public class LoginIdentifierClassifier {

    public enum LoginIdentifierType {
        EMAIL,
        PHONE,
        USERNAME
    }

    public record NormalizedLoginIdentifier(
        LoginIdentifierType type,
        String value
    ) {}

    public static NormalizedLoginIdentifier classifyAndNormalize(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email hoặc số điện thoại hợp lệ.");
        }
        String input = rawInput.trim();

        // 1. Check if it matches email format
        if (input.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}$")) {
            return new NormalizedLoginIdentifier(LoginIdentifierType.EMAIL, input.toLowerCase(Locale.ROOT));
        }

        // 2. Normalize and check if it's a valid phone number
        String normalizedPhone = PhoneUtils.normalize(input);
        if (PhoneUtils.isValid(normalizedPhone)) {
            return new NormalizedLoginIdentifier(LoginIdentifierType.PHONE, normalizedPhone);
        }

        // 3. Check if it matches standard username format (alphanumeric, dots, hyphens, underscores)
        if (input.matches("^[A-Za-z0-9_.-]{3,50}$")) {
            return new NormalizedLoginIdentifier(LoginIdentifierType.USERNAME, input);
        }

        // 4. Invalid
        throw new IllegalArgumentException("Vui lòng nhập email hoặc số điện thoại hợp lệ.");
    }
}
