package com.smashvn.shop.controller.admin;

import com.smashvn.shop.dto.blog.BlogDTO;
import com.smashvn.shop.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping({"/admin/blog", "/admin/blogs"})
@RequiredArgsConstructor
@Slf4j
public class AdminBlogController {

    private final BlogService blogService;

    @GetMapping
    public String listBlogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "sortBy", defaultValue = "publishDate") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(value = "showDeleted", defaultValue = "false") boolean showDeleted,
            Model model) {

        if (page < 0) page = 0;
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BlogDTO> blogsPage = blogService.getAdminBlogs(showDeleted, status, category, author, query, pageable);

        model.addAttribute("blogsPage", blogsPage);
        model.addAttribute("query", query);
        model.addAttribute("statusFilter", status);
        model.addAttribute("categoryFilter", category);
        model.addAttribute("authorFilter", author);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("showDeleted", showDeleted);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogsPage.getTotalPages());
        model.addAttribute("activeTab", "blog");

        return "admin/blog-list";
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam(value = "iframe", defaultValue = "false") boolean isIframe,
            Model model) {
        model.addAttribute("blog", new BlogDTO());
        model.addAttribute("activeTab", "blog");
        model.addAttribute("isIframe", isIframe);
        return "admin/blog-add";
    }

    @PostMapping("/add")
    public String processAdd(
            @ModelAttribute("blog") BlogDTO blogDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "iframe", required = false, defaultValue = "false") boolean isIframe,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            if (blogDTO.getTitle() == null || blogDTO.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề bài viết không được để trống!");
            }
            if (blogDTO.getContent() == null || blogDTO.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung bài viết không được để trống!");
            }
            
            blogService.createBlog(blogDTO, imageFile, username);
            if (isIframe) {
                model.addAttribute("success", true);
                model.addAttribute("title", "Thao Tác Thành Công");
                model.addAttribute("message", "Thêm bài viết mới thành công (trạng thái Nháp)!");
                model.addAttribute("isIframe", true);
                return "admin/confirm-result";
            }
            redirectAttributes.addFlashAttribute("successMsg", "Thêm bài viết mới thành công (trạng thái Nháp)!");
            return "redirect:/admin/blog";
        } catch (Exception e) {
            model.addAttribute("blog", blogDTO);
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("activeTab", "blog");
            model.addAttribute("isIframe", isIframe);
            return "admin/blog-add";
        }
    }

    @GetMapping({"/edit/{id}", "/sua/{id}"})
    public String showEditForm(
            @PathVariable("id") Integer id,
            @RequestParam(value = "iframe", defaultValue = "false") boolean isIframe,
            Model model) {
        try {
            BlogDTO blog = blogService.getAdminBlogById(id);
            model.addAttribute("blog", blog);
            model.addAttribute("activeTab", "blog");
            model.addAttribute("isIframe", isIframe);
            return "admin/blog-edit";
        } catch (Exception e) {
            return "redirect:/admin/blog";
        }
    }

    @PostMapping({"/edit/{id}", "/sua/{id}"})
    public String processEdit(
            @PathVariable("id") Integer id,
            @ModelAttribute("blog") BlogDTO blogDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "iframe", required = false, defaultValue = "false") boolean isIframe,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isManager = "QL".equals(session.getAttribute("vaiTro"));
        try {
            if (blogDTO.getTitle() == null || blogDTO.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề bài viết không được để trống!");
            }
            if (blogDTO.getContent() == null || blogDTO.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung bài viết không được để trống!");
            }

            blogService.updateBlog(id, blogDTO, imageFile, username, isManager);
            if (isIframe) {
                model.addAttribute("success", true);
                model.addAttribute("title", "Cập Nhật Thành Công");
                model.addAttribute("message", "Cập nhật bài viết thành công!");
                model.addAttribute("isIframe", true);
                return "admin/confirm-result";
            }
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật bài viết thành công!");
            return "redirect:/admin/blog";
        } catch (Exception e) {
            model.addAttribute("blog", blogDTO);
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("activeTab", "blog");
            model.addAttribute("isIframe", isIframe);
            return "admin/blog-edit";
        }
    }

    @PostMapping("/publish/{id}")
    public String togglePublish(
            @PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("vaiTro");
        if (!"QL".equals(role)) {
            redirectAttributes.addFlashAttribute("warningMsg", "Bạn không có quyền thực hiện chức năng này!");
            return "redirect:/admin/blog";
        }

        try {
            BlogDTO blog = blogService.getAdminBlogById(id);
            String currentStatus = blog.getStatus();
            String newStatus = currentStatus.equals("DRAFT") ? "PUBLISHED" : "DRAFT";
            blog.setStatus(newStatus);
            
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            blogService.updateBlog(id, blog, null, username, true);
            
            String statusMsg = newStatus.equals("PUBLISHED") ? "Xuất bản bài viết thành công!" : "Chuyển bài viết về trạng thái Nháp!";
            redirectAttributes.addFlashAttribute("successMsg", statusMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("warningMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/blog";
    }

    @PostMapping("/delete/{id}")
    public String softDelete(
            @PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("vaiTro");
        if (!"QL".equals(role)) {
            redirectAttributes.addFlashAttribute("warningMsg", "Bạn không có quyền thực hiện chức năng này!");
            return "redirect:/admin/blog";
        }

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            blogService.deleteBlog(id, username);
            redirectAttributes.addFlashAttribute("successMsg", "Xóa bài viết thành công (đã đưa vào hàng chờ xóa sau 90 ngày)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("warningMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/blog";
    }

    @GetMapping("/preview/{id}")
    public String previewBlog(@PathVariable("id") Integer id, Model model) {
        try {
            BlogDTO blog = blogService.getAdminBlogById(id);
            List<BlogDTO> recentBlogs = blogService.getRecentBlogs(3);

            model.addAttribute("blog", blog);
            model.addAttribute("recentBlogs", recentBlogs);
            model.addAttribute("isPreview", true);
            model.addAttribute("previewMode", true);
            
            List<com.smashvn.shop.dto.blog.BlogCommentDTO> comments = blogService.getCommentsForBlog(blog.getId());
            model.addAttribute("comments", comments);
            return "blog-detail";
        } catch (Exception e) {
            return "redirect:/admin/blog";
        }
    }
}
