package com.smashvn.shop.dto.cart;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemView {
    private Integer cartItemId;
    private Integer idSanPhamChiTiet;
    private Integer sanPhamId;
    private String tenSanPham;
    private String anhSanPham;
    private String danhMuc;
    private List<String> thuocTinh;
    private Integer soLuong;
    private Integer soLuongTon;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private boolean hetHang;
    private boolean ngungBan;
    private boolean hopLe;
    private boolean guestItem;
}
