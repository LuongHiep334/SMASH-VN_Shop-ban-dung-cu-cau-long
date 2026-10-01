package com.smashvn.shop.dto.order;

import java.io.Serializable;
import java.math.BigDecimal;

/** Detached result retained in the session only after the order transaction returns. */
public record CompletedCheckout(
        Integer orderId,
        String maDonHang,
        String paymentMethod,
        BigDecimal tongTien,
        boolean guest,
        boolean newAccount,
        boolean grantGuestAccess,
        String guestEmail,
        Integer ghnToDistrictId,
        String ghnToWardCode) implements Serializable {

    public String redirectUrl() {
        if ("COD".equalsIgnoreCase(paymentMethod) || maDonHang == null || maDonHang.isBlank()
                || tongTien == null || tongTien.signum() <= 0) {
            return "/user/manage-order/" + orderId;
        }
        return "/payment/sepay/simulate?maDonHang="
                + java.net.URLEncoder.encode(maDonHang, java.nio.charset.StandardCharsets.UTF_8);
    }
}
