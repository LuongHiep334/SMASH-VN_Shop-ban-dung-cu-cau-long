package com.smashvn.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.Blog;
import java.util.Optional;

import com.smashvn.shop.entity.BlogStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Integer> {
    Optional<Blog> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Integer id);
    
    // Public pages queries
    Page<Blog> findByDeletedFalseAndStatus(BlogStatus status, Pageable pageable);
    Optional<Blog> findBySlugAndDeletedFalseAndStatus(String slug, BlogStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Blog b WHERE b.deleted = false AND b.status = :status AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.summary) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Blog> searchPublicBlogs(BlogStatus status, String query, Pageable pageable);

    // Admin pages queries (Search & Filter)
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Blog b WHERE " +
           "(:showDeleted = true OR b.deleted = false) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:category IS NULL OR :category = '' OR b.category = :category) AND " +
           "(:author IS NULL OR :author = '' OR b.author = :author) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " b.title LIKE %:query% OR " +
           " b.summary LIKE %:query% OR " +
           " b.content LIKE %:query% OR " +
           " b.author LIKE %:query% OR " +
           " b.category LIKE %:query%)")
    Page<Blog> searchAdminBlogs(
            @org.springframework.data.repository.query.Param("showDeleted") boolean showDeleted,
            @org.springframework.data.repository.query.Param("status") BlogStatus status,
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("author") String author,
            @org.springframework.data.repository.query.Param("query") String query,
            Pageable pageable);
            
    // For scheduled purge job
    List<Blog> findByDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);

    @org.springframework.data.jpa.repository.Query(
        "SELECT b FROM Blog b WHERE b.deleted = false AND b.status = :status " +
        "AND (b.publishDate < :currentPublishDate OR (b.publishDate = :currentPublishDate AND b.id < :currentId)) " +
        "ORDER BY b.publishDate DESC, b.id DESC"
    )
    List<Blog> findPreviousBlog(
        @org.springframework.data.repository.query.Param("status") BlogStatus status,
        @org.springframework.data.repository.query.Param("currentPublishDate") java.time.LocalDate currentPublishDate,
        @org.springframework.data.repository.query.Param("currentId") Integer currentId,
        Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT b FROM Blog b WHERE b.deleted = false AND b.status = :status " +
        "AND (b.publishDate > :currentPublishDate OR (b.publishDate = :currentPublishDate AND b.id > :currentId)) " +
        "ORDER BY b.publishDate ASC, b.id ASC"
    )
    List<Blog> findNextBlog(
        @org.springframework.data.repository.query.Param("status") BlogStatus status,
        @org.springframework.data.repository.query.Param("currentPublishDate") java.time.LocalDate currentPublishDate,
        @org.springframework.data.repository.query.Param("currentId") Integer currentId,
        Pageable pageable
    );
}

