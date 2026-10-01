package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SoDiaChi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoDiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ho_va_ten_nguoi_nhan", length = 100)
    private String hoVaTenNguoiNhan;

    @Column(name = "sdt_nguoi_nhan", nullable = false, length = 15)
    private String sdtNguoiNhan;

    @Column(name = "dia_chi_cu_the", nullable = false, length = 255)
    private String diaChiCuThe;

    @Column(name = "tinh_thanh", length = 100)
    private String tinhThanh;

    @Column(name = "quan_huyen", length = 100)
    private String quanHuyen;

    @Column(name = "phuong_xa", length = 100)
    private String phuongXa;

    @Column(name = "ghn_province_id")
    private Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 50)
    private String ghnWardCode;

    @Column(name = "dia_chi_mac_dinh", nullable = false)
    private boolean diaChiMacDinh = false;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private java.time.LocalDateTime ngayTao = java.time.LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime ngayCapNhat;

    @Transient
    private String quocGia = "Việt Nam";

    @Transient
    private String maBuuDien;

    @Transient
    private Double latitude;

    @Transient
    private Double longitude;

    @Transient
    private String hoNguoiNhan;
    @Transient
    private String tenNguoiNhan;

    public String getHoNguoiNhan() {
        if (this.hoNguoiNhan != null) return this.hoNguoiNhan;
        String[] parts = splitRecipientName();
        return parts[0];
    }

    public void setHoNguoiNhan(String hoNguoiNhan) {
        this.hoNguoiNhan = hoNguoiNhan;
        updateHoVaTenNguoiNhan();
    }

    public String getTenNguoiNhan() {
        if (this.tenNguoiNhan != null) return this.tenNguoiNhan;
        String[] parts = splitRecipientName();
        return parts[1];
    }

    public void setTenNguoiNhan(String tenNguoiNhan) {
        this.tenNguoiNhan = tenNguoiNhan;
        updateHoVaTenNguoiNhan();
    }

    private void updateHoVaTenNguoiNhan() {
        String h = this.hoNguoiNhan != null ? this.hoNguoiNhan : getHoNguoiNhan();
        String t = this.tenNguoiNhan != null ? this.tenNguoiNhan : getTenNguoiNhan();
        this.hoVaTenNguoiNhan = joinName(h, t);
    }

    public String getThanhPho() {
        return tinhThanh;
    }

    public void setThanhPho(String thanhPho) {
        this.tinhThanh = thanhPho;
    }

    public Integer getProvinceId() {
        return ghnProvinceId;
    }

    public void setProvinceId(Integer provinceId) {
        this.ghnProvinceId = provinceId;
    }

    public Integer getDistrictId() {
        return ghnDistrictId;
    }

    public void setDistrictId(Integer districtId) {
        this.ghnDistrictId = districtId;
    }

    public String getWardCode() {
        return ghnWardCode;
    }

    public void setWardCode(String wardCode) {
        this.ghnWardCode = wardCode;
    }

    public String getProvinceName() {
        return tinhThanh;
    }

    public void setProvinceName(String provinceName) {
        this.tinhThanh = provinceName;
    }

    public String getDistrictName() {
        return quanHuyen;
    }

    public void setDistrictName(String districtName) {
        this.quanHuyen = districtName;
    }

    public String getWardName() {
        return phuongXa;
    }

    public void setWardName(String wardName) {
        this.phuongXa = wardName;
    }

    public boolean isDefaultShipping() {
        return diaChiMacDinh;
    }

    public void setDefaultShipping(boolean defaultShipping) {
        this.diaChiMacDinh = defaultShipping;
    }

    public boolean isDefaultBilling() {
        return diaChiMacDinh;
    }

    public void setDefaultBilling(boolean defaultBilling) {
        this.diaChiMacDinh = defaultBilling;
    }

    private String[] splitRecipientName() {
        String fullName = hoVaTenNguoiNhan == null ? "" : hoVaTenNguoiNhan.trim();
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
