package com.smashvn.shop.service;

import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ChatbotProductSearchResponseDto;
import java.math.BigDecimal;
import java.util.List;

public interface ChatbotService {
    ChatMessageDto sendMessage(ChatRequest request, Integer idTaiKhoan, String sessionId);
    List<ChatMessageDto> getConversationHistory(Long conversationId, Integer idTaiKhoan, String sessionId);
    void submitFeedback(ChatFeedbackRequest request, Integer idTaiKhoan, String sessionId);
    ChatbotProductSearchResponseDto searchProductsApi(String keyword, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, Integer limit);
}
