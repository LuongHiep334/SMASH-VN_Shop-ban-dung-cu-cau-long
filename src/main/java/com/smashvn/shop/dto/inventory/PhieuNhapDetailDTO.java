package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuNhapDetailDTO {
    private Integer id;
    private String maPhieuNhap;
    private LocalDateTime ngayNhap;
    private String ngayNhapHienThi;
    private String tenNhanVien;
    private String ghiChu;
    private BigDecimal tongTien;
    private List<ItemDetail> chiTietList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDetail {
        private Integer idSpct;
        private String tenSanPham;
        private String phanLoaiHienThi;
        private Integer soLuong;
        private BigDecimal giaNhap;
        private BigDecimal thanhTien;
    }
}
