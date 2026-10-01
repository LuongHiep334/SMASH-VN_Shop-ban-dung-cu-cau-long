package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaKhoiPhuc")
@Data
public class TokenKhoiPhuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ma_xac_nhan", nullable = false)
    private String maXacNhan;

    @Column(name = "loai_xac_nhan", nullable = false, length = 20, columnDefinition = "nvarchar(20)")
    private String loaiXacNhan;

    @Column(name = "thoi_gian_het_han", nullable = false)
    private LocalDateTime thoiGianHetHan;

    @Column(name = "da_su_dung", nullable = false)
    private boolean daSuDung;
}