package com.smashvn.shop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "SanPhamChiTiet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"sanPham", "hinhAnhSanPhams", "sanPhamChiTietThuocTinhs"})
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "sku", length = 40)
    private String sku;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong_ton", nullable = false)
    @Builder.Default
    private Integer soLuongTon = 0;

    @Column(name = "so_luong_sp_loi", nullable = false)
    @Builder.Default
    private Integer soLuongSpLoi = 0;

    @Column(name = "gia_nhap")
    private BigDecimal giaNhap;

    @Column(name = "ngay_tao")
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat")
    @Builder.Default
    private LocalDateTime ngayCapNhat = LocalDateTime.now();

    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private Boolean trangThaiValue = true;

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SELECT)
    @jakarta.persistence.OrderBy("laAnhChinh DESC, thuTu ASC, id ASC")
    @Builder.Default
    private List<HinhAnhSanPham> hinhAnhSanPhams = new ArrayList<>();

    @OneToMany(mappedBy = "sanPhamChiTiet", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 30)
    @Builder.Default
    private java.util.Set<SanPhamChiTietThuocTinh> sanPhamChiTietThuocTinhs = new java.util.LinkedHashSet<>();

    @Transient
    private String hinhAnhSanPham;

    public Integer getId() {
        return id;
    }

    public Integer getIdSanPhamChiTiet() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SanPham getSanPham() {
        return sanPham;
    }

    public void setSanPham(SanPham sanPham) {
        this.sanPham = sanPham;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public Integer getSoLuongTon() {
        return soLuongTon != null ? soLuongTon : 0;
    }

    public void setSoLuongTon(Integer soLuongTon) {
        this.soLuongTon = soLuongTon;
    }

    public Integer getSoLuongSpLoi() {
        return soLuongSpLoi != null ? soLuongSpLoi : 0;
    }

    public void setSoLuongSpLoi(Integer soLuongSpLoi) {
        this.soLuongSpLoi = soLuongSpLoi;
    }

    public BigDecimal getGiaNhap() {
        return giaNhap;
    }

    public void setGiaNhap(BigDecimal giaNhap) {
        this.giaNhap = giaNhap;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public Boolean getTrangThaiValue() {
        return trangThaiValue;
    }

    public void setTrangThaiValue(Boolean trangThaiValue) {
        this.trangThaiValue = trangThaiValue;
    }

    public List<HinhAnhSanPham> getHinhAnhSanPhams() {
        return hinhAnhSanPhams;
    }

    public void setHinhAnhSanPhams(List<HinhAnhSanPham> hinhAnhSanPhams) {
        this.hinhAnhSanPhams = hinhAnhSanPhams;
    }

    public java.util.Set<SanPhamChiTietThuocTinh> getSanPhamChiTietThuocTinhs() {
        return sanPhamChiTietThuocTinhs;
    }

    public void setSanPhamChiTietThuocTinhs(java.util.Set<SanPhamChiTietThuocTinh> sanPhamChiTietThuocTinhs) {
        this.sanPhamChiTietThuocTinhs = sanPhamChiTietThuocTinhs;
    }

    public String getHinhAnhSanPham() {
        if (hinhAnhSanPhams != null && !hinhAnhSanPhams.isEmpty()) {
            HinhAnhSanPham mainImg = hinhAnhSanPhams.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getLaAnhChinh()))
                    .findFirst()
                    .orElseGet(() -> hinhAnhSanPhams.stream()
                    .filter(img -> img.getThuTu() != null)
                    .min(java.util.Comparator.comparing(HinhAnhSanPham::getThuTu))
                    .orElse(hinhAnhSanPhams.get(0)));
            if (mainImg != null && mainImg.getUrlHinhAnh() != null) {
                return mainImg.getUrlHinhAnh();
            }
        }
        return hinhAnhSanPham;
    }

    public String getHinhAnhUrl() {
        String img = getHinhAnhSanPham();
        if (img == null || img.isBlank()) {
            return "/images/placeholder.png";
        }
        img = img.trim();
        if (img.startsWith("/")) {
            return img;
        }
        if (img.startsWith("uploads/")) {
            return "/" + img;
        }
        return "/uploads/product/" + img;
    }

    public void setHinhAnhSanPham(String hinhAnhSanPham) {
        this.hinhAnhSanPham = hinhAnhSanPham;
        if (hinhAnhSanPham != null && !hinhAnhSanPham.isEmpty()) {
            if (this.hinhAnhSanPhams == null) {
                this.hinhAnhSanPhams = new ArrayList<>();
            } else {
                this.hinhAnhSanPhams.clear();
            }
            HinhAnhSanPham hasp = new HinhAnhSanPham();
            hasp.setSanPhamChiTiet(this);
            hasp.setUrlHinhAnh(hinhAnhSanPham);
            hasp.setMauSac(getMauSac());
            this.hinhAnhSanPhams.add(hasp);
        }
    }

    /**
     * Dynamic attribute getters (reads from sanPhamChiTietThuocTinhs list)
     */
    public String getGiaTriThuocTinh(String tenThuocTinh) {
        if (sanPhamChiTietThuocTinhs == null || tenThuocTinh == null) {
            return null;
        }
        String val = sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && tenThuocTinh.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()))
                .map(SanPhamChiTietThuocTinh::getGiaTri)
                .findFirst()
                .orElse(null);
        if (val != null) {
            return val;
        }

        if ("Kích thước".equalsIgnoreCase(tenThuocTinh) || "Size".equalsIgnoreCase(tenThuocTinh)) {
            return sanPhamChiTietThuocTinhs.stream()
                    .filter(tt -> tt.getThuocTinh() != null && ("Kích thước".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()) || "Size".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh())))
                    .map(SanPhamChiTietThuocTinh::getGiaTri)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public String getMauSac() {
        return getGiaTriThuocTinh("Màu sắc");
    }

    public String getKichThuoc() {
        return getGiaTriThuocTinh("Kích thước");
    }

    public String getTrongLuong() {
        return getGiaTriThuocTinh("Trọng lượng");
    }

    public String getSucCang() {
        return getGiaTriThuocTinh("Sức căng");
    }

    public String getChatLieu() {
        return getGiaTriThuocTinh("Chất liệu");
    }

    public String getMucCang() {
        return getSucCang();
    }

    public void setMauSac(String mauSac) {
        setGiaTriThuocTinhHelper("Màu sắc", mauSac);
    }

    public void setKichThuoc(String kichThuoc) {
        setGiaTriThuocTinhHelper("Kích thước", kichThuoc);
    }

    public void setTrongLuong(String trongLuong) {
        setGiaTriThuocTinhHelper("Trọng lượng", trongLuong);
    }

    public void setSucCang(String sucCang) {
        setGiaTriThuocTinhHelper("Sức căng", sucCang);
    }

    public void setMucCang(String mucCang) {
        setGiaTriThuocTinhHelper("Sức căng", mucCang);
    }

    public void setChatLieu(String chatLieu) {
        setGiaTriThuocTinhHelper("Chất liệu", chatLieu);
    }

    private void setGiaTriThuocTinhHelper(String tenThuocTinh, String giaTri) {
        if (this.sanPhamChiTietThuocTinhs == null) {
            this.sanPhamChiTietThuocTinhs = new java.util.LinkedHashSet<>();
        }

        String targetName = tenThuocTinh;
        if (this.sanPham != null && this.sanPham.getDanhMuc() != null && this.sanPham.getDanhMuc().getThuocTinhList() != null) {
            for (ThuocTinh catAtt : this.sanPham.getDanhMuc().getThuocTinhList()) {
                if (catAtt.getTenThuocTinh() != null) {
                    if (catAtt.getTenThuocTinh().equalsIgnoreCase(tenThuocTinh)) {
                        targetName = catAtt.getTenThuocTinh();
                        break;
                    } else if (("Kích thước".equalsIgnoreCase(tenThuocTinh) || "Size".equalsIgnoreCase(tenThuocTinh))
                            && ("Kích thước".equalsIgnoreCase(catAtt.getTenThuocTinh()) || "Size".equalsIgnoreCase(catAtt.getTenThuocTinh()))) {
                        targetName = catAtt.getTenThuocTinh();
                        break;
                    }
                }
            }
        }

        final String finalTargetName = targetName;
        SanPhamChiTietThuocTinh existing = this.sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && (finalTargetName.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh())
                || (("Kích thước".equalsIgnoreCase(finalTargetName) || "Size".equalsIgnoreCase(finalTargetName))
                && ("Kích thước".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()) || "Size".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh())))))
                .findFirst()
                .orElse(null);

        if (giaTri == null || giaTri.isBlank()) {
            if (existing != null) {
                this.sanPhamChiTietThuocTinhs.remove(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setGiaTri(giaTri.trim());
        } else {
            ThuocTinh tt = null;
            try {
                com.smashvn.shop.repository.ThuocTinhRepository repo = com.smashvn.shop.config.SpringContextHelper.getBean(com.smashvn.shop.repository.ThuocTinhRepository.class);
                if (repo != null) {
                    tt = repo.findByTenThuocTinhIgnoreCase(finalTargetName.trim())
                            .orElseGet(() -> repo.save(ThuocTinh.builder()
                            .tenThuocTinh(finalTargetName.trim())
                            .trangThai(true)
                            .build()));
                }
            } catch (Exception ignored) {
            }

            if (tt == null) {
                tt = ThuocTinh.builder()
                        .tenThuocTinh(finalTargetName.trim())
                        .trangThai(true)
                        .build();
            }

            SanPhamChiTietThuocTinh val = SanPhamChiTietThuocTinh.builder()
                    .sanPhamChiTiet(this)
                    .thuocTinh(tt)
                    .giaTri(giaTri.trim())
                    .build();
            this.sanPhamChiTietThuocTinhs.add(val);
        }
    }

    public String getDisplayNameForAttribute(SanPhamChiTietThuocTinh tt) {
        if (tt == null || tt.getThuocTinh() == null) {
            return "";
        }
        String dbName = tt.getThuocTinh().getTenThuocTinh();
        if (dbName == null) {
            return "";
        }

        if (this.sanPham != null && this.sanPham.getDanhMuc() != null && this.sanPham.getDanhMuc().getThuocTinhList() != null) {
            for (ThuocTinh catAtt : this.sanPham.getDanhMuc().getThuocTinhList()) {
                if (catAtt.getTenThuocTinh() != null) {
                    if (catAtt.getTenThuocTinh().equalsIgnoreCase(dbName)) {
                        return catAtt.getTenThuocTinh();
                    }
                    if (("Kích thước".equalsIgnoreCase(dbName) || "Size".equalsIgnoreCase(dbName))
                            && ("Kích thước".equalsIgnoreCase(catAtt.getTenThuocTinh()) || "Size".equalsIgnoreCase(catAtt.getTenThuocTinh()))) {
                        return catAtt.getTenThuocTinh();
                    }
                }
            }
        }
        return dbName;
    }

    public String getPhanLoaiHienThi() {
        if (sanPhamChiTietThuocTinhs == null || sanPhamChiTietThuocTinhs.isEmpty()) {
            return "Mặc định";
        }
        com.smashvn.shop.constant.CategoryType catType = (sanPham != null && sanPham.getDanhMuc() != null)
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(sanPham.getDanhMuc())
                : com.smashvn.shop.constant.CategoryType.OTHER;

        List<SanPhamChiTietThuocTinh> validAttrs = sanPhamChiTietThuocTinhs.stream()
                .filter(tt -> tt.getThuocTinh() != null && tt.getGiaTri() != null && !tt.getGiaTri().isBlank())
                .filter(tt -> {
                    String tenTT = tt.getThuocTinh().getTenThuocTinh().trim().toLowerCase();
                    if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC || catType == com.smashvn.shop.constant.CategoryType.GIAY) {
                        if (tenTT.contains("căng") || tenTT.contains("trọng lượng") || tenTT.contains("weight")) {
                            return false;
                        }
                    } else if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        if (tenTT.contains("kích thước") || tenTT.contains("size")) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (validAttrs.isEmpty()) {
            return "Mặc định";
        }

        return validAttrs.stream()
                .map(tt -> getDisplayNameForAttribute(tt) + ": " + tt.getGiaTri())
                .collect(Collectors.joining(", "));
    }

    public void setTrangThai(String trangThai) {
        this.trangThaiValue = !"ngung_ban".equalsIgnoreCase(String.valueOf(trangThai))
                && !"ngung_kinh_doanh".equalsIgnoreCase(String.valueOf(trangThai))
                && !"false".equalsIgnoreCase(String.valueOf(trangThai));
    }

    public String getTrangThai() {
        return Boolean.FALSE.equals(trangThaiValue) ? "ngung_kinh_doanh" : "dang_ban";
    }

    public String getThuocTinhMapJson() {
        if (sanPhamChiTietThuocTinhs == null || sanPhamChiTietThuocTinhs.isEmpty()) {
            return "{}";
        }
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (SanPhamChiTietThuocTinh tt : sanPhamChiTietThuocTinhs) {
            if (tt.getThuocTinh() != null && tt.getGiaTri() != null && !tt.getGiaTri().isBlank()) {
                String displayName = getDisplayNameForAttribute(tt);
                map.put(displayName.trim(), tt.getGiaTri().trim());
            }
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    public boolean isDangBan() {
        return !Boolean.FALSE.equals(trangThaiValue);
    }
}
