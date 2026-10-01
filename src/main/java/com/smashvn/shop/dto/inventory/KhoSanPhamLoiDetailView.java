package com.smashvn.shop.dto.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel dai dien cho toan bo trang Chi Tiet Kho San Pham Loi cua 1 bien the.
 */
public class KhoSanPhamLoiDetailView {

    private Integer idSanPhamChiTiet;
    private Integer idSanPham;
    private String tenSanPham;
    private String phanLoaiHienThi;
    private String hinhAnhUrl;
    private Integer soLuongTon;
    private Integer soLuongSpLoi;
    private List<KhoSanPhamLoiSourceView> lichSuChuyenKhoLoi = new ArrayList<>();
    private List<KhoSanPhamLoiLichSuXuLyView> lichSuXuLyKhoLoi = new ArrayList<>();

    public KhoSanPhamLoiDetailView() {}

    public KhoSanPhamLoiDetailView(Integer idSanPhamChiTiet, Integer idSanPham,
                                   String tenSanPham, String phanLoaiHienThi,
                                   String hinhAnhUrl, Integer soLuongTon, Integer soLuongSpLoi,
                                   List<KhoSanPhamLoiSourceView> lichSuChuyenKhoLoi) {
        this(idSanPhamChiTiet, idSanPham, tenSanPham, phanLoaiHienThi, hinhAnhUrl, soLuongTon, soLuongSpLoi, lichSuChuyenKhoLoi, new ArrayList<>());
    }

    public KhoSanPhamLoiDetailView(Integer idSanPhamChiTiet, Integer idSanPham,
                                   String tenSanPham, String phanLoaiHienThi,
                                   String hinhAnhUrl, Integer soLuongTon, Integer soLuongSpLoi,
                                   List<KhoSanPhamLoiSourceView> lichSuChuyenKhoLoi,
                                   List<KhoSanPhamLoiLichSuXuLyView> lichSuXuLyKhoLoi) {
        this.idSanPhamChiTiet = idSanPhamChiTiet;
        this.idSanPham = idSanPham;
        this.tenSanPham = tenSanPham;
        this.phanLoaiHienThi = phanLoaiHienThi;
        this.hinhAnhUrl = hinhAnhUrl;
        this.soLuongTon = soLuongTon != null ? soLuongTon : 0;
        this.soLuongSpLoi = soLuongSpLoi != null ? soLuongSpLoi : 0;
        this.lichSuChuyenKhoLoi = lichSuChuyenKhoLoi != null ? lichSuChuyenKhoLoi : new ArrayList<>();
        this.lichSuXuLyKhoLoi = lichSuXuLyKhoLoi != null ? lichSuXuLyKhoLoi : new ArrayList<>();
    }

    public Integer getIdSanPhamChiTiet() { return idSanPhamChiTiet; }
    public void setIdSanPhamChiTiet(Integer idSanPhamChiTiet) { this.idSanPhamChiTiet = idSanPhamChiTiet; }

    public Integer getIdSanPham() { return idSanPham; }
    public void setIdSanPham(Integer idSanPham) { this.idSanPham = idSanPham; }

    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }

    public String getPhanLoaiHienThi() { return phanLoaiHienThi; }
    public void setPhanLoaiHienThi(String phanLoaiHienThi) { this.phanLoaiHienThi = phanLoaiHienThi; }

    public String getHinhAnhUrl() { return hinhAnhUrl; }
    public void setHinhAnhUrl(String hinhAnhUrl) { this.hinhAnhUrl = hinhAnhUrl; }

    public Integer getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(Integer soLuongTon) { this.soLuongTon = soLuongTon != null ? soLuongTon : 0; }

    public Integer getSoLuongSpLoi() { return soLuongSpLoi; }
    public void setSoLuongSpLoi(Integer soLuongSpLoi) { this.soLuongSpLoi = soLuongSpLoi != null ? soLuongSpLoi : 0; }

    public List<KhoSanPhamLoiSourceView> getLichSuChuyenKhoLoi() { return lichSuChuyenKhoLoi; }
    public void setLichSuChuyenKhoLoi(List<KhoSanPhamLoiSourceView> lichSuChuyenKhoLoi) {
        this.lichSuChuyenKhoLoi = lichSuChuyenKhoLoi != null ? lichSuChuyenKhoLoi : new ArrayList<>();
    }

    public List<KhoSanPhamLoiLichSuXuLyView> getLichSuXuLyKhoLoi() { return lichSuXuLyKhoLoi; }
    public void setLichSuXuLyKhoLoi(List<KhoSanPhamLoiLichSuXuLyView> lichSuXuLyKhoLoi) {
        this.lichSuXuLyKhoLoi = lichSuXuLyKhoLoi != null ? lichSuXuLyKhoLoi : new ArrayList<>();
    }
}