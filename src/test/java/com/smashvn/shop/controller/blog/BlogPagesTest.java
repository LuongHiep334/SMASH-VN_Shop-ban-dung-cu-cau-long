package com.smashvn.shop.controller.blog;

import com.smashvn.shop.entity.Blog;
import com.smashvn.shop.entity.BlogStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.BlogRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class BlogPagesTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    private MockMvc mockMvc;
    private Blog publishedBlog;
    private Blog draftBlog;
    private TaiKhoan user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        user = new TaiKhoan();
        user.setUsername("admin_blog_tester@smashvn.com");
        user.setMatKhau("123456");
        user.setVaiTro("QL");
        user.setTrangThai("hoat_dong");
        user = taiKhoanRepository.save(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_QL"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        publishedBlog = Blog.builder()
                .title("Bài viết thử nghiệm đã xuất bản")
                .slug("bai-viet-thu-nghiem-da-xuat-ban")
                .summary("Tóm tắt bài viết thử nghiệm")
                .content("<p>Nội dung bài viết thử nghiệm...</p>")
                .image("/images/blog/blog-1.jpg")
                .publishDate(LocalDate.now())
                .author("Tác Giả Test")
                .category("Đánh giá vợt")
                .tags("vot,yonex,cau long")
                .status(BlogStatus.PUBLISHED)
                .deleted(false)
                .nguoiDang(user)
                .createdAt(LocalDateTime.now())
                .build();
        publishedBlog = blogRepository.save(publishedBlog);

        draftBlog = Blog.builder()
                .title("Bài viết bản nháp")
                .slug("bai-viet-ban-nhap")
                .summary("Tóm tắt bài viết bản nháp")
                .content("<p>Nội dung bài viết bản nháp...</p>")
                .image("/images/blog/blog-2.jpg")
                .publishDate(LocalDate.now())
                .author("Tác Giả Nháp")
                .category("Kỹ thuật")
                .tags("ky thuat,tap luyen")
                .status(BlogStatus.DRAFT)
                .deleted(false)
                .nguoiDang(user)
                .createdAt(LocalDateTime.now())
                .build();
        draftBlog = blogRepository.save(draftBlog);
    }

    @Test
    void testPublicBlogList() throws Exception {
        mockMvc.perform(get("/blog"))
                .andExpect(status().isOk());
    }

    @Test
    void testPublicBlogDetail() throws Exception {
        mockMvc.perform(get("/blog/" + publishedBlog.getSlug()))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminBlogList() throws Exception {
        mockMvc.perform(get("/admin/blog")
                .sessionAttr("vaiTro", "QL")
                .sessionAttr("nguoiDungDangNhap", "admin_blog_tester@smashvn.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminBlogPreview() throws Exception {
        mockMvc.perform(get("/admin/blog/preview/" + publishedBlog.getId())
                .sessionAttr("vaiTro", "QL")
                .sessionAttr("nguoiDungDangNhap", "admin_blog_tester@smashvn.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminBlogPreviewDraft() throws Exception {
        mockMvc.perform(get("/admin/blog/preview/" + draftBlog.getId())
                .sessionAttr("vaiTro", "QL")
                .sessionAttr("nguoiDungDangNhap", "admin_blog_tester@smashvn.com"))
                .andExpect(status().isOk());
    }
}
