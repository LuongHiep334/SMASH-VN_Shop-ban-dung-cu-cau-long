package com.smashvn.shop.entity;


import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SanPham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"danhMuc", "thuongHieu", "nhanVien", "cacDotGiamGia", "sanPhamChiTiets"})
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne
    @JoinColumn(name = "id_thuong_hieu", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "ma_san_pham", length = 20)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenSanPham;
    
    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private Boolean trangThaiValue = true;

    public String getTrangThai() {
        return Boolean.FALSE.equals(trangThaiValue) ? "ngung_kinh_doanh" : "dang_ban";
    }

    public void setTrangThai(String status) {
        this.trangThaiValue = !"ngung_ban".equalsIgnoreCase(String.valueOf(status))
                && !"ngung_kinh_doanh".equalsIgnoreCase(String.valueOf(status))
                && !"false".equalsIgnoreCase(String.valueOf(status));
    }
    @Column(name = "mo_ta", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @ManyToMany(mappedBy = "sanPhams")
    @org.hibernate.annotations.BatchSize(size = 30)
    private Set<DotGiamGia> cacDotGiamGia;
    
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 30)
    private List<SanPhamChiTiet> sanPhamChiTiets;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DanhMuc getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(DanhMuc danhMuc) {
        this.danhMuc = danhMuc;
    }

    public ThuongHieu getThuongHieu() {
        return thuongHieu;
    }

    public void setThuongHieu(ThuongHieu thuongHieu) {
        this.thuongHieu = thuongHieu;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public Boolean getTrangThaiValue() {
        return trangThaiValue;
    }

    public void setTrangThaiValue(Boolean trangThaiValue) {
        this.trangThaiValue = trangThaiValue;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Set<DotGiamGia> getCacDotGiamGia() {
        return cacDotGiamGia;
    }

    public void setCacDotGiamGia(Set<DotGiamGia> cacDotGiamGia) {
        this.cacDotGiamGia = cacDotGiamGia;
    }

    public List<SanPhamChiTiet> getSanPhamChiTiets() {
        return sanPhamChiTiets;
    }

    public void setSanPhamChiTiets(List<SanPhamChiTiet> sanPhamChiTiets) {
        this.sanPhamChiTiets = sanPhamChiTiets;
    }

    public int getTongSoLuongTon() {
        if (!org.hibernate.Hibernate.isInitialized(sanPhamChiTiets) || sanPhamChiTiets == null || sanPhamChiTiets.isEmpty()) {
            return 0;
        }
        return sanPhamChiTiets.stream()
                .mapToInt(spct -> spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                .sum();
    }

    public int getTongSoLuongTonDangBan() {
        if (!org.hibernate.Hibernate.isInitialized(sanPhamChiTiets) || sanPhamChiTiets == null || sanPhamChiTiets.isEmpty()) {
            return 0;
        }
        return sanPhamChiTiets.stream()
                .filter(spct -> spct.getTrangThai() == null || spct.getTrangThai().isBlank() || "dang_ban".equals(spct.getTrangThai()))
                .mapToInt(spct -> spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                .sum();
    }

    public Integer getActiveGiamGiaPhanTram() {
        if (!org.hibernate.Hibernate.isInitialized(cacDotGiamGia) || cacDotGiamGia == null || cacDotGiamGia.isEmpty()) {
            return 0;
        }
        return cacDotGiamGia.stream()
                .filter(dgg -> dgg.getActive() && "ACTIVE".equals(dgg.getDynamicStatus()))
                .mapToInt(DotGiamGia::getPhanTramGiam)
                .max()
                .orElse(0);
    }

    public java.math.BigDecimal getGiaSauGiam(java.math.BigDecimal giaGoc) {
        int phanTram = getActiveGiamGiaPhanTram();
        if (phanTram <= 0) {
            return giaGoc;
        }
        return giaGoc.multiply(java.math.BigDecimal.valueOf(100 - phanTram))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    public String getActiveChienDichNgayKetThuc() {
        if (!org.hibernate.Hibernate.isInitialized(cacDotGiamGia) || cacDotGiamGia == null || cacDotGiamGia.isEmpty()) {
            return "2027/01/01 00:00:00";
        }
        return cacDotGiamGia.stream()
                .filter(dgg -> dgg.getActive() && "ACTIVE".equals(dgg.getDynamicStatus()))
                .map(DotGiamGia::getNgayKetThuc)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .map(ldt -> ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .orElse("2027/01/01 00:00:00");
    }

    public java.math.BigDecimal getMinGiaBan() {
        if (!org.hibernate.Hibernate.isInitialized(sanPhamChiTiets) || sanPhamChiTiets == null || sanPhamChiTiets.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return sanPhamChiTiets.stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);
    }

    public java.math.BigDecimal getMinGiaNhap() {
        if (sanPhamChiTiets == null || sanPhamChiTiets.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return sanPhamChiTiets.stream()
                .map(SanPhamChiTiet::getGiaNhap)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);
    }

    @Column(name = "so_luot_danh_gia", nullable = false)
    private Integer soDanhGia = 0;

    @Column(name = "diem_trung_binh", nullable = false)
    private Double diemTrungBinh = 0.0;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat = LocalDateTime.now();
}
