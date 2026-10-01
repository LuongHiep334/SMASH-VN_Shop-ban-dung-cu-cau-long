package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.ThuongHieu;

public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {

    /** Duplicate check for add-new: case-insensitive */
    boolean existsByTenThuongHieuIgnoreCase(String tenThuongHieu);

    /** Duplicate check for edit: case-insensitive, excludes the brand being edited */
    boolean existsByTenThuongHieuIgnoreCaseAndIdNot(String tenThuongHieu, Integer id);

    /** Lấy danh sách thương hiệu đang hiển thị (trangThai = true) */
    java.util.List<ThuongHieu> findByTrangThaiTrue();
}

