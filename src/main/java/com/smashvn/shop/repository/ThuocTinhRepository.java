package com.smashvn.shop.repository;

import com.smashvn.shop.entity.ThuocTinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThuocTinhRepository extends JpaRepository<ThuocTinh, Integer> {
    List<ThuocTinh> findByTrangThaiTrueOrderByIdAsc();
    boolean existsByTenThuocTinhIgnoreCase(String tenThuocTinh);
    Optional<ThuocTinh> findByTenThuocTinhIgnoreCase(String tenThuocTinh);
    Optional<ThuocTinh> findByTenThuocTinhIgnoreCaseAndTrangThaiTrue(String tenThuocTinh);
    List<ThuocTinh> findByTenThuocTinhContainingIgnoreCaseAndTrangThaiTrue(String keyword);
}
