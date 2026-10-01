package com.smashvn.shop.service.blog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dto.blog.BlogCommentDTO;
import com.smashvn.shop.dto.blog.BlogDTO;
import com.smashvn.shop.entity.Blog;
import com.smashvn.shop.entity.BlogComment;
import com.smashvn.shop.entity.BlogStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.BlogCommentRepository;
import com.smashvn.shop.repository.BlogRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.common.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final FileStorageService fileStorageService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final CommentModerationService commentModerationService;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;

    private static final Safelist BLOG_SAFE_LIST
            = Safelist.relaxed()
                    .addTags("table", "thead", "tbody", "tfoot", "tr", "td", "th", "pre", "code", "figure")
                    .preserveRelativeLinks(true);

    // Customer public list query (filtered by non-deleted and published)
    public Page<BlogDTO> getBlogs(String query, Pageable pageable) {
        Page<Blog> blogs;
        if (query != null && !query.trim().isEmpty()) {
            blogs = blogRepository.searchPublicBlogs(BlogStatus.PUBLISHED, query.trim(), pageable);
        } else {
            blogs = blogRepository.findByDeletedFalseAndStatus(BlogStatus.PUBLISHED, pageable);
        }
        return blogs.map(this::convertToDTO);
    }

    // Customer public detail query
    public BlogDTO getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlugAndDeletedFalseAndStatus(slug, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết nào với đường dẫn: " + slug));
        return convertToDTO(blog);
    }

    // Get previous blog slug
    public String getPreviousBlogSlug(LocalDate publishDate, Integer id) {
        Pageable pageable = PageRequest.of(0, 1);
        List<Blog> list = blogRepository.findPreviousBlog(BlogStatus.PUBLISHED, publishDate, id, pageable);
        return list.isEmpty() ? null : list.get(0).getSlug();
    }

    // Get next blog slug
    public String getNextBlogSlug(LocalDate publishDate, Integer id) {
        Pageable pageable = PageRequest.of(0, 1);
        List<Blog> list = blogRepository.findNextBlog(BlogStatus.PUBLISHED, publishDate, id, pageable);
        return list.isEmpty() ? null : list.get(0).getSlug();
    }

    // Customer recent blogs
    public List<BlogDTO> getRecentBlogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "publishDate"));
        return blogRepository.findByDeletedFalseAndStatus(BlogStatus.PUBLISHED, pageable).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Admin specification search, filter & pagination
    public Page<BlogDTO> getAdminBlogs(boolean showDeleted, String statusStr, String category, String author, String query, Pageable pageable) {
        BlogStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = BlogStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception e) {
                // Ignore invalid status
            }
        }
        Page<Blog> blogs = blogRepository.searchAdminBlogs(
                showDeleted,
                status,
                (category != null ? category.trim() : null),
                (author != null ? author.trim() : null),
                (query != null ? query.trim() : null),
                pageable
        );
        return blogs.map(this::convertToDTO);
    }

    public BlogDTO getAdminBlogById(Integer id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết ID: " + id));
        return convertToDTO(blog);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBlog(BlogDTO blogDTO, MultipartFile imageFile, String actingUser) throws Exception {
        BlogDTO cleaned = sanitizeFields(blogDTO);

        String savedFileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            List<String> uploaded = fileStorageService.saveBlogImages(Collections.singletonList(imageFile));
            if (!uploaded.isEmpty()) {
                savedFileName = "/uploads/blog/" + uploaded.get(0);
            }
        }

        String slug = generateUniqueSlug(cleaned.getTitle(), null);
        TaiKhoan nguoiDang = taiKhoanRepository.findByUsername(actingUser);

        Blog blog = Blog.builder()
                .nguoiDang(nguoiDang)
                .title(cleaned.getTitle())
                .slug(slug)
                .summary(cleaned.getSummary())
                .content(cleaned.getContent())
                .image(savedFileName)
                .publishDate(LocalDate.now())
                .author(cleaned.getAuthor())
                .category(cleaned.getCategory())
                .tags(cleaned.getTags() != null ? String.join(",", cleaned.getTags()) : "")
                .commentsCount(0)
                .status(BlogStatus.DRAFT) // Default to DRAFT
                .deleted(false)
                .createdBy(actingUser)
                .createdAt(LocalDateTime.now())
                .build();

        blogRepository.save(blog);
        log.info("[BLOG_CMS] Created new blog post: {} (ID: {}) by {}", blog.getTitle(), blog.getId(), actingUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBlog(Integer id, BlogDTO blogDTO, MultipartFile imageFile, String actingUser, boolean isManager) throws Exception {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết ID: " + id));

        BlogDTO cleaned = sanitizeFields(blogDTO);

        // RBAC: Staff (non-manager) cannot change status to PUBLISHED from DRAFT
        if (!isManager && blogDTO.getStatus() != null) {
            BlogStatus newStatus = BlogStatus.valueOf(blogDTO.getStatus().toUpperCase());
            if (newStatus == BlogStatus.PUBLISHED && blog.getStatus() == BlogStatus.DRAFT) {
                throw new org.springframework.security.access.AccessDeniedException("Nhân viên không có quyền xuất bản bài viết!");
            }
        }

        String savedFileName = blog.getImage();
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old physical file if it was custom uploaded
            if (savedFileName != null && savedFileName.startsWith("/uploads/blog/")) {
                String oldFile = savedFileName.substring("/uploads/blog/".length());
                fileStorageService.deleteImage(oldFile, "blog");
            }
            List<String> uploaded = fileStorageService.saveBlogImages(Collections.singletonList(imageFile));
            if (!uploaded.isEmpty()) {
                savedFileName = "/uploads/blog/" + uploaded.get(0);
            }
        }

        String slug = blog.getSlug();
        if (!blog.getTitle().equals(cleaned.getTitle())) {
            slug = generateUniqueSlug(cleaned.getTitle(), id);
        }

        blog.setTitle(cleaned.getTitle());
        blog.setSlug(slug);
        blog.setSummary(cleaned.getSummary());
        blog.setContent(cleaned.getContent());
        blog.setImage(savedFileName);
        blog.setAuthor(cleaned.getAuthor());
        blog.setCategory(cleaned.getCategory());
        blog.setTags(cleaned.getTags() != null ? String.join(",", cleaned.getTags()) : "");

        if (blogDTO.getStatus() != null) {
            blog.setStatus(BlogStatus.valueOf(blogDTO.getStatus().toUpperCase()));
        }

        blog.setUpdatedBy(actingUser);
        blog.setUpdatedAt(LocalDateTime.now());

        blogRepository.save(blog);
        log.info("[BLOG_CMS] Updated blog post ID: {} by {}", id, actingUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBlog(Integer id, String actingUser) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết ID: " + id));

        blog.setDeleted(true);
        blog.setDeletedAt(LocalDateTime.now());
        blog.setUpdatedBy(actingUser);
        blog.setUpdatedAt(LocalDateTime.now());

        blogRepository.save(blog);
        log.info("[BLOG_CMS] Soft-deleted blog post ID: {} by QL {}", id, actingUser);
    }

    // Daily purge of soft deleted blogs older than 90 days
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void purgeDeletedBlogs() {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(90);
        List<Blog> blogsToPurge = blogRepository.findByDeletedTrueAndDeletedAtBefore(limitDate);

        for (Blog blog : blogsToPurge) {
            try {
                if (blog.getImage() != null && blog.getImage().startsWith("/uploads/blog/")) {
                    String imgFile = blog.getImage().substring("/uploads/blog/".length());
                    fileStorageService.deleteImage(imgFile, "blog");
                }
                blogRepository.delete(blog);
                log.info("[BLOG_PURGE] Permanently deleted blog: {} (ID: {})", blog.getTitle(), blog.getId());
            } catch (Exception e) {
                log.error("[BLOG_PURGE_ERROR] Failed to delete blog ID {}: {}", blog.getId(), e.getMessage());
            }
        }
    }

    // Slug generation
    public String generateUniqueSlug(String title, Integer currentBlogId) {
        if (title == null || title.trim().isEmpty()) {
            title = "bai-viet";
        }
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int counter = 2;
        while (true) {
            boolean exists;
            if (currentBlogId == null) {
                exists = blogRepository.existsBySlug(slug);
            } else {
                exists = blogRepository.existsBySlugAndIdNot(slug, currentBlogId);
            }
            if (!exists) {
                return slug;
            }
            slug = baseSlug + "-" + counter;
            counter++;
        }
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String temp = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String out = pattern.matcher(temp).replaceAll("");
        out = out.replace("đ", "d").replace("Đ", "D");
        out = out.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (out.isEmpty()) {
            out = "post";
        }
        return out;
    }

    // HTML sanitization using Jsoup
    public BlogDTO sanitizeFields(BlogDTO dto) {
        if (dto == null) {
            return null;
        }
        dto.setTitle(Jsoup.clean(dto.getTitle() != null ? dto.getTitle() : "", Safelist.none()));
        dto.setSummary(Jsoup.clean(dto.getSummary() != null ? dto.getSummary() : "", Safelist.none()));
        dto.setAuthor(Jsoup.clean(dto.getAuthor() != null ? dto.getAuthor() : "", Safelist.none()));

        List<String> cleanedTags = new ArrayList<>();
        if (dto.getTags() != null) {
            for (String tag : dto.getTags()) {
                if (tag != null) {
                    cleanedTags.add(Jsoup.clean(tag, Safelist.none()));
                }
            }
        }
        dto.setTags(cleanedTags);

        if (dto.getContent() != null) {
            dto.setContent(Jsoup.clean(dto.getContent(), "http://localhost", BLOG_SAFE_LIST));
        }
        return dto;
    }

    private BlogDTO convertToDTO(Blog blog) {
        List<String> tagList = new java.util.ArrayList<>();
        if (blog.getTags() != null && !blog.getTags().trim().isEmpty()) {
            tagList = Arrays.stream(blog.getTags().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        return BlogDTO.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .summary(blog.getSummary())
                .content(blog.getContent())
                .image(blog.getImage())
                .publishDate(blog.getPublishDate() != null ? blog.getPublishDate().toString() : "")
                .author(blog.getAuthor())
                .category(blog.getCategory())
                .tags(tagList)
                .commentsCount(blogCommentRepository.countByBlogIdAndDeletedFalse(blog.getId()))
                .status(blog.getStatus() != null ? blog.getStatus().name() : "DRAFT")
                .deleted(blog.getDeleted())
                .createdBy(blog.getCreatedBy())
                .createdAt(blog.getCreatedAt() != null ? blog.getCreatedAt().toString() : "")
                .updatedBy(blog.getUpdatedBy())
                .updatedAt(blog.getUpdatedAt() != null ? blog.getUpdatedAt().toString() : "")
                .idNguoiDang(blog.getNguoiDang() != null ? blog.getNguoiDang().getId() : null)
                .emailNguoiDang(blog.getNguoiDang() != null ? blog.getNguoiDang().getUsername() : null)
                .build();
    }

    // --- BLOG COMMENT SYSTEM V1.26 ---
    public List<BlogCommentDTO> getCommentsForBlog(Integer blogId) {
        List<BlogComment> comments = blogCommentRepository.findByBlogIdAndDeletedFalseOrderByCreatedAtDesc(blogId);
        return comments.stream().map(c -> {
            String tenHienThi = getDisplayNameForAccount(c.getTaiKhoan());
            String deletedByEmail = c.getDeletedBy() != null ? c.getDeletedBy().getUsername() : null;
            return BlogCommentDTO.builder()
                    .id(c.getId())
                    .idBlog(c.getBlog().getId())
                    .idTaiKhoan(c.getTaiKhoan().getId())
                    .emailTaiKhoan(c.getTaiKhoan().getUsername())
                    .tenHienThi(tenHienThi)
                    .content(c.getContent())
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")
                    .deleted(c.getDeleted())
                    .deletedAt(c.getDeletedAt() != null ? c.getDeletedAt().toString() : "")
                    .deletedReason(c.getDeletedReason())
                    .deletedByEmail(deletedByEmail)
                    .build();
        }).collect(Collectors.toList());
    }

    public String getDisplayNameForAccount(TaiKhoan tk) {
        if (tk == null) {
            return "Ẩn danh";
        }
        String role = tk.getVaiTro();
        if ("KH".equals(role)) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                String ho = kh.getHoKh() != null ? kh.getHoKh().trim() : "";
                String ten = kh.getTenKh() != null ? kh.getTenKh().trim() : "";
                String full = (ho + " " + ten).trim();
                return !full.isEmpty() ? full : "Khách hàng";
            }
            return "Khách hàng";
        } else if ("NV".equals(role) || "QL".equals(role)) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(tk.getId());
            if (nv != null && nv.getHoTenNv() != null && !nv.getHoTenNv().trim().isEmpty()) {
                return nv.getHoTenNv().trim();
            }
            return "QL".equals(role) ? "Quản lý hệ thống" : "Nhân viên hệ thống";
        }
        return tk.getUsername();
    }

    @Transactional(rollbackFor = Exception.class)
    public void addComment(String slug, String content, String actingEmail) {
        // 1. Verify blog status
        Blog blog = blogRepository.findBySlugAndDeletedFalseAndStatus(slug, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại hoặc đã bị ẩn."));

        // 2. Verify acting user
        TaiKhoan tk = taiKhoanRepository.findByUsername(actingEmail);
        if (tk == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại.");
        }

        // 3. Authorization check for comment ban
        if (tk.getNgayKhoaBinhLuanDen() != null && tk.getNgayKhoaBinhLuanDen().isAfter(LocalDateTime.now())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Tài khoản của bạn đang bị khóa bình luận đến " + tk.getNgayKhoaBinhLuanDen().toString()
            );
        }

        // 4. Content length validation
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung bình luận không được để trống.");
        }

        // 5. XSS Sanitization (Run BEFORE profanity and database)
        String sanitizedContent = Jsoup.clean(content, Safelist.none()).trim();
        if (sanitizedContent.isEmpty()) {
            throw new IllegalArgumentException("Nội dung bình luận không hợp lệ.");
        }
        if (sanitizedContent.length() > 1000) {
            throw new IllegalArgumentException("Bình luận vượt quá độ dài cho phép (tối đa 1000 ký tự).");
        }

        // 6. Dual Rate Limit check (10s, 1m, 1h)
        LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
        int count10s = blogCommentRepository.countByTaiKhoanIdAndCreatedAtAfter(tk.getId(), tenSecondsAgo);
        if (count10s > 0) {
            throw new IllegalArgumentException("Bạn gửi bình luận quá nhanh. Vui lòng đợi 10 giây.");
        }

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        int count1m = blogCommentRepository.countByTaiKhoanIdAndCreatedAtAfter(tk.getId(), oneMinuteAgo);
        if (count1m >= 10) {
            throw new IllegalArgumentException("Bạn gửi bình luận quá thường xuyên. Vui lòng thử lại sau.");
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int count1h = blogCommentRepository.countByTaiKhoanIdAndCreatedAtAfter(tk.getId(), oneHourAgo);
        if (count1h >= 50) {
            throw new IllegalArgumentException("Bạn gửi bình luận quá thường xuyên. Vui lòng thử lại sau.");
        }

        // 7. Unicode-Aware Profanity Filter Check
        if (containsProfanity(sanitizedContent)) {
            throw new IllegalArgumentException("Nội dung bình luận chứa từ ngữ không phù hợp.");
        }

        // 8. Create and persist comment
        BlogComment comment = BlogComment.builder()
                .blog(blog)
                .taiKhoan(tk)
                .content(sanitizedContent)
                .createdAt(LocalDateTime.now())
                .createdBy(tk)
                .deleted(false)
                .build();

        blogCommentRepository.save(comment);
        log.info("[BLOG_COMMENT] Comment posted successfully on blog slug: {} by user: {}", slug, actingEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Integer commentId, String actingEmail, String reason) {
        TaiKhoan actor = taiKhoanRepository.findByUsername(actingEmail);
        if (actor == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại.");
        }

        // Comment moderation authorization: ONLY admin/manager (QL)
        if (!"QL".equals(actor.getVaiTro())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện chức năng này!");
        }

        BlogComment comment = blogCommentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Bình luận không tồn tại hoặc đã bị xóa trước đó."));

        comment.setDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        comment.setDeletedBy(actor);
        comment.setDeletedReason(reason != null ? Jsoup.clean(reason, Safelist.none()).trim() : "Vi phạm quy chuẩn");

        blogCommentRepository.save(comment);
        log.info("[BLOG_COMMENT] Soft-deleted comment ID: {} by admin: {}, Reason: {}", commentId, actingEmail, reason);
    }

    private boolean containsProfanity(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String normalizedComment = normalizeAndCollapse(content);
        String paddedComment = " " + normalizedComment + " ";

        List<String> keywords = commentModerationService.getActiveKeywords();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }
            String paddedKeyword = " " + keyword + " ";
            if (paddedComment.contains(paddedKeyword)) {
                log.warn("[PROFANITY_FILTER] Blocked comment containing prohibited keyword: '{}'", keyword);
                return true;
            }
        }
        return false;
    }

    private String normalizeAndCollapse(String input) {
        if (input == null) {
            return "";
        }
        String temp = input.toLowerCase();
        temp = java.text.Normalizer.normalize(temp, java.text.Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}+", "");
        temp = temp.replace('đ', 'd').replace('Đ', 'd');
        // Replace non-alphanumeric characters with spaces to separate tokens and handle punctuation-obfuscation
        temp = temp.replaceAll("[^a-z0-9\\s]", " ");

        // Now collapse single-character sequences
        String[] words = temp.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        int prevWordLen = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() == 1) {
                if (prevWordLen == 1) {
                    // Collapse: no space
                    sb.append(word);
                } else {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(word);
                }
                prevWordLen = 1;
            } else {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(word);
                prevWordLen = word.length();
            }
        }
        return sb.toString();
    }
}
