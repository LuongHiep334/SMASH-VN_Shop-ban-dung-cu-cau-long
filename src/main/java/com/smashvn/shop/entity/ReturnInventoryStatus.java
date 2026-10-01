package com.smashvn.shop.entity;

public enum ReturnInventoryStatus {
    CHUA_XU_LY("CHUA_XU_LY", "Chưa xử lý kho"),
    DA_HOAN_KHO("DA_HOAN_KHO", "Đã khôi phục tồn kho bán"),
    DA_CHUYEN_KHO_LOI("DA_CHUYEN_KHO_LOI", "Đã chuyển vào kho hàng lỗi"),
    DANG_TRA_LAI_KHACH("DANG_TRA_LAI_KHACH", "Đang xử lý gửi trả sản phẩm cho khách"),
    DA_TRA_LAI_KHACH("DA_TRA_LAI_KHACH", "Đã xử lý trả lại sản phẩm cho khách thành công");

    private final String value;
    private final String label;

    ReturnInventoryStatus(String value, String label) {
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
