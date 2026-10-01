package com.smashvn.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TaiKhoan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "mat_khau", nullable = true)
    private String matKhau;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tai_khoan", nullable = false, length = 20)
    private AccountStatus trangThaiTaiKhoan = AccountStatus.ACTIVE;

    @Column(name = "so_lan_mua_thanh_cong", nullable = false)
    private Integer soLanMuaThanhCong = 0;

    @Column(name = "vai_tro", nullable = false, length = 10)
    private String vaiTro;

    @Column(name = "ma_xac_thuc_khoa", length = 100)
    private String maXacThucKhoa;

    @Column(name = "so_lan_nhac_nho_vi_pham", nullable = false)
    private Integer soLanNhacNhoViPham = 0;

    @Column(name = "thoi_han_mo_khoa")
    private java.time.LocalDateTime thoiHanMoKhoa;

    @Column(name = "ngay_vi_pham_gan_nhat")
    private java.time.LocalDateTime ngayViPhamGanNhat;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat;

    public String getTrangThai() {
        if (trangThaiTaiKhoan == AccountStatus.PENDING_LOCK) {
            return "cho_khoa";
        }
        if (trangThaiTaiKhoan == AccountStatus.LOCKED) {
            return "bi_khoa";
        }
        if (trangThaiTaiKhoan == AccountStatus.GUEST) {
            return "khach_vang_lai";
        }
        return "hoat_dong";
    }

    public void setTrangThai(String trangThai) {
        if ("cho_khoa".equals(trangThai)) {
            this.trangThaiTaiKhoan = AccountStatus.PENDING_LOCK;
        } else if ("bi_khoa".equals(trangThai)) {
            this.trangThaiTaiKhoan = AccountStatus.LOCKED;
        } else if ("khach_vang_lai".equals(trangThai) || "guest".equalsIgnoreCase(String.valueOf(trangThai))) {
            this.trangThaiTaiKhoan = AccountStatus.GUEST;
        } else {
            this.trangThaiTaiKhoan = AccountStatus.ACTIVE;
        }
    }

    public String getTokenXacThucKhoa() {
        return maXacThucKhoa;
    }

    public void setTokenXacThucKhoa(String tokenXacThucKhoa) {
        this.maXacThucKhoa = tokenXacThucKhoa;
    }

    public java.time.LocalDateTime getNgayKhoaBinhLuanDen() {
        return thoiHanMoKhoa;
    }

    public void setNgayKhoaBinhLuanDen(java.time.LocalDateTime ngayKhoaBinhLuanDen) {
        this.thoiHanMoKhoa = ngayKhoaBinhLuanDen;
    }
}
