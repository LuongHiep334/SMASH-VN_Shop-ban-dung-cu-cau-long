package com.smashvn.shop.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DanhGia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "so_sao", nullable = false)
    private Double soSao;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String binhLuan;

    @Builder.Default
    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayDanhGia = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    // Cờ kiểm duyệt ẩn/hiện độc lập
    @Builder.Default
    @Column(name = "binh_luan_an", nullable = false)
    private Boolean binhLuanAn = false;

    @Builder.Default
    @Column(name = "hinh_anh_an", nullable = false)
    private Boolean hinhAnhAn = false;

    // Cờ xóa mềm
    @Builder.Default
    @Column(name = "da_xoa", nullable = false)
    private Boolean daXoa = false;

    @Column(name = "ngay_xoa")
    private LocalDateTime ngayXoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nguoi_xoa")
    private TaiKhoan nguoiXoa;

    // Audit kiểm duyệt bình luận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private TaiKhoan nhanVien;

    @Column(name = "ngay_an_binh_luan")
    private LocalDateTime ngayAnBinhLuan;

    @Column(name = "ngay_hien_binh_luan")
    private LocalDateTime ngayHienBinhLuan;

    @Column(name = "ngay_an_hinh_anh")
    private LocalDateTime ngayAnHinhAnh;
    @Column(name = "ngay_hien_hinh_anh")
    private LocalDateTime ngayHienHinhAnh;

    // Liên kết đa ảnh
    @OneToMany(mappedBy = "danhGia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<HinhAnhDanhGia> danhSachAnh = new ArrayList<>();

    public Boolean getAnBinhLuan() {
        return binhLuanAn;
    }

    public void setAnBinhLuan(Boolean anBinhLuan) {
        this.binhLuanAn = anBinhLuan;
    }

    public Boolean getAnHinhAnh() {
        return hinhAnhAn;
    }

    public void setAnHinhAnh(Boolean anHinhAnh) {
        this.hinhAnhAn = anHinhAnh;
    }

    public void setNguoiAnBinhLuan(TaiKhoan nguoiAnBinhLuan) {
        this.nhanVien = nguoiAnBinhLuan;
    }

    public void setNguoiHienBinhLuan(TaiKhoan nguoiHienBinhLuan) {
        this.nhanVien = nguoiHienBinhLuan;
    }

    public void setNguoiAnHinhAnh(TaiKhoan nguoiAnHinhAnh) {
        this.nhanVien = nguoiAnHinhAnh;
    }

    public void setNguoiHienHinhAnh(TaiKhoan nguoiHienHinhAnh) {
        this.nhanVien = nguoiHienHinhAnh;
    }

    public TaiKhoan getNguoiAnBinhLuan() {
        return Boolean.TRUE.equals(binhLuanAn) ? nhanVien : null;
    }

    public TaiKhoan getNguoiHienBinhLuan() {
        return Boolean.FALSE.equals(binhLuanAn) && ngayHienBinhLuan != null ? nhanVien : null;
    }

    public TaiKhoan getNguoiAnHinhAnh() {
        return Boolean.TRUE.equals(hinhAnhAn) ? nhanVien : null;
    }

    public TaiKhoan getNguoiHienHinhAnh() {
        return Boolean.FALSE.equals(hinhAnhAn) && ngayHienHinhAnh != null ? nhanVien : null;
    }
}
