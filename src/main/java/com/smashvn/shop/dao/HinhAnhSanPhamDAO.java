package com.smashvn.shop.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.HinhAnhSanPham;

public interface HinhAnhSanPhamDAO extends JpaRepository<HinhAnhSanPham, Integer>{
    boolean existsByUrlHinhAnh(String urlHinhAnh);
}
