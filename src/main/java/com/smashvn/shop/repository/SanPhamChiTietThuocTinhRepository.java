package com.smashvn.shop.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;

public interface SanPhamChiTietThuocTinhRepository extends JpaRepository<SanPhamChiTietThuocTinh, Integer> {

    List<SanPhamChiTietThuocTinh> findBySanPhamChiTiet_Id(Integer sanPhamChiTietId);

    void deleteBySanPhamChiTiet_Id(Integer sanPhamChiTietId);

    boolean existsByThuocTinh_IdAndSanPhamChiTiet_SanPham_DanhMuc_Id(
            Integer thuocTinhId,
            Integer danhMucId);

    @Query("""
            SELECT DISTINCT s.giaTri FROM SanPhamChiTietThuocTinh s
            WHERE LOWER(s.thuocTinh.tenThuocTinh) = LOWER(:tenThuocTinh)
              AND s.giaTri IS NOT NULL AND s.giaTri != ''
              AND s.sanPhamChiTiet.trangThaiValue = true
            """)
    List<String> findDistinctGiaTriByTenThuocTinh(@Param("tenThuocTinh") String tenThuocTinh);

    @Query("""
            SELECT 
                tt.id AS thuocTinhId,
                tt.tenThuocTinh AS tenThuocTinh,
                att.giaTri AS giaTri,
                COUNT(DISTINCT sp.id) AS productCount
            FROM SanPhamChiTietThuocTinh att
            JOIN att.thuocTinh tt
            JOIN att.sanPhamChiTiet spct
            JOIN spct.sanPham sp
            JOIN sp.danhMuc dm
            JOIN DanhMucThuocTinh dmtt ON dmtt.danhMuc.id = dm.id AND dmtt.thuocTinh.id = tt.id
            WHERE dm.id = :categoryId
              AND dm.trangThai = true
              AND tt.trangThai = true
              AND dmtt.trangThai = true
              AND sp.trangThaiValue = true
              AND spct.trangThaiValue = true
              AND att.giaTri IS NOT NULL 
              AND TRIM(att.giaTri) != ''
            GROUP BY tt.id, tt.tenThuocTinh, att.giaTri
            ORDER BY tt.id ASC, att.giaTri ASC
            """)
    List<com.smashvn.shop.dto.product.AttributeOptionProjection> findAttributeOptionProjectionsByCategory(@Param("categoryId") Integer categoryId);
}
