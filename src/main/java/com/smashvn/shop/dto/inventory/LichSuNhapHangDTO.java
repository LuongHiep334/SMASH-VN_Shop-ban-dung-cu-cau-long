package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuNhapHangDTO {
    private Integer id;
    private Integer idPhieuNhap;
    private String maPhieuNhap;
    private Integer idSpct;
    private String phanLoaiHienThi;
    private LocalDateTime thoiGianNhap;
    private String thoiGianHienThi;
    private Integer soLuongNhap;
    private Integer tonCu;
    private Integer tonMoi;
    private BigDecimal giaNhap;
    private String nguoiThucHien;
    private String ghiChu;

    public BigDecimal getThanhTien() {
        if (giaNhap != null && soLuongNhap != null) {
            return giaNhap.multiply(BigDecimal.valueOf(soLuongNhap.longValue()));
        }
        return BigDecimal.ZERO;
    }
}
