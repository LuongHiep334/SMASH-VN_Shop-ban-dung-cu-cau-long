package com.smashvn.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.SoDiaChi;

public interface SoDiaChiRepository extends JpaRepository<SoDiaChi, Integer> {

    // Tìm danh sách địa chỉ theo ID khách hàng
    List<SoDiaChi> findByKhachHang_Id(Integer idKhachHang);

    // Tìm danh sách địa chỉ theo ID khách hàng sắp xếp theo mặc định giao hàng trước
    @Query("SELECT sd FROM SoDiaChi sd WHERE sd.khachHang.id = :idKhachHang "
            + "ORDER BY sd.diaChiMacDinh DESC, sd.id ASC")
    List<SoDiaChi> findByKhachHang_IdOrderByDefault(@Param("idKhachHang") Integer idKhachHang);

    // Tìm địa chỉ mặc định giao hàng của khách hàng
    Optional<SoDiaChi> findByKhachHang_IdAndDiaChiMacDinhTrue(Integer idKhachHang);

    // Tìm địa chỉ theo địa chỉ cụ thể và khách hàng
    Optional<SoDiaChi> findByKhachHang_IdAndDiaChiCuThe(Integer idKhachHang, String diaChiCuThe);

    // Kiểm tra tồn tại địa chỉ mặc định giao hàng
    boolean existsByKhachHang_IdAndDiaChiMacDinhTrue(Integer idKhachHang);

    // Đếm số địa chỉ giao hàng mặc định cho khách hàng
    long countByKhachHang_IdAndDiaChiMacDinhTrue(Integer idKhachHang);
}
