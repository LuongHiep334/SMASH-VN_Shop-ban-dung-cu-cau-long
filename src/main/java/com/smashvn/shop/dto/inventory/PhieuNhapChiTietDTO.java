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
public class PhieuNhapChiTietDTO {
    private Integer id;
    private Integer idPhieuNhap;
    private String maPhieuNhap;
    private LocalDateTime ngayNhap;
    private String ngayNhapHienThi;
    private Integer idSpct;
    private String phanLoaiHienThi;
    private Integer soLuongNhap;
    private BigDecimal giaNhap;
    private BigDecimal thanhTien;
    private String tenNhanVien;
    private String ghiChu;
}
