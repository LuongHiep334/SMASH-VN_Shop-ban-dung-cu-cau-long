package com.smashvn.shop.dto.chatbot;

import lombok.Data;

@Data
public class ChatFeedbackRequest {
    private Long messageId;
    private Integer rating;
    private String note;
}
