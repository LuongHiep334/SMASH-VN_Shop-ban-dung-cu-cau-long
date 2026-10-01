package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HinhAnhSanPham")
@Data
public class HinhAnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "url_hinh_anh", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @Column(name = "la_anh_chinh")
    private Boolean laAnhChinh = false;

    @Column(name = "thu_tu")
    private Integer thuTu;
}
