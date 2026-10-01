package com.smashvn.shop.entity;

public enum ReturnStatus {
    PENDING_APPROVAL("PENDING_APPROVAL", "Chờ shop duyệt trả hàng"),
    REJECTED("REJECTED", "Shop từ chối trả hàng"),
    WAITING_FOR_PICKUP("WAITING_FOR_PICKUP", "Chờ GHN lấy hàng trả"),
    PICKED_UP("PICKED_UP", "GHN đã lấy hàng từ khách"),
    RETURNING("RETURNING", "Đang vận chuyển trả về shop"),
    DELIVERED_TO_SHOP("DELIVERED_TO_SHOP", "Hàng đã giao về đến shop"),
    RETURNED("RETURNED", "Đã kiểm hàng & Nhập kho"),
    REFUNDED("REFUNDED", "Đã hoàn tiền cho khách"),
    LOST("LOST", "Mất hàng (Không hoàn kho)"),
    DAMAGED("DAMAGED", "Hỏng hàng (Không hoàn kho)"),
    PENDING_RETURN("PENDING_RETURN", "Chờ nhập kho"),
    EXCHANGE_STOCK_ALLOCATED("EXCHANGE_STOCK_ALLOCATED", "Đã phân bổ tồn kho hàng đổi"),
    EXCHANGE_SHIPPING("EXCHANGE_SHIPPING", "Đang giao sản phẩm đổi mới"),
    EXCHANGED("EXCHANGED", "Đã hoàn tất đổi hàng");

    private final String value;
    private final String label;

    ReturnStatus(String value, String label) {
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
