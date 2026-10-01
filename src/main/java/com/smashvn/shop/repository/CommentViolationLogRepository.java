package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.smashvn.shop.entity.CommentViolationLog;
import java.util.List;

public interface CommentViolationLogRepository extends JpaRepository<CommentViolationLog, Integer> {
    @Query("""
            select v
            from CommentViolationLog v
            left join fetch v.taiKhoan
            left join fetch v.sanPham
            order by v.ngayViPham desc
            """)
    List<CommentViolationLog> findAllByOrderByNgayViPhamDesc();
}
