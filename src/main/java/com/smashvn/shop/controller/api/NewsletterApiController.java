package com.smashvn.shop.controller.api;

import com.smashvn.shop.exception.NewsletterValidationException;
import com.smashvn.shop.service.NewsletterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class NewsletterApiController {

    private final NewsletterService newsletterService;
    private final com.smashvn.shop.repository.NewsletterSubscriberRepository subscriberRepository;

    public static class SubscribeRequest {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @PostMapping("/api/newsletter/subscribe")
    @ResponseBody
    public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
        newsletterService.subscribe(request.getEmail());
        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng ký nhận ưu đãi thành công!"));
    }

    @GetMapping("/api/newsletter/unsubscribe")
    public String unsubscribe(@RequestParam("token") String token, Model model) {
        try {
            newsletterService.unsubscribe(token);
            return "unsubscribe-success";
        } catch (NewsletterValidationException e) {
            log.warn("[Newsletter] Validation failed during unsubscribe: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "unsubscribe-error";
        } catch (Exception e) {
            log.error("[Newsletter] Unexpected error during unsubscribe: ", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra trong hệ thống, vui lòng thử lại sau.");
            return "unsubscribe-error";
        }
    }

    @PostMapping("/api/newsletter/unsubscribe-ajax")
    @ResponseBody
    public ResponseEntity<?> unsubscribeAjax(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email không được để trống!"));
        }
        String normalizedEmail = email.trim().toLowerCase();
        java.util.Optional<com.smashvn.shop.entity.NewsletterSubscriber> opt = subscriberRepository.findByEmail(normalizedEmail);
        if (opt.isPresent()) {
            com.smashvn.shop.entity.NewsletterSubscriber sub = opt.get();
            newsletterService.unsubscribe(sub.getTokenHuy());
            return ResponseEntity.ok(Map.of("success", true, "message", "Hủy đăng ký nhận bản tin thành công!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email chưa đăng ký bản tin!"));
        }
    }

    @ExceptionHandler(NewsletterValidationException.class)
    @ResponseBody
    public ResponseEntity<?> handleValidationException(NewsletterValidationException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<?> handleAllOtherExceptions(Exception e) {
        log.error("[Newsletter API] Unhandled exception: ", e);
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Có lỗi xảy ra trong hệ thống, vui lòng thử lại sau."));
    }
}
