package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.smashvn.shop.entity.NhanVien;

public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("SELECT nv FROM NhanVien nv WHERE nv.hoTenNv LIKE :keyword OR nv.taiKhoan.username LIKE :keyword")
    List<NhanVien> searchNhanVien(@Param("keyword") String keyword);

    NhanVien findByTaiKhoanId(Integer idTaiKhoan);

    @Query("SELECT COUNT(nv) > 0 FROM NhanVien nv WHERE nv.soDienThoaiNv = :soDienThoai")
    boolean existsBySoDienThoai(@Param("soDienThoai") String soDienThoai);

    boolean existsBySoDienThoaiNvAndIdNot(String soDienThoaiNv, Integer id);

    @Query("SELECT nv FROM NhanVien nv WHERE nv.taiKhoan.trangThaiTaiKhoan = com.smashvn.shop.entity.AccountStatus.PENDING_LOCK")
    List<NhanVien> findPendingLockEmployees();
}
