package com.smashvn.shop.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDonChiTiet")
@Data
public class HoaDonChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false)
    private BigDecimal donGia;

    @Column(name = "gia_goc")
    private BigDecimal giaGoc;

    @Column(name = "gia_sau_giam")
    private BigDecimal giaSauGiam;

    @Column(name = "ten_san_pham_snapshot", length = 255)
    private String tenSanPhamSnapshot;

    @Column(name = "sku_snapshot", length = 100)
    private String skuSnapshot;

    @Column(name = "ten_dot_giam_gia_snapshot", length = 255)
    private String tenDotGiamGiaSnapshot;

    @Column(name = "thuoc_tinh_snapshot", length = 500)
    private String thuocTinhSnapshot;

    @Column(name = "ngay_tao", nullable = false)
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    public String getTenDotGiamGia() {
        return tenDotGiamGiaSnapshot;
    }

    public BigDecimal getGiaNiemYet() {
        return giaGoc;
    }

    public Integer getPhanTramGiam() {
        if (giaGoc == null || donGia == null || giaGoc.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (giaGoc.compareTo(donGia) <= 0) {
            return 0;
        }
        BigDecimal traceDiff = giaGoc.subtract(donGia);
        BigDecimal percent = traceDiff.multiply(new BigDecimal("100")).divide(giaGoc, 0, java.math.RoundingMode.HALF_UP);
        return percent.intValue();
    }
}
