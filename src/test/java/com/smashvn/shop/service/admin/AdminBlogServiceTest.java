package com.smashvn.shop.service.admin;

import com.smashvn.shop.dto.blog.BlogDTO;
import com.smashvn.shop.entity.Blog;
import com.smashvn.shop.entity.BlogStatus;
import com.smashvn.shop.repository.BlogRepository;
import com.smashvn.shop.service.blog.BlogService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminBlogServiceTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testBlogStatusConstraint() {
        // Since JPA Type Safety blocks non-enum values on entities,
        // we execute a native SQL query to test the database CHECK constraint CK_BLOG_STATUS.
        assertThrows(Exception.class, () -> {
            entityManager.createNativeQuery(
                "INSERT INTO Blog (tieu_de, duong_dan, trang_thai, da_xoa, ngay_tao) " +
                "VALUES ('Test Invalid Status', 'test-invalid-status', 'INVALID', 0, GETDATE())"
            ).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    void testCustomSafelistPreservesCkeditorContent() {
        String inputContent = "<figure class=\"table\"><table><thead><tr><th>Header 1</th><th>Header 2</th></tr></thead>" +
                "<tbody><tr><td>Data 1</td><td>Data 2</td></tr></tbody></table></figure>" +
                "<pre><code>System.out.println(\"Hello World\");</code></pre>" +
                "<p><a href=\"/uploads/blog/image.png\">Relative Link</a></p>";
        
        BlogDTO dto = BlogDTO.builder()
                .title("Test Title")
                .summary("Test Summary")
                .content(inputContent)
                .author("Author")
                .build();
        
        BlogDTO sanitized = blogService.sanitizeFields(dto);
        String cleaned = sanitized.getContent();
        
        assertTrue(cleaned.contains("table"));
        assertTrue(cleaned.contains("thead"));
        assertTrue(cleaned.contains("tbody"));
        assertTrue(cleaned.contains("tr"));
        assertTrue(cleaned.contains("th"));
        assertTrue(cleaned.contains("td"));
        assertTrue(cleaned.contains("pre"));
        assertTrue(cleaned.contains("code"));
        assertTrue(cleaned.contains("figure"));
        assertTrue(cleaned.contains("href=\"/uploads/blog/image.png\""));
    }

    @Test
    void testCustomSafelistRemovesUnsafeContent() {
        String inputContent = "<p>Safe text</p><script>alert('hack');</script><iframe src='javascript:alert(1)'></iframe>" +
                "<img src='x' onerror='alert(1)'>";
        
        BlogDTO dto = BlogDTO.builder()
                .title("Test Title")
                .summary("Test Summary")
                .content(inputContent)
                .author("Author")
                .build();
                
        BlogDTO sanitized = blogService.sanitizeFields(dto);
        String cleaned = sanitized.getContent();
        
        assertFalse(cleaned.contains("script"));
        assertFalse(cleaned.contains("iframe"));
        assertFalse(cleaned.contains("onerror"));
    }

    @Test
    void testSoftDeleteRetention() throws Exception {
        Blog blog = Blog.builder()
                .title("Test Soft Delete")
                .slug("test-soft-delete")
                .summary("Summary")
                .content("<p>Content</p>")
                .status(BlogStatus.DRAFT)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        blog = blogRepository.save(blog);
        
        blogService.deleteBlog(blog.getId(), "testUser");
        
        Blog softDeleted = blogRepository.findById(blog.getId()).orElse(null);
        assertNotNull(softDeleted);
        assertTrue(softDeleted.getDeleted());
        assertNotNull(softDeleted.getDeletedAt());
        assertEquals("testUser", softDeleted.getUpdatedBy());
    }

    @Test
    void testScheduledPurge() {
        // Create a blog that is deleted 91 days ago
        Blog oldDeletedBlog = Blog.builder()
                .title("Old Deleted Blog")
                .slug("old-deleted-blog")
                .summary("Summary")
                .content("<p>Content</p>")
                .status(BlogStatus.DRAFT)
                .deleted(true)
                .deletedAt(LocalDateTime.now().minusDays(91))
                .createdAt(LocalDateTime.now().minusDays(100))
                .build();
        
        // Create a blog that is deleted 89 days ago (should not be purged)
        Blog recentDeletedBlog = Blog.builder()
                .title("Recent Deleted Blog")
                .slug("recent-deleted-blog")
                .summary("Summary")
                .content("<p>Content</p>")
                .status(BlogStatus.DRAFT)
                .deleted(true)
                .deletedAt(LocalDateTime.now().minusDays(89))
                .createdAt(LocalDateTime.now().minusDays(100))
                .build();
        
        oldDeletedBlog = blogRepository.save(oldDeletedBlog);
        recentDeletedBlog = blogRepository.save(recentDeletedBlog);
        
        // Run the purge task
        blogService.purgeDeletedBlogs();
        
        // Verify old deleted blog is purged (no longer exists in DB)
        assertFalse(blogRepository.findById(oldDeletedBlog.getId()).isPresent());
        
        // Verify recent deleted blog is NOT purged (still exists in DB)
        assertTrue(blogRepository.findById(recentDeletedBlog.getId()).isPresent());
    }

    @Test
    void testGenerateUniqueSlug() {
        String slug1 = blogService.generateUniqueSlug("Học chơi cầu lông", null);
        assertEquals("hoc-choi-cau-long", slug1);
        
        Blog blog = Blog.builder()
                .title("Học chơi cầu lông")
                .slug(slug1)
                .summary("Summary")
                .content("<p>Content</p>")
                .status(BlogStatus.DRAFT)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        blog = blogRepository.save(blog);
        
        // Generating for another blog with same title should append suffix
        String slug2 = blogService.generateUniqueSlug("Học chơi cầu lông", null);
        assertEquals("hoc-choi-cau-long-2", slug2);
        
        // Generating for the same blog (editing case) should allow using the same slug
        String slugSelf = blogService.generateUniqueSlug("Học chơi cầu lông", blog.getId());
        assertEquals("hoc-choi-cau-long", slugSelf);
    }

    @Test
    void testStaffCannotPublishDraft() {
        Blog blog = Blog.builder()
                .title("Draft Blog")
                .slug("draft-blog")
                .summary("Summary")
                .content("<p>Content</p>")
                .status(BlogStatus.DRAFT)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        blog = blogRepository.save(blog);
        
        BlogDTO updateDto = BlogDTO.builder()
                .title("Draft Blog")
                .summary("Summary")
                .content("<p>Content</p>")
                .status("PUBLISHED")
                .build();
        
        final Integer blogId = blog.getId();
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            blogService.updateBlog(blogId, updateDto, null, "nvUser", false); // false = not manager
        });
        
        // Manager should be able to publish
        assertDoesNotThrow(() -> {
            blogService.updateBlog(blogId, updateDto, null, "qlUser", true); // true = manager
        });
        
        Blog updated = blogRepository.findById(blogId).orElse(null);
        assertNotNull(updated);
        assertEquals(BlogStatus.PUBLISHED, updated.getStatus());
    }
}
