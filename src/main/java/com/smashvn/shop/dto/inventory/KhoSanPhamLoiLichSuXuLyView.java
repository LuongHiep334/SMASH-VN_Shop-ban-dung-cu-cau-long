package com.smashvn.shop.dto.inventory;

import java.time.LocalDateTime;

/**
 * ViewModel dai dien cho 1 record lich su thao tac xu ly kho loi (Phase 3).
 * Du lieu duoc doc tu EditLog (tenBang = 'SanPhamChiTiet', ghiChu LIKE '[KHO_LOI_%').
 */
public class KhoSanPhamLoiLichSuXuLyView {

    private Integer idLog;
    private LocalDateTime thoiGian;
    private String thoiGianFormatted;
    private String hanhDong;
    private String hanhDongRaw;
    private String badgeClass;
    private Integer soLuong;
    private String nguoiThucHien;
    private String vaiTroThucHien;
    private String ghiChu;

    public KhoSanPhamLoiLichSuXuLyView() {}

    public KhoSanPhamLoiLichSuXuLyView(Integer idLog, LocalDateTime thoiGian, String thoiGianFormatted,
                                       String hanhDong, String hanhDongRaw, String badgeClass,
                                       Integer soLuong, String nguoiThucHien, String vaiTroThucHien,
                                       String ghiChu) {
        this.idLog = idLog;
        this.thoiGian = thoiGian;
        this.thoiGianFormatted = thoiGianFormatted;
        this.hanhDong = hanhDong;
        this.hanhDongRaw = hanhDongRaw;
        this.badgeClass = badgeClass;
        this.soLuong = soLuong;
        this.nguoiThucHien = nguoiThucHien;
        this.vaiTroThucHien = vaiTroThucHien;
        this.ghiChu = ghiChu;
    }

    public Integer getIdLog() { return idLog; }
    public void setIdLog(Integer idLog) { this.idLog = idLog; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public String getThoiGianFormatted() { return thoiGianFormatted; }
    public void setThoiGianFormatted(String thoiGianFormatted) { this.thoiGianFormatted = thoiGianFormatted; }

    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }

    public String getHanhDongRaw() { return hanhDongRaw; }
    public void setHanhDongRaw(String hanhDongRaw) { this.hanhDongRaw = hanhDongRaw; }

    public String getBadgeClass() { return badgeClass; }
    public void setBadgeClass(String badgeClass) { this.badgeClass = badgeClass; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public String getNguoiThucHien() { return nguoiThucHien; }
    public void setNguoiThucHien(String nguoiThucHien) { this.nguoiThucHien = nguoiThucHien; }

    public String getVaiTroThucHien() { return vaiTroThucHien; }
    public void setVaiTroThucHien(String vaiTroThucHien) { this.vaiTroThucHien = vaiTroThucHien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}