package com.smashvn.shop.entity;

public enum OrderStatus {
    CHO_THANH_TOAN("cho_thanh_toan"),
    CHO_XAC_NHAN("cho_xac_nhan"),
    DA_XAC_NHAN("da_xac_nhan"),
    DANG_CHUAN_BI_HANG("dang_chuan_bi_hang"),
    SAN_SANG_GIAO("san_sang_giao"),
    DA_TAO_VAN_DON_GHN("da_tao_van_don_ghn"),
    DA_BAN_GIAO_GHN("da_ban_giao_ghn"),
    DANG_GIAO("dang_giao"),
    GIAO_THAT_BAI("giao_that_bai"),
    DANG_LAY_HANG("dang_lay_hang"),
    DA_GIAO("da_giao"),
    DA_HUY("da_huy"),
    STOCK_CONFLICT("stock_conflict");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
