package com.smashvn.shop.util;

import com.smashvn.shop.exception.PromotionValidationException;

/**
 * Kieu ap dung san pham khi tao/sua dot giam gia.
 *
 * <ul>
 *   <li>{@code MANUAL}      - Admin tick chon tung san pham thu cong.</li>
 *   <li>{@code PRICE_RANGE} - He thong tu tim san pham co gia trong khoang nhap.</li>
 * </ul>
 *
 * <p>Enum nay chi ton tai o tang form/service,
 * <b>khong duoc luu vao DB</b>.</p>
 */
public enum ApDungKieu {
    MANUAL,
    PRICE_RANGE;

    /**
     * Parse an toan tu chuoi form.
     * Neu chuoi rong/null thi mac dinh la {@code MANUAL}.
     * Neu gia tri khong hop le thi nem {@link PromotionValidationException}.
     *
     * @param value chuoi tu form (co the null/rong).
     * @return enum tuong ung.
     * @throws PromotionValidationException neu gia tri khong hop le.
     */
    public static ApDungKieu fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MANUAL; // mac dinh
        }
        try {
            return ApDungKieu.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PromotionValidationException(
                "Ki\u1ec3u \u00e1p d\u1ee5ng kh\u00f4ng h\u1ee3p l\u1ec7! Ch\u1ec9 cho ph\u00e9p 'MANUAL' ho\u1eb7c 'PRICE_RANGE'.");
        }
    }
}
