package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.PhieuNhapChiTiet;

@Repository
public interface PhieuNhapChiTietRepository extends JpaRepository<PhieuNhapChiTiet, Integer> {

    List<PhieuNhapChiTiet> findByPhieuNhap_Id(Integer idPhieuNhap);

    @Query("SELECT pnct FROM PhieuNhapChiTiet pnct JOIN FETCH pnct.phieuNhap pn LEFT JOIN FETCH pn.nhanVien nv WHERE pnct.sanPhamChiTiet.id = :idSpct ORDER BY pn.ngayNhap DESC")
    List<PhieuNhapChiTiet> findBySpctIdWithReceiptDetails(@Param("idSpct") Integer idSpct);

    @Query("SELECT pnct FROM PhieuNhapChiTiet pnct JOIN FETCH pnct.phieuNhap pn LEFT JOIN FETCH pn.nhanVien nv WHERE pnct.sanPhamChiTiet.sanPham.id = :idSanPham ORDER BY pn.ngayNhap DESC")
    List<PhieuNhapChiTiet> findBySanPhamIdWithReceiptDetails(@Param("idSanPham") Integer idSanPham);

    @Query("SELECT SUM(pnct.soLuong) FROM PhieuNhapChiTiet pnct WHERE pnct.sanPhamChiTiet.id = :idSpct")
    Long sumSoLuongNhapBySpctId(@Param("idSpct") Integer idSpct);

    @Query("SELECT COUNT(pnct.id) FROM PhieuNhapChiTiet pnct WHERE pnct.sanPhamChiTiet.id = :idSpct")
    Long countSoLanNhapBySpctId(@Param("idSpct") Integer idSpct);
}
