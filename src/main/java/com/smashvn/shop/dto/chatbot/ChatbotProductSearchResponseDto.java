package com.smashvn.shop.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotProductSearchResponseDto {
    private boolean success;
    private long total;
    private int displayed;
    private List<ChatProductResponse> products;
}
