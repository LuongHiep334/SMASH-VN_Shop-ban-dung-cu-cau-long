package com.smashvn.shop.dto.chatbot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long messageId;
    private Long conversationId;
    private String message;
    private String status;
    private String time;
    private List<ChatProductResponse> products;
    private boolean requiresHumanSupport;
    private ShopContactDto contact;
}
