package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.GioHang;

public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    GioHang findByKhachHang_Id(Integer idKhachHang);

}
