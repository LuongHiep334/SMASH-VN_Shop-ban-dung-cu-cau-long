package com.smashvn.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPhamChiTietThuocTinh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "sanPhamChiTiet")
public class SanPhamChiTietThuocTinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false)
    @JsonIgnore
    private SanPhamChiTiet sanPhamChiTiet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_thuoc_tinh", nullable = false)
    @EqualsAndHashCode.Include
    private ThuocTinh thuocTinh;

    @Column(name = "gia_tri", nullable = false, length = 500)
    @EqualsAndHashCode.Include
    private String giaTri;
}
