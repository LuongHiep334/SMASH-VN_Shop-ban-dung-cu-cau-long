package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smashvn.shop.entity.HoaDon;

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByKhachHang_Id(Integer idKhachHang);

    List<HoaDon> findByKhachHang_IdOrderByIdDesc(Integer idKhachHang);

    java.util.Optional<HoaDon> findByGhnOrderCode(String ghnOrderCode);

    default java.util.Optional<HoaDon> findByMaDonHang(String maDonHang) {
        Integer id = parseIdFromMaDonHang(maDonHang);
        return id != null ? findById(id) : java.util.Optional.empty();
    }

    default java.util.Optional<HoaDon> findByMaDonHangOrNormalized(String maDonHang, String normalizedMaDonHang) {
        Integer id = parseIdFromMaDonHang(maDonHang != null ? maDonHang : normalizedMaDonHang);
        return id != null ? findById(id) : java.util.Optional.empty();
    }

    static Integer parseIdFromMaDonHang(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String cleanCode = code.trim().toUpperCase();

        try {
            // 1. If code contains a hyphen like HDSVN20260813-140 or DHSVN20260813-140
            if (cleanCode.contains("-")) {
                String lastPart = cleanCode.substring(cleanCode.lastIndexOf("-") + 1).trim();
                if (lastPart.matches("\\d+")) {
                    return Integer.valueOf(lastPart);
                }
            }

            // 2. If code has no hyphen like HDSVN20260813140 (Prefix HDSVN/DHSVN + 8-digit YYYYMMDD date + Order ID)
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(HDSVN|DHSVN)\\d{8}(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher dateMatcher = datePattern.matcher(cleanCode);
            if (dateMatcher.find()) {
                return Integer.valueOf(dateMatcher.group(2));
            }

            // 3. Fallback: match trailing digits if they fit within Integer.MAX_VALUE
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+$");
            java.util.regex.Matcher matcher = pattern.matcher(cleanCode);
            if (matcher.find()) {
                String digits = matcher.group();
                long val = Long.parseLong(digits);
                if (val <= Integer.MAX_VALUE) {
                    return (int) val;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    @Query("SELECT hd FROM HoaDon hd WHERE (hd.phuongThucThanhToan.maPhuongThuc = 'zalopay' OR hd.phuongThucThanhToan.tenPhuongThuc = 'ZaloPay') AND hd.trangThaiThanhToan = 'PENDING' AND hd.ngayTao < :cutoff")
    List<HoaDon> findPendingZaloPayOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT hd.id, "
            + "hd.khachHang.hoTenKh, "
            + "null, "
            + "hd.ngayTao, "
            + "null, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "null, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "null, "
            + "null, "
            + "null, "
            + "hd.tongTien, "
            + "hd.refundStatus "
            + "FROM HoaDon hd "
            + "LEFT JOIN hd.khachHang "
            + "LEFT JOIN hd.phuongThucThanhToan "
            + "WHERE hd.ngayTao BETWEEN :start AND :end "
            + "ORDER BY hd.ngayTao ASC")
    List<Object[]> findAllOrdersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(kh.id) FROM KhachHang kh "
            + "WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id "
            + "AND hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) BETWEEN :start AND :end")
    Long countNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT hd.id, "
            + "hd.khachHang.hoTenKh, "
            + "null, "
            + "hd.ngayTao, "
            + "null, "
            + "hd.phuongThucThanhToan.tenPhuongThuc, "
            + "null, "
            + "hd.trangThaiThanhToan, "
            + "hd.trangThaiDonHang, "
            + "null, "
            + "null, "
            + "null, "
            + "hd.tongTien, "
            + "hd.nhanVien.id "
            + "FROM HoaDon hd "
            + "LEFT JOIN hd.khachHang "
            + "LEFT JOIN hd.phuongThucThanhToan "
            + "WHERE (LOWER(hd.trangThaiThanhToan) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.trangThaiThanhToan) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.trangThaiThanhToan) = 'cancelled' AND hd.ngayThanhToan IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end "
            + "ORDER BY hd.ngayTao DESC")
    List<Object[]> findRawTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(DISTINCT hd.id) FROM HoaDon hd WHERE (LOWER(hd.trangThaiThanhToan) = 'paid' OR hd.trangThaiThanhToan = 'DA_THANH_TOAN' OR hd.trangThaiThanhToan = 'CHO_HOAN_TIEN' OR LOWER(hd.trangThaiThanhToan) = 'refunded' OR hd.trangThaiThanhToan = 'REFUNDED' OR (LOWER(hd.trangThaiThanhToan) = 'cancelled' AND hd.ngayThanhToan IS NOT NULL) OR hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) AND hd.ngayTao BETWEEN :start AND :end")
    Long countTransactionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT hd FROM HoaDon hd WHERE hd.id = :id")
    java.util.Optional<HoaDon> findByIdWithLock(@Param("id") Integer id);

    /**
     * Tìm các đơn hàng đang vận chuyển có mã vận đơn GHN (dùng cho Scheduler
     * Polling). Native query vì TichHopVanChuyen không có JPA Entity. Phải thêm
     * các subquery cho @Formula fields (ghnOrderCode, ghnStatus) vì SELECT hd.*
     * chỉ trả về cột vật lý, không bao gồm cột ảo @Formula.
     */
    @Query(value = """
            SELECT hd.*,
                (SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC) AS ghnOrderCode,
                (SELECT TOP 1 t.ma_van_don FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN_RETURN' ORDER BY t.id DESC) AS ghnReturnOrderCode,
                (SELECT TOP 1 t.trang_thai FROM TichHopVanChuyen t WHERE t.id_hoa_don = hd.id AND t.nha_cung_cap = 'GHN' ORDER BY t.id DESC) AS ghnStatus,
                CASE
                    WHEN hd.trang_thai_thanh_toan IN ('REFUNDED', 'DA_HOAN_TIEN') OR hd.trang_thai_hoan_hang = 'REFUNDED' THEN 'COMPLETED'
                    WHEN hd.trang_thai_thanh_toan = 'CHO_HOAN_TIEN' OR (hd.loai_yeu_cau_doi_tra = 'TRA' AND hd.trang_thai_hoan_hang = 'RETURNED') THEN 'PENDING'
                    ELSE NULL
                END AS refundStatus
            FROM HoaDon hd
            WHERE hd.trang_thai_don_hang IN ('cho_xac_nhan', 'dang_lay_hang', 'dang_giao')
              AND EXISTS (
                  SELECT 1 FROM TichHopVanChuyen t
                  WHERE t.id_hoa_don = hd.id
                    AND t.nha_cung_cap = 'GHN'
                    AND t.ma_van_don IS NOT NULL
              )
            ORDER BY hd.ngay_tao ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM HoaDon hd
            WHERE hd.trang_thai_don_hang IN ('cho_xac_nhan', 'dang_lay_hang', 'dang_giao')
              AND EXISTS (
                  SELECT 1 FROM TichHopVanChuyen t
                  WHERE t.id_hoa_don = hd.id
                    AND t.nha_cung_cap = 'GHN'
                    AND t.ma_van_don IS NOT NULL
              )
            """,
            nativeQuery = true)
    List<HoaDon> findActiveShippingOrders(Pageable pageable);

    @Query("SELECT COUNT(hd) FROM HoaDon hd WHERE ((hd.khachHang IS NOT NULL AND hd.khachHang.taiKhoan IS NOT NULL AND hd.khachHang.taiKhoan.id = :idTaiKhoan) OR LOWER(hd.emailNguoiNhan) = LOWER(:email)) AND LOWER(hd.trangThaiDonHang) != 'da_huy'")
    long countOrdersByTaiKhoanOrEmail(@Param("idTaiKhoan") Integer idTaiKhoan, @Param("email") String email);
}
