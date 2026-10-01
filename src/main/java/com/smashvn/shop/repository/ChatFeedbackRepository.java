package com.smashvn.shop.repository;

import com.smashvn.shop.entity.ChatFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {
    Optional<ChatFeedback> findByMessageIdAndTaiKhoanId(Long messageId, Integer idTaiKhoan);
    Optional<ChatFeedback> findByMessageIdAndSessionId(Long messageId, String sessionId);
}
