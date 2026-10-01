package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.DanhMuc;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    /** Duplicate check for add-new: case-insensitive */
    boolean existsByTenDanhMucIgnoreCase(String tenDanhMuc);

    /** Duplicate check for edit: case-insensitive, excludes the category being edited */
    boolean existsByTenDanhMucIgnoreCaseAndIdNot(String tenDanhMuc, Integer id);

    /** Lấy danh sách danh mục đang hiển thị (trangThai = true) */
    java.util.List<DanhMuc> findByTrangThaiTrue();
}

