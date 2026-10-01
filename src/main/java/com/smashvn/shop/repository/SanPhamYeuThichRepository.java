package com.smashvn.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPhamYeuThich;
import com.smashvn.shop.entity.SanPhamYeuThich.SanPhamYeuThichKey;

public interface SanPhamYeuThichRepository extends JpaRepository<SanPhamYeuThich, SanPhamYeuThichKey> {

    // Tìm danh sách sản phẩm yêu thích của khách hàng
    @Query("SELECT spy FROM SanPhamYeuThich spy WHERE spy.khachHang.id = :idKhachHang "
            + "ORDER BY spy.ngayThem DESC")
    List<SanPhamYeuThich> findByKhachHang_Id(@Param("idKhachHang") Integer idKhachHang);

    // Tìm danh sách sản phẩm yêu thích của khách hàng với phân trang
    @Query("SELECT spy FROM SanPhamYeuThich spy WHERE spy.khachHang.id = :idKhachHang "
            + "ORDER BY spy.ngayThem DESC")
    List<SanPhamYeuThich> findTop10ByKhachHang_Id(@Param("idKhachHang") Integer idKhachHang);

    // Kiểm tra sản phẩm đã được thêm vào wishlist của khách hàng chưa
    boolean existsById_KhachHangIdAndId_SanPhamId(Integer idKhachHang, Integer idSanPham);

    // Tìm một mục wishlist cụ thể
    Optional<SanPhamYeuThich> findById_KhachHangIdAndId_SanPhamId(Integer idKhachHang, Integer idSanPham);

    // Đếm số lượng sản phẩm yêu thích của khách hàng
    long countByKhachHang_Id(Integer idKhachHang);

    // Xóa sản phẩm yêu thích
    void deleteById_KhachHangIdAndId_SanPhamId(Integer idKhachHang, Integer idSanPham);

    // Xóa tất cả sản phẩm yêu thích của khách hàng
    void deleteByKhachHang_Id(Integer idKhachHang);

    // Tìm khách hàng yêu thích sản phẩm cụ thể
    @Query("SELECT spy.khachHang FROM SanPhamYeuThich spy WHERE spy.sanPham.id = :idSanPham")
    List<KhachHang> findCustomersByProduct(@Param("idSanPham") Integer idSanPham);

    // Đếm số khách hàng yêu thích sản phẩm
    long countById_SanPhamId(Integer idSanPham);
}
