package com.smashvn.shop.service.blog;

import com.smashvn.shop.entity.Blog;
import com.smashvn.shop.entity.BlogComment;
import com.smashvn.shop.entity.BlogStatus;
import com.smashvn.shop.entity.CommentModerationKeyword;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.BlogCommentRepository;
import com.smashvn.shop.repository.BlogRepository;
import com.smashvn.shop.repository.CommentModerationKeywordRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.dto.blog.BlogCommentDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BlogCommentServiceTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private BlogCommentRepository blogCommentRepository;

    @Autowired
    private CommentModerationKeywordRepository keywordRepository;

    @Autowired
    private CommentModerationService commentModerationService;

    private Blog testBlog;
    private TaiKhoan testUser;
    private TaiKhoan adminUser;

    @BeforeEach
    void setUp() {
        // Clear caches before each test runs to avoid stale cache state
        commentModerationService.clearKeywordCache();

        // Create a unique blog
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        testBlog = Blog.builder()
                .title("Test Blog " + uuid)
                .slug("test-blog-" + uuid)
                .summary("Test Summary")
                .content("<p>Test content</p>")
                .status(BlogStatus.PUBLISHED)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        testBlog = blogRepository.save(testBlog);

        // Create a unique standard user
        testUser = new TaiKhoan();
        testUser.setUsername("user-" + uuid + "@test.com");
        testUser.setMatKhau("password");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        // Create a unique admin user
        adminUser = new TaiKhoan();
        adminUser.setUsername("admin-" + uuid + "@test.com");
        adminUser.setMatKhau("password");
        adminUser.setVaiTro("QL");
        adminUser.setTrangThai("hoat_dong");

        adminUser = taiKhoanRepository.save(adminUser);
    }

    @Test
    void testAddValidComment() {
        String content = "Đây là một bình luận hoàn toàn hợp lệ!";
        assertDoesNotThrow(() -> {
            blogService.addComment(testBlog.getSlug(), content, testUser.getUsername());
        });

        List<BlogCommentDTO> comments = blogService.getCommentsForBlog(testBlog.getId());
        assertEquals(1, comments.size());
        assertEquals(content, comments.get(0).getContent());
        assertEquals(testUser.getUsername(), comments.get(0).getEmailTaiKhoan());
        assertFalse(comments.get(0).getDeleted());
    }

    @Test
    void testAddEmptyComment() {
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "", testUser.getUsername());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "   ", testUser.getUsername());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), null, testUser.getUsername());
        });
    }

    @Test
    void testAddInvalidHtmlComment() {
        // Comment containing only HTML should be sanitized to empty, causing validation to fail
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "<script>alert('hack')</script>", testUser.getUsername());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "   <div></div>   ", testUser.getUsername());
        });
    }

    @Test
    void testAddLongComment() {
        // 1001 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1001; i++) {
            sb.append("a");
        }
        String longContent = sb.toString();

        assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), longContent, testUser.getUsername());
        });
    }

    @Test
    void testCommentRateLimit10s() {
        String content1 = "Bình luận thứ nhất";
        String content2 = "Bình luận thứ hai";

        assertDoesNotThrow(() -> {
            blogService.addComment(testBlog.getSlug(), content1, testUser.getUsername());
        });

        // The second comment should violate the 10-second limit
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), content2, testUser.getUsername());
        });

        assertTrue(exception.getMessage().contains("quá nhanh") || exception.getMessage().contains("10 giây"));
    }

    @Test
    void testCommentBanActive() {
        // Lock user comment privileges until tomorrow
        testUser.setNgayKhoaBinhLuanDen(LocalDateTime.now().plusDays(1));
        testUser = taiKhoanRepository.save(testUser);

        assertThrows(AccessDeniedException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "Xin chào", testUser.getUsername());
        });
    }

    @Test
    void testCommentBanExpired() {
        // Lock user comment privileges until yesterday (expired)
        testUser.setNgayKhoaBinhLuanDen(LocalDateTime.now().minusDays(1));
        testUser = taiKhoanRepository.save(testUser);

        assertDoesNotThrow(() -> {
            blogService.addComment(testBlog.getSlug(), "Chào ngày mới!", testUser.getUsername());
        });

        List<BlogCommentDTO> comments = blogService.getCommentsForBlog(testBlog.getId());
        assertEquals(1, comments.size());
    }

    @Test
    void testProfanityFilterSimple() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String profanityWord = "badword" + uuid;
        
        CommentModerationKeyword keyword = CommentModerationKeyword.builder()
                .keyword(profanityWord)
                .active(true)
                .build();
        keywordRepository.save(keyword);
        commentModerationService.clearKeywordCache();

        // Try adding comment containing the profanity
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "Đây là comment chứa " + profanityWord + " vô văn hóa", testUser.getUsername());
        });

        assertTrue(exception.getMessage().contains("từ ngữ không phù hợp"));
    }

    @Test
    void testProfanityFilterObfuscation() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String baseProfanity = "toxic" + uuid;

        CommentModerationKeyword keyword = CommentModerationKeyword.builder()
                .keyword(baseProfanity)
                .active(true)
                .build();
        keywordRepository.save(keyword);
        commentModerationService.clearKeywordCache();

        // Attempting to bypass with punctuation (e.g. t.o.x.i.c)
        StringBuilder obfuscated = new StringBuilder();
        for (char c : baseProfanity.toCharArray()) {
            if (obfuscated.length() > 0) {
                obfuscated.append(".");
            }
            obfuscated.append(c);
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogService.addComment(testBlog.getSlug(), "Tránh né từ cấm: " + obfuscated.toString(), testUser.getUsername());
        });

        assertTrue(exception.getMessage().contains("từ ngữ không phù hợp"));
    }

    @Test
    void testDeleteCommentAuthorization() {
        // First add a comment
        String content = "Bình luận cần xóa";
        blogService.addComment(testBlog.getSlug(), content, testUser.getUsername());
        
        List<BlogCommentDTO> comments = blogService.getCommentsForBlog(testBlog.getId());
        assertEquals(1, comments.size());
        Integer commentId = comments.get(0).getId();

        // 1. Regular user tries to delete -> should fail
        assertThrows(AccessDeniedException.class, () -> {
            blogService.deleteComment(commentId, testUser.getUsername(), "Tự ý xóa");
        });

        // 2. Admin tries to delete -> should succeed
        assertDoesNotThrow(() -> {
            blogService.deleteComment(commentId, adminUser.getUsername(), "Spam link quảng cáo");
        });

        // Comment should be soft-deleted and not visible in getCommentsForBlog
        List<BlogCommentDTO> commentsAfterDelete = blogService.getCommentsForBlog(testBlog.getId());
        assertTrue(commentsAfterDelete.isEmpty());
    }
}
