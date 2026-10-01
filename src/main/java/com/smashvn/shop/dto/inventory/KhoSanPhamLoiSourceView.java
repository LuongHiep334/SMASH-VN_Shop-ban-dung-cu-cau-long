package com.smashvn.shop.dto.inventory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel dai dien cho tung don hang nguon da chuyen bien the vao kho loi.
 */
public class KhoSanPhamLoiSourceView {

    private Integer idHoaDon;
    private String maDonHang;
    private Integer soLuongDaChuyen;
    private String lyDoHoanTra;
    private String loaiYeuCauDoiTra;
    private String loaiYeuCauDoiTraRaw;
    private String trangThaiHoanHang;
    private String trangThaiXuLyHangHoan;
    private List<String> bangChungList = new ArrayList<>();
    private String nguoiXuLy;
    private String vaiTroNguoiXuLy;
    private LocalDateTime thoiGianXuLy;
    private String thoiGianXuLyFormatted;

    public KhoSanPhamLoiSourceView() {}

    public boolean isVideo(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();
        return p.endsWith(".mp4") || p.endsWith(".webm") || p.endsWith(".mov") || p.endsWith(".mkv");
    }

    public boolean isImage(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();
        return p.endsWith(".jpg") || p.endsWith(".jpeg") || p.endsWith(".png") || p.endsWith(".webp") || p.endsWith(".gif");
    }

    public Integer getIdHoaDon() { return idHoaDon; }
    public void setIdHoaDon(Integer idHoaDon) { this.idHoaDon = idHoaDon; }

    public String getMaDonHang() { return maDonHang; }
    public void setMaDonHang(String maDonHang) { this.maDonHang = maDonHang; }

    public Integer getSoLuongDaChuyen() { return soLuongDaChuyen; }
    public void setSoLuongDaChuyen(Integer soLuongDaChuyen) { this.soLuongDaChuyen = soLuongDaChuyen; }

    public String getLyDoHoanTra() { return lyDoHoanTra; }
    public void setLyDoHoanTra(String lyDoHoanTra) { this.lyDoHoanTra = lyDoHoanTra; }

    public String getLoaiYeuCauDoiTra() { return loaiYeuCauDoiTra; }
    public void setLoaiYeuCauDoiTra(String loaiYeuCauDoiTra) { this.loaiYeuCauDoiTra = loaiYeuCauDoiTra; }

    public String getLoaiYeuCauDoiTraRaw() { return loaiYeuCauDoiTraRaw; }
    public void setLoaiYeuCauDoiTraRaw(String loaiYeuCauDoiTraRaw) { this.loaiYeuCauDoiTraRaw = loaiYeuCauDoiTraRaw; }

    public String getTrangThaiHoanHang() { return trangThaiHoanHang; }
    public void setTrangThaiHoanHang(String trangThaiHoanHang) { this.trangThaiHoanHang = trangThaiHoanHang; }

    public String getTrangThaiXuLyHangHoan() { return trangThaiXuLyHangHoan; }
    public void setTrangThaiXuLyHangHoan(String trangThaiXuLyHangHoan) { this.trangThaiXuLyHangHoan = trangThaiXuLyHangHoan; }

    public List<String> getBangChungList() { return bangChungList; }
    public void setBangChungList(List<String> bangChungList) { this.bangChungList = bangChungList != null ? bangChungList : new ArrayList<>(); }

    public String getNguoiXuLy() { return nguoiXuLy; }
    public void setNguoiXuLy(String nguoiXuLy) { this.nguoiXuLy = nguoiXuLy; }

    public String getVaiTroNguoiXuLy() { return vaiTroNguoiXuLy; }
    public void setVaiTroNguoiXuLy(String vaiTroNguoiXuLy) { this.vaiTroNguoiXuLy = vaiTroNguoiXuLy; }

    public LocalDateTime getThoiGianXuLy() { return thoiGianXuLy; }
    public void setThoiGianXuLy(LocalDateTime thoiGianXuLy) { this.thoiGianXuLy = thoiGianXuLy; }

    public String getThoiGianXuLyFormatted() { return thoiGianXuLyFormatted; }
    public void setThoiGianXuLyFormatted(String thoiGianXuLyFormatted) { this.thoiGianXuLyFormatted = thoiGianXuLyFormatted; }
}