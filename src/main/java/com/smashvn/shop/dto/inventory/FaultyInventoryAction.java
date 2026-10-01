package com.smashvn.shop.dto.inventory;

/**
 * Cac hanh dong xu ly san pham dang nam trong kho loi – Phase 3.
 */
public enum FaultyInventoryAction {

    SUA_XONG_NHAP_LAI_KHO("Sửa xong - Nhập lại kho bán", "[KHO_LOI_SUA_XONG_NHAP_LAI_KHO]", "bg-success"),
    TIEU_HUY("Tiêu hủy", "[KHO_LOI_TIEU_HUY]", "bg-danger"),
    TRA_NHA_CUNG_CAP("Trả nhà cung cấp", "[KHO_LOI_TRA_NCC]", "bg-warning text-dark");

    private final String label;
    private final String logPrefix;
    private final String badgeClass;

    FaultyInventoryAction(String label, String logPrefix, String badgeClass) {
        this.label = label;
        this.logPrefix = logPrefix;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public static FaultyInventoryAction fromString(String text) {
        if (text == null || text.isBlank()) return null;
        for (FaultyInventoryAction action : values()) {
            if (action.name().equalsIgnoreCase(text.trim())) {
                return action;
            }
        }
        return null;
    }
}