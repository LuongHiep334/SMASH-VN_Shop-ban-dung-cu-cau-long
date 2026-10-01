package com.smashvn.shop.controller.api;

import com.smashvn.shop.dto.chatbot.ChatbotProductSearchResponseDto;
import com.smashvn.shop.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotProductsRestController {

    private final ChatbotService chatbotService;

    @GetMapping("/products")
    public ResponseEntity<ChatbotProductSearchResponseDto> getProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "limit", required = false) Integer limit) {
        ChatbotProductSearchResponseDto response = chatbotService.searchProductsApi(keyword, category, brand, minPrice, maxPrice, limit);
        return ResponseEntity.ok(response);
    }
}
