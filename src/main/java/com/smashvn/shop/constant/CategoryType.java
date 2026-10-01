package com.smashvn.shop.constant;

import com.smashvn.shop.entity.DanhMuc;
import java.text.Normalizer;
import java.util.Locale;

public enum CategoryType {
    VOT, GIAY, TRANG_PHUC, HOP_CAU, CUOC, BALO, QUAN_CAN, BANG_QUAN, OTHER;

    public static CategoryType fromDanhMuc(DanhMuc dm) {
        return dm == null ? OTHER : fromName(dm.getTenDanhMuc());
    }

    public static CategoryType fromName(String rawName) {
        String name = normalize(rawName);
        if (name.isEmpty()) return OTHER;

        // Ưu tiên tiền tố mô tả loại hàng. Cách này tránh nhận nhầm
        // "Túi đựng vợt" thành VOT chỉ vì tên có chứa chữ "vợt".
        if (startsWithAny(name, "tui", "balo")) return BALO;
        if (startsWithAny(name, "quan can")) return QUAN_CAN;
        if (startsWithAny(name, "bang quan")) return BANG_QUAN;
        if (startsWithAny(name, "hop cau", "qua cau")) return HOP_CAU;
        if (startsWithAny(name, "day cuoc", "cuoc")) return CUOC;
        if (startsWithAny(name, "giay")) return GIAY;
        if (startsWithAny(name, "trang phuc", "ao", "quan", "vay", "tat", "vo")) return TRANG_PHUC;
        if (startsWithAny(name, "vot")) return VOT;
        return OTHER;
    }

    /**
     * Giữ chữ ký cũ để các tích hợp ngoài dự án không bị vỡ, nhưng ID không còn
     * tham gia phân loại nghiệp vụ.
     */
    @Deprecated(forRemoval = true)
    public static CategoryType fromIdOrName(DanhMuc dm, Integer id) {
        return fromDanhMuc(dm);
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.equals(prefix) || value.startsWith(prefix + " ") || value.startsWith(prefix + "-")) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
