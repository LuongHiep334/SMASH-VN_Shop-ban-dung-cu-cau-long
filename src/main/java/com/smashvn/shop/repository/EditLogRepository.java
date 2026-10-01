package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.EditLog;

public interface EditLogRepository extends JpaRepository<EditLog, Integer> {

    List<EditLog> findByTenBangAndIdBanGhiOrderByThoiGianAsc(String tenBang, Integer idBanGhi);

    List<EditLog> findByTenBangAndIdBanGhiInOrderByThoiGianDesc(String tenBang, List<Integer> idBanGhis);

    List<EditLog> findByTaiKhoan_Id(Integer id);

    /**
     * Phase 2 – Kho San Pham Loi: Query batch EditLog voi ghiChu [KIEM_HANG_HANG_LOI] cho cac hoa don lien quan.
     * Fetch san TaiKhoan de tranh N+1 query.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT e
        FROM EditLog e
        LEFT JOIN FETCH e.taiKhoan tk
        WHERE e.tenBang = 'HoaDon'
          AND e.idBanGhi IN :idBanGhis
          AND e.ghiChu LIKE '%[KIEM_HANG_HANG_LOI]%'
        ORDER BY e.thoiGian DESC
    """)
    List<EditLog> findKiemHangLoiLogsBatch(@org.springframework.data.repository.query.Param("idBanGhis") List<Integer> idBanGhis);

    /**
     * Phase 3 – Kho San Pham Loi: Query lich su thao tac xu ly kho loi (tenBang = 'SanPhamChiTiet', ghiChu LIKE '[KHO_LOI_%').
     * Fetch san TaiKhoan de tranh N+1 query.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT e
        FROM EditLog e
        LEFT JOIN FETCH e.taiKhoan tk
        WHERE e.tenBang = 'SanPhamChiTiet'
          AND e.idBanGhi = :spctId
          AND e.ghiChu LIKE '%KHO_LOI_%'
        ORDER BY e.thoiGian DESC
    """)
    List<EditLog> findLichSuXuLyKhoLoi(@org.springframework.data.repository.query.Param("spctId") Integer spctId);
}
