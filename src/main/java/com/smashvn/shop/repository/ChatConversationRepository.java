package com.smashvn.shop.repository;

import com.smashvn.shop.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findAllByTaiKhoanIdAndTrangThai(Integer idTaiKhoan, String trangThai);
    List<ChatConversation> findAllBySessionIdAndTrangThai(String sessionId, String trangThai);
}
