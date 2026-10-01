package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.dto.product.BrandRevenueDTO;
import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.entity.HoaDonChiTiet;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    List<HoaDonChiTiet> findByHoaDon_Id(Integer idHoaDon);

    @Query("SELECT COALESCE(SUM(hdct.soLuong), 0L) FROM HoaDonChiTiet hdct "
            + "WHERE LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh', 'delivered') "
            + "AND (hdct.hoaDon.trangThaiThanhToan IS NULL OR UPPER(hdct.hoaDon.trangThaiThanhToan) NOT IN ('REFUNDED', 'HOAN_TIEN', 'DA_HOAN_TIEN')) "
            + "AND hdct.hoaDon.ngayTao BETWEEN :start AND :end")
    Long getTotalProductsSold(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.smashvn.shop.dto.product.TopProductDTO("
            + "hdct.sanPhamChiTiet.sanPham.id, "
            + "hdct.sanPhamChiTiet.sanPham.tenSanPham, "
            + "COALESCE((SELECT MIN(ha.urlHinhAnh) FROM HinhAnhSanPham ha WHERE ha.sanPhamChiTiet.id = MIN(hdct.sanPhamChiTiet.id)), ''), "
            + "hdct.sanPhamChiTiet.sanPham.danhMuc.tenDanhMuc, "
            + "COALESCE(SUM(hdct.soLuong), 0L), "
            + "COALESCE(SUM(hdct.soLuong * hdct.donGia), 0.0)"
            + ") "
            + "FROM HoaDonChiTiet hdct "
            + "WHERE LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh', 'delivered') "
            + "AND (hdct.hoaDon.trangThaiThanhToan IS NULL OR UPPER(hdct.hoaDon.trangThaiThanhToan) NOT IN ('REFUNDED', 'HOAN_TIEN', 'DA_HOAN_TIEN')) "
            + "AND hdct.hoaDon.ngayTao BETWEEN :startDate AND :endDate "
            + "GROUP BY hdct.sanPhamChiTiet.sanPham.id, hdct.sanPhamChiTiet.sanPham.tenSanPham, hdct.sanPhamChiTiet.sanPham.danhMuc.tenDanhMuc "
            + "ORDER BY SUM(hdct.soLuong) DESC")
    List<TopProductDTO> findBestSellingProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(hdct.soLuong * hdct.donGia), 0.0) FROM HoaDonChiTiet hdct "
            + "WHERE LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh', 'delivered') "
            + "AND (hdct.hoaDon.trangThaiThanhToan IS NULL OR UPPER(hdct.hoaDon.trangThaiThanhToan) NOT IN ('REFUNDED', 'HOAN_TIEN', 'DA_HOAN_TIEN')) "
            + "AND hdct.hoaDon.ngayTao BETWEEN :startDate AND :endDate")
    Double getTotalProductLineRevenueInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT new com.smashvn.shop.dto.product.BrandRevenueDTO("
            + "hdct.sanPhamChiTiet.sanPham.thuongHieu.id, "
            + "COALESCE(hdct.sanPhamChiTiet.sanPham.thuongHieu.tenThuongHieu, 'Khác'), "
            + "COALESCE(SUM(hdct.soLuong), 0L), "
            + "COALESCE(SUM(hdct.soLuong * hdct.donGia), 0.0)"
            + ") "
            + "FROM HoaDonChiTiet hdct "
            + "WHERE LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh', 'delivered') "
            + "AND (hdct.hoaDon.trangThaiThanhToan IS NULL OR UPPER(hdct.hoaDon.trangThaiThanhToan) NOT IN ('REFUNDED', 'HOAN_TIEN', 'DA_HOAN_TIEN')) "
            + "AND hdct.hoaDon.ngayTao BETWEEN :startDate AND :endDate "
            + "GROUP BY hdct.sanPhamChiTiet.sanPham.thuongHieu.id, hdct.sanPhamChiTiet.sanPham.thuongHieu.tenThuongHieu "
            + "ORDER BY SUM(hdct.soLuong * hdct.donGia) DESC")
    List<BrandRevenueDTO> findRevenueByBrand(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(hdct) > 0 FROM HoaDonChiTiet hdct "
            + "WHERE hdct.hoaDon.khachHang.taiKhoan.id = :taiKhoanId "
            + "AND hdct.sanPhamChiTiet.sanPham.id = :sanPhamId "
            + "AND LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh')")
    boolean hasPurchasedProduct(@Param("taiKhoanId") Integer taiKhoanId, @Param("sanPhamId") Integer sanPhamId);

    boolean existsBySanPhamChiTiet_Id(Integer idBienThe);

    @Query("SELECT DISTINCT hdct.sanPhamChiTiet.id FROM HoaDonChiTiet hdct "
            + "WHERE hdct.sanPhamChiTiet.sanPham.id = :sanPhamId")
    List<Integer> findOrderedVariantIdsBySanPhamId(@Param("sanPhamId") Integer sanPhamId);

    /**
     * Phase 2 – Kho San Pham Loi: Tim cac HoaDonChiTiet da tung ghi nhan dua
     * bien the nay vao kho loi. Dieu kien: hdct.sanPhamChiTiet.id = :spctId va
     * hoaDon.trangThaiXuLyHangHoan = DA_CHUYEN_KHO_LOI.
     */
    @Query("""
        SELECT hdct
        FROM HoaDonChiTiet hdct
        JOIN FETCH hdct.hoaDon hd
        WHERE hdct.sanPhamChiTiet.id = :spctId
          AND hd.trangThaiXuLyHangHoan = com.smashvn.shop.entity.ReturnInventoryStatus.DA_CHUYEN_KHO_LOI
        ORDER BY hd.id DESC
    """)
    List<HoaDonChiTiet> findKhoLoiSources(@Param("spctId") Integer spctId);

    @Query("SELECT hdct.sanPhamChiTiet.sanPham.id, COALESCE(SUM(hdct.soLuong), 0L) "
            + "FROM HoaDonChiTiet hdct "
            + "WHERE LOWER(hdct.hoaDon.trangThaiDonHang) IN ('da_giao', 'hoan_thanh', 'delivered') "
            + "AND (hdct.hoaDon.trangThaiThanhToan IS NULL OR UPPER(hdct.hoaDon.trangThaiThanhToan) NOT IN ('REFUNDED', 'HOAN_TIEN', 'DA_HOAN_TIEN')) "
            + "AND hdct.hoaDon.ngayTao BETWEEN :startDate AND :endDate "
            + "GROUP BY hdct.sanPhamChiTiet.sanPham.id")
    List<Object[]> findSoldQuantityByProductInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
