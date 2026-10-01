package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HoaDon")
@EntityListeners(HoaDonEntityListener.class)
@Data
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia; // Có thể null

    @ManyToOne
    @JoinColumn(name = "id_don_vi_van_chuyen")
    private DonViVanChuyen donViVanChuyen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dia_chi")
    private SoDiaChi diaChi;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "tien_hang", nullable = false)
    private BigDecimal tongTienHang = BigDecimal.ZERO;

    @Column(name = "phi_van_chuyen", nullable = false)
    private BigDecimal phiVanChuyen = BigDecimal.ZERO;

    @Column(name = "so_tien_giam_gia", nullable = false)
    private BigDecimal soTienGiamVoucher = BigDecimal.ZERO;

    @Column(name = "tong_tien", nullable = false)
    private BigDecimal tongTien;

    @Column(name = "trang_thai_don_hang", nullable = false, length = 50)
    private String trangThaiDonHang = "CHO_XAC_NHAN";

    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 50)
    private String trangThaiThanhToan = "CHO_THANH_TOAN";

    @Column(name = "ten_nguoi_nhan", length = 100)
    private String tenNguoiNhan;

    @Column(name = "sdt_nhan", nullable = false, length = 15)
    private String sdtNhan;

    @Column(name = "email_nguoi_nhan", length = 255)
    private String emailNguoiNhan;

    @Column(name = "dia_chi_nhan", nullable = false, length = 500)
    private String diaChiNhan;

    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    @Column(name = "ly_do_hoan_tra", length = 500)
    private String lyDoHoanTra;

    @Deprecated
    public String getLyDoHoanTien() {
        return lyDoHoanTra;
    }

    @Deprecated
    public void setLyDoHoanTien(String lyDoHoanTien) {
        this.lyDoHoanTra = lyDoHoanTien;
    }

    @Transient
    private String maDonHang;

    public String getMaDonHang() {
        if (id == null) {
            return maDonHang;
        }
        String prefix = (nhanVien != null) ? "HDSVN" : "DHSVN";
        String dateStr = ngayTao != null ? ngayTao.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) : "20260708";
        return prefix + dateStr + "-" + id;
    }

    public void setMaDonHang(String maDonHang) {
        this.maDonHang = maDonHang;
    }

    @Transient
    private String paymentMethod;

    @Transient
    private String paymentStatus;

    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    @Transient
    private String transactionId;

    @Transient
    private String gatewayResponse;

    @org.hibernate.annotations.Formula("(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap IN ('GHN', 'GHN_FALLBACK') ORDER BY t.id DESC)")
    private String ghnOrderCode;

    @org.hibernate.annotations.Formula("(SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap IN ('GHN_RETURN', 'GHN_RETURN_FALLBACK') ORDER BY t.id DESC)")
    private String ghnReturnOrderCode;

    @org.hibernate.annotations.Formula("(SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = id AND t.nha_cung_cap IN ('GHN', 'GHN_FALLBACK') ORDER BY t.id DESC)")
    private String ghnStatus;

    @Transient
    private Integer ghnToDistrictId;

    @Transient
    private String ghnToWardCode;

    @Transient
    private String maVoucherApDung;

    @Transient
    private String tenVoucherApDung;

    @Transient
    private String moTaVoucherSnapshot;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "nguoi_xac_nhan_thanh_toan", length = 100)
    private String nguoiXacNhanThanhToan;

    @Column(name = "thoi_gian_xac_nhan")
    private LocalDateTime thoiGianXacNhan;

    @Column(name = "trang_thai_hoan_hang", length = 50)
    @Enumerated(EnumType.STRING)
    private ReturnStatus trangThaiHoanHang;

    @Column(name = "loai_yeu_cau_doi_tra", length = 20)
    private String loaiYeuCauDoiTra;

    @Column(name = "bang_chung_hoan_tra", columnDefinition = "NVARCHAR(MAX)")
    private String bangChungHoanTra;

    @Column(name = "trang_thai_xu_ly_hang_hoan", length = 50)
    @Enumerated(EnumType.STRING)
    private ReturnInventoryStatus trangThaiXuLyHangHoan;

    @Transient
    private LocalDateTime ngayXacNhanHoanHang;

    @Transient
    private NhanVien nhanVienXacNhan;

    @org.hibernate.annotations.Formula("CASE "
            + "WHEN trang_thai_thanh_toan IN ('REFUNDED', 'DA_HOAN_TIEN') OR trang_thai_hoan_hang = 'REFUNDED' THEN 'COMPLETED' "
            + "WHEN trang_thai_thanh_toan = 'CHO_HOAN_TIEN' OR (loai_yeu_cau_doi_tra = 'TRA' AND trang_thai_hoan_hang = 'RETURNED') THEN 'PENDING' "
            + "ELSE NULL END")
    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    @Transient
    private LocalDateTime refundTime;

    @Transient
    private NhanVien refundConfirmedBy;

    // Các trường đính kèm cho chứng từ và thông tin hoàn tiền
    @Transient
    private String phuongThucHoanTien;

    @Transient
    private BigDecimal soTienHoan;

    @Transient
    private String maGiaoDichHoanTien;

    @Transient
    private String ghiChuHoanTien;

    @Transient
    private String anhChungTuHoanTien;

    @Transient
    private LocalDateTime thoiGianHoanTien;

    @Transient
    private String nguoiThucHienHoanTien;

    @Transient
    private Boolean daNhapKhoHoan;

    // Getters overriding Lombok for NULL safety
    public BigDecimal getSoTienGiamVoucher() {
        return soTienGiamVoucher == null ? BigDecimal.ZERO : soTienGiamVoucher;
    }

    public BigDecimal getTongTienHang() {
        return tongTienHang == null ? BigDecimal.ZERO : tongTienHang;
    }

    public BigDecimal getTongTien() {
        return tongTien == null ? BigDecimal.ZERO : tongTien;
    }

    public BigDecimal getPhiVanChuyen() {
        return phiVanChuyen == null ? BigDecimal.ZERO : phiVanChuyen;
    }

    public String getPaymentMethod() {
        if (paymentMethod != null) {
            return paymentMethod.toLowerCase();
        }
        if (phuongThucThanhToan != null && phuongThucThanhToan.getTenPhuongThuc() != null) {
            return phuongThucThanhToan.getTenPhuongThuc().toLowerCase();
        }
        return null;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : trangThaiThanhToan;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        this.trangThaiThanhToan = paymentStatus;
    }

    public boolean isCod() {
        if (paymentMethod != null && ("cod".equalsIgnoreCase(paymentMethod) || "tien_mat".equalsIgnoreCase(paymentMethod))) {
            return true;
        }
        if (phuongThucThanhToan != null) {
            String ma = phuongThucThanhToan.getMaPhuongThuc();
            String ten = phuongThucThanhToan.getTenPhuongThuc();
            if (ma != null && ("cod".equalsIgnoreCase(ma) || "tien_mat".equalsIgnoreCase(ma))) {
                return true;
            }
            if (ten != null && (ten.toLowerCase().contains("cod") || ten.toLowerCase().contains("nhận hàng") || ten.toLowerCase().contains("tiền mặt"))) {
                return true;
            }
        }
        if (paymentMethod == null && phuongThucThanhToan == null) {
            return true;
        }
        return false;
    }

    public boolean isPrepaid() {
        return !isCod();
    }

    public LocalDateTime getPaidAt() {
        return ngayThanhToan;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.ngayThanhToan = paidAt;
    }

    @PrePersist
    @PreUpdate
    public void normalizeAmounts() {
        if (this.soTienGiamVoucher == null) {
            this.soTienGiamVoucher = BigDecimal.ZERO;
        }
        if (this.tongTienHang == null) {
            this.tongTienHang = BigDecimal.ZERO;
        }
        if (this.tongTien == null) {
            this.tongTien = BigDecimal.ZERO;
        }
        if (this.phiVanChuyen == null) {
            this.phiVanChuyen = BigDecimal.ZERO;
        }
    }
}
