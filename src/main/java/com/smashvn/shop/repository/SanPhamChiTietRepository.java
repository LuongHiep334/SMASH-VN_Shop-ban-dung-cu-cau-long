package com.smashvn.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.SanPhamChiTiet;

import jakarta.persistence.LockModeType;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    List<SanPhamChiTiet> findTop8ByOrderByIdDesc();

    List<SanPhamChiTiet> findBySanPham_Id(Integer sanPhamId);

    Optional<SanPhamChiTiet> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("""
            SELECT DISTINCT spct FROM SanPhamChiTiet spct
            LEFT JOIN FETCH spct.sanPhamChiTietThuocTinhs att
            LEFT JOIN FETCH spct.hinhAnhSanPhams ha
            WHERE spct.sanPham.id = :sanPhamId
              AND spct.trangThaiValue = true
            ORDER BY spct.id ASC
            """)
    List<SanPhamChiTiet> findActiveCandidatesBySanPhamId(@Param("sanPhamId") Integer sanPhamId);


    @Query("""
            SELECT DISTINCT spct FROM SanPhamChiTiet spct
            JOIN FETCH spct.sanPham sp
            LEFT JOIN FETCH sp.danhMuc dm
            LEFT JOIN FETCH sp.thuongHieu th
            LEFT JOIN FETCH sp.nhanVien nv
            LEFT JOIN FETCH spct.hinhAnhSanPhams ha
            LEFT JOIN FETCH spct.sanPhamChiTietThuocTinhs att
            WHERE sp.trangThaiValue = true
              AND dm.trangThai = true
              AND th.trangThai = true
              AND spct.trangThaiValue = true
              AND spct.soLuongTon > 0
            """)
    List<SanPhamChiTiet> findAllActiveInStock();

    /**
     * Database-first lookup used by the chatbot. Query joins SanPhamChiTietThuocTinh.
     */
    @Query("""
            SELECT DISTINCT spct FROM SanPhamChiTiet spct
            JOIN FETCH spct.sanPham sp
            LEFT JOIN FETCH sp.danhMuc dm
            LEFT JOIN FETCH sp.thuongHieu th
            LEFT JOIN spct.sanPhamChiTietThuocTinhs att
            WHERE sp.trangThaiValue = true
              AND dm.trangThai = true
              AND th.trangThai = true
              AND spct.trangThaiValue = true
              AND spct.soLuongTon > 0
              AND (:keyword IS NULL OR
                   LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(COALESCE(sp.moTa, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(COALESCE(att.giaTri, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword2, '%')) OR
                   LOWER(COALESCE(sp.moTa, '')) LIKE LOWER(CONCAT('%', :keyword2, '%')) OR
                   LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword3, '%')) OR
                   LOWER(COALESCE(sp.moTa, '')) LIKE LOWER(CONCAT('%', :keyword3, '%')))
              AND (:brandName IS NULL OR
                   REPLACE(LOWER(th.tenThuongHieu), '-', '') LIKE
                   REPLACE(LOWER(CONCAT('%', :brandName, '%')), '-', ''))
              AND (:categoryName IS NULL OR LOWER(dm.tenDanhMuc) LIKE LOWER(CONCAT('%', :categoryName, '%')))
              AND (:minPrice IS NULL OR spct.giaBan >= :minPrice)
              AND (:maxPrice IS NULL OR spct.giaBan <= :maxPrice)
              AND (:color IS NULL OR EXISTS (
                    SELECT 1 FROM SanPhamChiTietThuocTinh subAtt
                    WHERE subAtt.sanPhamChiTiet = spct
                      AND LOWER(subAtt.thuocTinh.tenThuocTinh) LIKE '%màu%'
                      AND LOWER(subAtt.giaTri) LIKE LOWER(CONCAT('%', :color, '%'))
                  ))
              AND (:weight IS NULL OR EXISTS (
                    SELECT 1 FROM SanPhamChiTietThuocTinh subAtt2
                    WHERE subAtt2.sanPhamChiTiet = spct
                      AND (LOWER(subAtt2.thuocTinh.tenThuocTinh) LIKE '%trọng%' OR LOWER(subAtt2.thuocTinh.tenThuocTinh) LIKE '%kích%' OR LOWER(subAtt2.thuocTinh.tenThuocTinh) LIKE '%size%')
                      AND LOWER(subAtt2.giaTri) LIKE LOWER(CONCAT('%', :weight, '%'))
                  ))
            ORDER BY spct.soLuongTon DESC, spct.id DESC
            """)
    List<SanPhamChiTiet> searchForChatbot(
            @Param("keyword") String keyword,
            @Param("keyword2") String keyword2,
            @Param("keyword3") String keyword3,
            @Param("brandName") String brandName,
            @Param("categoryName") String categoryName,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("color") String color,
            @Param("weight") String weight,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT spct
            FROM SanPhamChiTiet spct
            LEFT JOIN FETCH spct.sanPhamChiTietThuocTinhs att
            LEFT JOIN FETCH att.thuocTinh tt
            WHERE spct.sanPham.id = :sanPhamId
              AND spct.trangThaiValue = true
            ORDER BY spct.id ASC
            """)
    List<SanPhamChiTiet> findActiveBySanPham_Id(@Param("sanPhamId") Integer sanPhamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.id = :id")
    Optional<SanPhamChiTiet> findByIdWithLock(@Param("id") Integer id);

    /**
     * Lay danh sach bien the co hang loi (soLuongSpLoi > 0).
     * Ket qua sap xep giam dan theo soLuongSpLoi de hien nhieu loi truoc.
     * Dung cho tab Kho San Pham Loi – Phase 1.
     */
    List<SanPhamChiTiet> findBySoLuongSpLoiGreaterThanOrderBySoLuongSpLoiDesc(Integer soLuong);

    @Query("""
            SELECT DISTINCT s.giaTri FROM SanPhamChiTietThuocTinh s
            WHERE (LOWER(s.thuocTinh.tenThuocTinh) LIKE '%trọng%' OR LOWER(s.thuocTinh.tenThuocTinh) LIKE '%weight%')
              AND s.giaTri IS NOT NULL AND s.giaTri != ''
              AND s.sanPhamChiTiet.trangThaiValue = true
            """)
    List<String> findDistinctTrongLuong();

    @Query("""
            SELECT DISTINCT s.giaTri FROM SanPhamChiTietThuocTinh s
            WHERE (LOWER(s.thuocTinh.tenThuocTinh) LIKE '%kích%' OR LOWER(s.thuocTinh.tenThuocTinh) LIKE '%size%')
              AND s.giaTri IS NOT NULL AND s.giaTri != ''
              AND s.sanPhamChiTiet.trangThaiValue = true
            """)
    List<String> findDistinctKichThuoc();

    @Query("""
            SELECT DISTINCT spct
            FROM SanPhamChiTiet spct
            JOIN FETCH spct.sanPham sp
            LEFT JOIN FETCH sp.danhMuc dm
            LEFT JOIN FETCH sp.thuongHieu th
            LEFT JOIN FETCH sp.cacDotGiamGia dgg
            LEFT JOIN spct.sanPhamChiTietThuocTinhs att
            WHERE sp.trangThaiValue = true
              AND dm.trangThai = true
              AND th.trangThai = true
              AND spct.trangThaiValue = true
              AND (:idDanhMuc IS NULL OR :idDanhMuc = -1 OR dm.id = :idDanhMuc)
              AND (:idThuongHieu IS NULL OR :idThuongHieu = -1 OR th.id = :idThuongHieu)
              AND (
                    :keyword IS NULL OR :keyword = '' OR
                    LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(sp.maSanPham, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(spct.sku, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(att.giaTri, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(dm.tenDanhMuc, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(COALESCE(th.tenThuongHieu, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY sp.id DESC, spct.id ASC
            """)
    List<SanPhamChiTiet> searchActiveVariantsForPos(
            @Param("keyword") String keyword,
            @Param("idDanhMuc") Integer idDanhMuc,
            @Param("idThuongHieu") Integer idThuongHieu);
}
