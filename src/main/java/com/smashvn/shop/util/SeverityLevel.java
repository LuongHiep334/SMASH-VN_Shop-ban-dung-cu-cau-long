package com.smashvn.shop.util;

public enum SeverityLevel {
    NONE("Không vi phạm"),
    LOW("Nhẹ"),
    MEDIUM("Trung bình"),
    HIGH("Nặng"),
    CRITICAL("Nghiêm trọng");

    private final String displayName;

    SeverityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
