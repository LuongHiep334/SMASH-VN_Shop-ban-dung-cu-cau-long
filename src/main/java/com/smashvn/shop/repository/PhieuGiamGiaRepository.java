package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.smashvn.shop.entity.PhieuGiamGia;
import java.util.Optional;

/**
 * Repository cho entity {@link PhieuGiamGia} – Phiếu giảm giá (Voucher).
 *
 * <p>Kế thừa {@link JpaRepository} nên đã có đầy đủ thao tác CRUD. Ngoài ra,
 * interface này bổ sung các method đặc thù cho nghiệp vụ voucher.</p>
 */
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {

    /**
     * Tìm phiếu giảm giá theo mã phiếu (không phân biệt hoa/thường).
     * Dùng khi cần đọc thông tin voucher để hiển thị preview tại trang thanh toán.
     *
     * @param maPhieu mã phiếu cần tra cứu.
     * @return {@code Optional} chứa phiếu nếu tìm thấy, rỗng nếu không.
     */
    Optional<PhieuGiamGia> findByMaPhieu(String maPhieu);

    /**
     * Tìm phiếu giảm giá theo mã phiếu có kèm PESSIMISTIC WRITE LOCK (khóa hàng ghi).
     *
     * <p>Dùng khi cần trừ {@code soLuongConLai} một cách an toàn trong môi trường
     * đồng thời (nhiều người đặt hàng cùng lúc). Lock này ngăn transaction khác
     * đọc hoặc ghi vào cùng hàng cho đến khi transaction hiện tại commit/rollback.</p>
     *
     * @param maPhieu mã phiếu cần khóa và tìm.
     * @return {@code Optional} chứa phiếu được khóa, rỗng nếu không tìm thấy.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.maPhieu = :maPhieu")
    Optional<PhieuGiamGia> findByMaPhieuWithLock(@Param("maPhieu") String maPhieu);

    /**
     * Kiểm tra xem mã phiếu đã tồn tại trong hệ thống chưa (không phân biệt hoa/thường).
     * Dùng khi TẠO MỚI voucher để tránh trùng mã.
     *
     * @param maPhieu mã phiếu cần kiểm tra.
     * @return {@code true} nếu mã đã tồn tại.
     */
    boolean existsByMaPhieuIgnoreCase(String maPhieu);

    /**
     * Kiểm tra trùng mã phiếu khi CHỈNH SỬA voucher, bỏ qua chính bản ghi đang sửa.
     * Ngăn trường hợp admin đổi mã sang một mã đã thuộc voucher khác.
     *
     * @param maPhieu mã phiếu cần kiểm tra.
     * @param id      ID của voucher đang được sửa (để loại trừ khỏi kết quả tìm kiếm).
     * @return {@code true} nếu mã đã được dùng bởi một voucher KHÁC.
     */
    boolean existsByMaPhieuIgnoreCaseAndIdNot(String maPhieu, Integer id);

    /**
     * Lấy danh sách phiếu giảm giá đang có hiệu lực và còn số lượng sử dụng.
     */
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.soLuongConLai > 0 AND p.ngayBatDau <= :now AND p.ngayKetThuc >= :now ORDER BY p.giaTri DESC")
    java.util.List<PhieuGiamGia> findActiveVouchers(@Param("now") java.time.LocalDateTime now);
}
