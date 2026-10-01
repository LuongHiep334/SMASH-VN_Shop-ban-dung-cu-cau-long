package com.smashvn.shop.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPhamYeuThich")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamYeuThich {
    @EmbeddedId
    private SanPhamYeuThichKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("khachHangId")
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sanPhamId")
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "ngay_them", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime ngayThem = LocalDateTime.now();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamYeuThichKey implements Serializable {
        private Integer khachHangId;
        private Integer sanPhamId;
    }
}
