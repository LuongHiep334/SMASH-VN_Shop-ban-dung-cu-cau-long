package com.smashvn.shop.dto.inventory;

public class KhoSanPhamLoiView {

    private Integer idSanPhamChiTiet;
    private Integer idSanPham;
    private String tenSanPham;
    private String phanLoaiHienThi;
    private String hinhAnhUrl;
    private Integer soLuongTon;
    private Integer soLuongSpLoi;

    public KhoSanPhamLoiView() {}

    public KhoSanPhamLoiView(Integer idSanPhamChiTiet, Integer idSanPham,
                              String tenSanPham, String phanLoaiHienThi,
                              String hinhAnhUrl, Integer soLuongTon, Integer soLuongSpLoi) {
        this.idSanPhamChiTiet = idSanPhamChiTiet;
        this.idSanPham = idSanPham;
        this.tenSanPham = tenSanPham;
        this.phanLoaiHienThi = phanLoaiHienThi;
        this.hinhAnhUrl = hinhAnhUrl;
        this.soLuongTon = soLuongTon != null ? soLuongTon : 0;
        this.soLuongSpLoi = soLuongSpLoi != null ? soLuongSpLoi : 0;
    }

    public Integer getIdSanPhamChiTiet() { return idSanPhamChiTiet; }
    public Integer getIdSanPham()         { return idSanPham; }
    public String  getTenSanPham()        { return tenSanPham; }
    public String  getPhanLoaiHienThi()   { return phanLoaiHienThi; }
    public String  getHinhAnhUrl()        { return hinhAnhUrl; }
    public Integer getSoLuongTon()        { return soLuongTon; }
    public Integer getSoLuongSpLoi()      { return soLuongSpLoi; }

    public void setIdSanPhamChiTiet(Integer v) { this.idSanPhamChiTiet = v; }
    public void setIdSanPham(Integer v)         { this.idSanPham = v; }
    public void setTenSanPham(String v)         { this.tenSanPham = v; }
    public void setPhanLoaiHienThi(String v)    { this.phanLoaiHienThi = v; }
    public void setHinhAnhUrl(String v)         { this.hinhAnhUrl = v; }
    public void setSoLuongTon(Integer v)        { this.soLuongTon = v != null ? v : 0; }
    public void setSoLuongSpLoi(Integer v)      { this.soLuongSpLoi = v != null ? v : 0; }
}
