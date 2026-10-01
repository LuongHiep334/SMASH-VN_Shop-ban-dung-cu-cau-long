package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BienTheImportSummaryDTO {
    private Integer tonKhoHienTai;
    private BigDecimal giaVonBinhQuan;
    private BigDecimal giaBanHienTai;
    private Long tongSoLuongTungNhap;
    private Long soLanNhapHang;
}
