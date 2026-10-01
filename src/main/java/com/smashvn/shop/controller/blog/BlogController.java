package com.smashvn.shop.controller.blog;

import com.smashvn.shop.dto.blog.BlogDTO;
import com.smashvn.shop.dto.blog.BlogCommentDTO;
import com.smashvn.shop.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/blog")
    public String showBlogList(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "3") int size,
            Model model) {

        // Giới hạn chỉ số trang tối thiểu là 0
        if (page < 0) page = 0;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogDTO> blogsPage = blogService.getBlogs(query, pageable);
        List<BlogDTO> recentBlogs = blogService.getRecentBlogs(3);

        model.addAttribute("blogsPage", blogsPage);
        model.addAttribute("query", query);
        model.addAttribute("recentBlogs", recentBlogs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogsPage.getTotalPages());

        return "blog";
    }

    @GetMapping("/blog/{slug}")
    public String showBlogDetail(
            @PathVariable("slug") String slug,
            Model model) {

        try {
            BlogDTO blog = blogService.getBlogBySlug(slug);
            List<BlogDTO> recentBlogs = blogService.getRecentBlogs(3);

            String prevSlug = null;
            String nextSlug = null;
            if (blog.getPublishDate() != null && !blog.getPublishDate().isEmpty()) {
                java.time.LocalDate publishDate = java.time.LocalDate.parse(blog.getPublishDate());
                prevSlug = blogService.getPreviousBlogSlug(publishDate, blog.getId());
                nextSlug = blogService.getNextBlogSlug(publishDate, blog.getId());
            }

            List<BlogCommentDTO> comments = blogService.getCommentsForBlog(blog.getId());
            model.addAttribute("comments", comments);

            model.addAttribute("blog", blog);
            model.addAttribute("recentBlogs", recentBlogs);
            model.addAttribute("prevBlogSlug", prevSlug);
            model.addAttribute("nextBlogSlug", nextSlug);
            return "blog-detail";
        } catch (Exception e) {
            // Nếu không tìm thấy slug bài viết, trả về trang lỗi 404
            return "404";
        }
    }

    @PostMapping("/blog/{slug}/comment")
    public String addComment(
            @PathVariable("slug") String slug,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("nguoiDungDangNhap");
        if (email == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName();
            }
        }

        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Bạn cần đăng nhập để bình luận.");
            return "redirect:/blog/" + slug;
        }

        try {
            blogService.addComment(slug, content, email);
            redirectAttributes.addFlashAttribute("successMsg", "Bình luận của bạn đã được đăng thành công.");
        } catch (Exception e) {
            log.error("[BLOG_CONTROLLER] Error adding comment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            // Pre-fill the content in flash attribute to let user edit it in case of validation error
            redirectAttributes.addFlashAttribute("commentContent", content);
        }

        return "redirect:/blog/" + slug;
    }

    @PostMapping("/blog/comment/delete/{id}")
    public String deleteComment(
            @PathVariable("id") Integer commentId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam("blogSlug") String blogSlug,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("nguoiDungDangNhap");
        if (email == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName();
            }
        }

        if (email == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Thao tác không hợp lệ.");
            return "redirect:/blog/" + blogSlug;
        }

        try {
            blogService.deleteComment(commentId, email, reason);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa bình luận thành công.");
        } catch (Exception e) {
            log.error("[BLOG_CONTROLLER] Error deleting comment ID {}: {}", commentId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/blog/" + blogSlug;
    }
}
