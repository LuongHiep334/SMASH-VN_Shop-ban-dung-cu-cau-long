package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.smashvn.shop.entity.SanPhamChiTiet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoHangDTO {
    private String maLo;
    private LocalDateTime ngayTao;
    private String thoiGianHienThi;
    private boolean isLegacyLot;
    private Integer tongSoBienThe;
    private Integer soBienTheConHang;
    private Integer tongSoLuongTonHienTai;
    private BigDecimal tongGiaTriVonHienTai; // sum(soLuongTon * giaNhap)
    private boolean coGiaNhapUncertain;
    private List<SanPhamChiTiet> danhSachSpctInLot;
}
