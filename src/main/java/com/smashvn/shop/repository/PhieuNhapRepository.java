package com.smashvn.shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smashvn.shop.entity.PhieuNhap;

@Repository
public interface PhieuNhapRepository extends JpaRepository<PhieuNhap, Integer> {

    Optional<PhieuNhap> findByMaPhieuNhap(String maPhieuNhap);

    @Query("SELECT MAX(pn.id) FROM PhieuNhap pn")
    Integer findMaxId();
}
