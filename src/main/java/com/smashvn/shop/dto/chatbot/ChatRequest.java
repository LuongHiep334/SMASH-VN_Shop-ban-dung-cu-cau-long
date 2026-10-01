package com.smashvn.shop.dto.chatbot;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private Long conversationId;

    public void setContent(String content) {
        this.message = content;
    }

    public String getContent() {
        return this.message;
    }
}
