package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GioHang")
@Data
public class GioHang {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	
	@ManyToOne
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @Column(name = "ngay_tao")
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat = java.time.LocalDateTime.now();
}
