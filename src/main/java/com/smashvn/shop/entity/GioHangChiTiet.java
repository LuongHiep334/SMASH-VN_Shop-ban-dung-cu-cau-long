package com.smashvn.shop.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GioHangChiTiet")
@Data
public class GioHangChiTiet {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	
	@ManyToOne
    @JoinColumn(name = "id_gio_hang", nullable = false)
    private GioHang gioHang;
	
	@ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;
	
	@Column(name = "so_luong", nullable = false)
	private Integer soLuong;

	@Column(name = "ngay_tao")
	private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();
}

