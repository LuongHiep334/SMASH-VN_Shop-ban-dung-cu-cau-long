package com.smashvn.shop.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.BlogComment;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer> {

    // SAFE SOFT-DELETE METHODS: DO NOT use findAll() or findById() directly in BlogService
    List<BlogComment> findByBlogIdAndDeletedFalseOrderByCreatedAtDesc(Integer blogId);

    int countByBlogIdAndDeletedFalse(Integer blogId);

    Optional<BlogComment> findByIdAndDeletedFalse(Integer id);

    // Rate limit check: count comments within time window for specific account
    int countByTaiKhoanIdAndCreatedAtAfter(Integer idTaiKhoan, LocalDateTime timeThreshold);
}
