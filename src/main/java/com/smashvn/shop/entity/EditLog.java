package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "EditLog")
@Data
public class EditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", nullable = true)
    private TaiKhoan taiKhoan;

    @Column(name = "ten_bang", nullable = false, length = 100)
    private String tenBang;

    @Column(name = "id_ban_ghi", nullable = false)
    private Integer idBanGhi;

    @Column(name = "hanh_dong", nullable = false, length = 20)
    private String hanhDong; // 'INSERT', 'UPDATE', 'DELETE'

    @Column(name = "gia_tri_cu", columnDefinition = "NVARCHAR(MAX)")
    private String giaTriCu;

    @Column(name = "gia_tri_moi", columnDefinition = "NVARCHAR(MAX)")
    private String giaTriMoi;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();

    @Column(name = "dia_chi_ip", length = 50)
    private String diaChiIp;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "vai_tro_thuc_hien", length = 20)
    private String vaiTroThucHien;
}
