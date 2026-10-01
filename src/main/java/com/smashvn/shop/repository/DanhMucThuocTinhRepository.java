package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.DanhMucThuocTinh;

public interface DanhMucThuocTinhRepository extends JpaRepository<DanhMucThuocTinh, Integer> {

    List<DanhMucThuocTinh> findByDanhMucIdAndTrangThaiTrue(Integer idDanhMuc);

    @Modifying
    @Transactional
    @Query("DELETE FROM DanhMucThuocTinh dmtt WHERE dmtt.danhMuc.id = :danhMucId")
    void deleteByDanhMucId(@Param("danhMucId") Integer danhMucId);
}
