package com.smashvn.shop.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ChatResponse;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.ChatbotService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatbotRestController {

    private final ChatbotService chatbotService;
    private final TaiKhoanRepository taiKhoanRepository;

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId(session);
        String sessionId = resolveSessionId(session);

        List<ChatMessageDto> history = chatbotService.getConversationHistory(conversationId, idTaiKhoan, sessionId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId(session);
        String sessionId = resolveSessionId(session);

        ChatMessageDto response = chatbotService.sendMessage(request, idTaiKhoan, sessionId);
        return ResponseEntity.ok(ChatResponse.builder()
                .messageId(response.getId())
                .conversationId(response.getConversationId())
                .message(response.getContent())
                .status(response.getStatus())
                .time(response.getThoiGian())
                .products(response.getSuggestedProducts() == null ? List.of() : response.getSuggestedProducts())
                .requiresHumanSupport(response.isRequiresHumanSupport())
                .contact(response.getContact())
                .build());
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(
            @RequestBody ChatFeedbackRequest request,
            HttpSession session) {
        Integer idTaiKhoan = resolveTaiKhoanId(session);
        String sessionId = resolveSessionId(session);

        try {
            chatbotService.submitFeedback(request, idTaiKhoan, sessionId);
            return ResponseEntity.ok("Cảm ơn bạn đã phản hồi!");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private Integer resolveTaiKhoanId(HttpSession session) {
        // Customer authentication in this application is session based, so the
        // session must be checked before Spring Security (which is normally
        // anonymous for storefront customers).
        Object sessionAccountId = session.getAttribute("idNguoiDung");
        if (sessionAccountId instanceof Integer accountId
                && taiKhoanRepository.existsById(accountId)) {
            return accountId;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            TaiKhoan tk = taiKhoanRepository.findByUsername(username);
            if (tk != null) {
                return tk.getId();
            }
        }
        return null;
    }

    private String resolveSessionId(HttpSession session) {
        String guestSessionId = (String) session.getAttribute("guestSessionId");
        if (guestSessionId == null) {
            guestSessionId = java.util.UUID.randomUUID().toString();
            session.setAttribute("guestSessionId", guestSessionId);
        }
        return guestSessionId;
    }
}
