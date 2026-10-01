package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.ThongBao;
import java.util.List;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Integer> {
    List<ThongBao> findByTaiKhoan_IdOrderByNgayTaoDesc(Integer taiKhoanId);
    long countByTaiKhoan_IdAndDaDocFalse(Integer taiKhoanId);
}
