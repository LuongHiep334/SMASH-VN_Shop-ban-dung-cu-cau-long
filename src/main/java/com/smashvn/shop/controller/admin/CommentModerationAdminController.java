package com.smashvn.shop.controller.admin;

import com.smashvn.shop.entity.CommentModerationKeyword;
import com.smashvn.shop.repository.CommentModerationKeywordRepository;
import com.smashvn.shop.service.blog.CommentModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/moderation/keywords")
@RequiredArgsConstructor
@Slf4j
public class CommentModerationAdminController {

    private final CommentModerationKeywordRepository keywordRepository;
    private final CommentModerationService commentModerationService;

    @GetMapping
    public String listKeywords(Model model) {
        List<CommentModerationKeyword> list = keywordRepository.findAll();
        model.addAttribute("keywords", list);
        model.addAttribute("activeTab", "moderationKeywords");
        return "admin/moderation-keywords";
    }

    @PostMapping("/add")
    public String addKeyword(
            @RequestParam("keyword") String keyword,
            RedirectAttributes redirectAttributes) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new IllegalArgumentException("Từ khóa không được để trống!");
            }
            String cleaned = Jsoup.clean(keyword.trim(), Safelist.none());
            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException("Từ khóa không hợp lệ!");
            }
            
            // Save to DB
            CommentModerationKeyword kw = CommentModerationKeyword.builder()
                    .keyword(cleaned)
                    .active(true)
                    .build();
            keywordRepository.save(kw);
            
            // Clear Cache
            commentModerationService.clearKeywordCache();
            redirectAttributes.addFlashAttribute("successMsg", "Thêm từ khóa mới thành công!");
        } catch (Exception e) {
            log.error("[KEYWORD_ADMIN] Failed to add keyword: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/moderation/keywords";
    }

    @PostMapping("/edit/{id}")
    public String editKeyword(
            @PathVariable("id") Integer id,
            @RequestParam("keyword") String keyword,
            RedirectAttributes redirectAttributes) {
        try {
            CommentModerationKeyword kw = keywordRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Từ khóa không tồn tại."));
            
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new IllegalArgumentException("Từ khóa không được để trống!");
            }
            String cleaned = Jsoup.clean(keyword.trim(), Safelist.none());
            if (cleaned.isEmpty()) {
                throw new IllegalArgumentException("Từ khóa không hợp lệ!");
            }
            
            kw.setKeyword(cleaned);
            keywordRepository.save(kw);
            
            // Clear Cache
            commentModerationService.clearKeywordCache();
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật từ khóa thành công!");
        } catch (Exception e) {
            log.error("[KEYWORD_ADMIN] Failed to edit keyword ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/moderation/keywords";
    }

    @PostMapping("/toggle/{id}")
    public String toggleKeyword(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            CommentModerationKeyword kw = keywordRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Từ khóa không tồn tại."));
            
            kw.setActive(!kw.getActive());
            keywordRepository.save(kw);
            
            // Clear Cache
            commentModerationService.clearKeywordCache();
            String status = kw.getActive() ? "kích hoạt" : "vô hiệu hóa";
            redirectAttributes.addFlashAttribute("successMsg", "Đã " + status + " từ khóa thành công!");
        } catch (Exception e) {
            log.error("[KEYWORD_ADMIN] Failed to toggle keyword ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/moderation/keywords";
    }

    @PostMapping("/delete/{id}")
    public String deleteKeyword(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            CommentModerationKeyword kw = keywordRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Từ khóa không tồn tại."));
            
            keywordRepository.delete(kw);
            
            // Clear Cache
            commentModerationService.clearKeywordCache();
            redirectAttributes.addFlashAttribute("successMsg", "Xóa từ khóa thành công!");
        } catch (Exception e) {
            log.error("[KEYWORD_ADMIN] Failed to delete keyword ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/moderation/keywords";
    }
}
