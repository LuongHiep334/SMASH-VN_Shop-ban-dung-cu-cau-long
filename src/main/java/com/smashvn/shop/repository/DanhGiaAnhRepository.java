package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.HinhAnhDanhGia;

public interface DanhGiaAnhRepository extends JpaRepository<HinhAnhDanhGia, Integer> {
}
