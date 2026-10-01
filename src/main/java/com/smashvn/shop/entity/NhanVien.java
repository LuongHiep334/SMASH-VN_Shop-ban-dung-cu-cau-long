package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "NhanVien")
@Data
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_tai_khoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTenNv;

    @Column(name = "chuc_vu", nullable = false, length = 100)
    private String chucVu;

    @Column(name = "so_dien_thoai_nv", nullable = false, length = 15)
    private String soDienThoaiNv;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat;

    public String getHoTen() {
        return hoTenNv;
    }

    public void setHoTen(String hoTen) {
        this.hoTenNv = hoTen;
    }
}
