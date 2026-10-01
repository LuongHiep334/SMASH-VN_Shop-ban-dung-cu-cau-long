package com.smashvn.shop.service.product;

import java.math.BigDecimal;

public record PriceSnapshot(
    BigDecimal giaNiemYet,
    BigDecimal giaBanSauGiam,
    BigDecimal phanTramGiam,
    BigDecimal soTienGiamSanPham,
    String tenDotGiamGia,
    Integer idDotGiamGia
) {}
