package com.smashvn.shop.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "KhachHang")
@Data
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_tai_khoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "ho_ten_kh", length = 100)
    private String hoTenKh;

    @Column(name = "so_dien_thoai_kh", length = 15)
    private String soDienThoaiKh;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat;

    @Transient
    private Boolean nhanBanTin = false;

    @Transient
    private Boolean laTaiKhoanNoiBo = false;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SoDiaChi> soDiaChis;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SanPhamYeuThich> sanPhamYeuThichs;

    @Transient
    private String hoKh;
    @Transient
    private String tenKh;

    public String getHoKh() {
        if (this.hoKh != null) {
            return this.hoKh;
        }
        String[] parts = splitHoTenKh();
        return parts[0];
    }

    public void setHoKh(String hoKh) {
        this.hoKh = hoKh;
        updateHoTenKh();
    }

    public String getTenKh() {
        if (this.tenKh != null) {
            return this.tenKh;
        }
        String[] parts = splitHoTenKh();
        return parts[1];
    }

    public void setTenKh(String tenKh) {
        this.tenKh = tenKh;
        updateHoTenKh();
    }

    private void updateHoTenKh() {
        String h = this.hoKh != null ? this.hoKh : getHoKh();
        String t = this.tenKh != null ? this.tenKh : getTenKh();
        this.hoTenKh = joinName(h, t);
    }

    public String getHo() {
        return getHoKh();
    }

    public void setHo(String ho) {
        setHoKh(ho);
    }

    public String getTen() {
        return getTenKh();
    }

    public void setTen(String ten) {
        setTenKh(ten);
    }

    private String[] splitHoTenKh() {
        String fullName = hoTenKh == null ? "" : hoTenKh.trim();
        if (fullName.isEmpty()) {
            return new String[]{"", ""};
        }
        int lastSpace = fullName.lastIndexOf(' ');
        if (lastSpace < 0) {
            return new String[]{"", fullName};
        }
        return new String[]{fullName.substring(0, lastSpace).trim(), fullName.substring(lastSpace + 1).trim()};
    }

    private String joinName(String ho, String ten) {
        String safeHo = ho == null ? "" : ho.trim();
        String safeTen = ten == null ? "" : ten.trim();
        return (safeHo + " " + safeTen).trim();
    }
}
