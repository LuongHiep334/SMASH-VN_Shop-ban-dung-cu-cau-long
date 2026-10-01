package com.smashvn.shop.service.impl;

import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.repository.ChatConversationRepository;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.ChatFeedbackRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatbotDbHelper {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatConversation saveConversation(ChatConversation conversation) {
        return chatConversationRepository.save(conversation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatFeedback saveFeedback(ChatFeedback feedback) {
        return chatFeedbackRepository.save(feedback);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateConversationTime(Long conversationId) {
        chatConversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setNgayCapNhat(LocalDateTime.now());
            chatConversationRepository.save(conv);
        });
    }
}
