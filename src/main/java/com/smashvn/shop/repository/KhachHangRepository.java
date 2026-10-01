package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.KhachHang;

public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    // Tìm khách hàng dựa vào ID của bảng TaiKhoan
    KhachHang findByTaiKhoan_Id(Integer idTaiKhoan);

    // Tìm khách hàng theo họ tên
    List<KhachHang> findByHoTenKhContainingIgnoreCase(String hoTenKh);

    // Tìm khách hàng theo số điện thoại
    KhachHang findBySoDienThoaiKh(String soDienThoaiKh);

    // Tìm khách hàng kết hợp cả họ và tên (LIKE trên cả hai trường)
    @Query("SELECT kh FROM KhachHang kh WHERE kh.hoTenKh LIKE CONCAT('%', :fullName, '%')")
    List<KhachHang> findByFullName(@Param("fullName") String fullName);

    // Tìm khách hàng có vai trò cụ thể trong tài khoản
    List<KhachHang> findByTaiKhoan_VaiTro(String vaiTro);

    java.util.Optional<KhachHang> findByTaiKhoan_Username(String username);

}
