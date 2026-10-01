package com.smashvn.shop.dto.chatbot;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private String role;
    private String senderType; // mapped to USER/BOT for UI compatibility
    private String content;
    private LocalDateTime createdAt;
    private String thoiGian; // formatted time e.g. "15:30"
    private String status;
    private List<ChatProductResponse> suggestedProducts;
    private boolean requiresHumanSupport;
    private ShopContactDto contact;
}
