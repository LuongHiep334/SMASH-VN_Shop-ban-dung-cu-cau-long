package com.smashvn.shop.dto.chatbot;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionDto {
    private Integer id;
    private String tenSanPham;
    private String thuongHieu;
    private String mauSac;
    private String trongLuong;
    private String phanLoai;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private String hinhAnh;
    private String duongDan;
}
