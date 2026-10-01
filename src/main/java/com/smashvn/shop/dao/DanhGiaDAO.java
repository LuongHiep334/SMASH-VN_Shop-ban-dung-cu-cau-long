package com.smashvn.shop.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smashvn.shop.entity.DanhGia;

public interface DanhGiaDAO extends JpaRepository<DanhGia, Integer> {
    
    // Tìm các đánh giá chưa bị xóa của một sản phẩm, sắp xếp theo thời gian mới nhất (cho frontend)
    List<DanhGia> findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(Integer sanPhamId);

    // Tìm các đánh giá chưa bị xóa và chưa bị ẩn của một sản phẩm
    List<DanhGia> findBySanPham_IdAndDaXoaFalseAndBinhLuanAnFalseOrderByNgayDanhGiaDesc(Integer idSanPham);

    // Tìm tất cả đánh giá chưa bị xóa và chưa bị ẩn trong toàn bộ hệ thống
    List<DanhGia> findByDaXoaFalseAndBinhLuanAnFalse();

    /**
     * Danh sách đánh giá được phép xuất hiện ở các nội dung công khai.
     * Một đánh giá đã có lịch sử vi phạm không được trả về, kể cả khi các cờ
     * ẩn riêng lẻ của đánh giá đang ở trạng thái hiện.
     */
    @Query("""
            select dg
            from DanhGia dg
            where dg.sanPham.id = :sanPhamId
              and dg.daXoa = false
              and not exists (
                  select vp.id
                  from CommentViolationLog vp
                  where vp.danhGia = dg
                     or (
                         vp.danhGia is null
                         and vp.sanPham = dg.sanPham
                         and vp.taiKhoan = dg.khachHang.taiKhoan
                     )
              )
            order by dg.ngayDanhGia desc
            """)
    List<DanhGia> findPublicBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
    
    // Tìm tất cả đánh giá sắp xếp theo thời gian mới nhất (cho admin)
    List<DanhGia> findAllByOrderByNgayDanhGiaDesc();
    
    // Tìm đánh giá cũ của khách hàng cho một sản phẩm để chỉnh sửa/ghi đè (chỉ lấy đánh giá chưa bị xóa)
    Optional<DanhGia> findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(Integer khachHangId, Integer sanPhamId);
    
    // Tìm đánh giá mới nhất của khách hàng theo tài khoản (phục vụ rate limit 30s)
    Optional<DanhGia> findTopByKhachHang_TaiKhoan_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(Integer taiKhoanId);
    
    // Đếm số lượng đánh giá của khách hàng trong khoảng thời gian nhất định (phục vụ rate limit 5 reviews/giờ)
    long countByKhachHang_TaiKhoan_IdAndDaXoaFalseAndNgayDanhGiaAfter(Integer taiKhoanId, LocalDateTime time);
}
