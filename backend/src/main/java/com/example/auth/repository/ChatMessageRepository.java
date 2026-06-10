package com.example.auth.repository;

import com.example.auth.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
    org.springframework.data.domain.Page<ChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, org.springframework.data.domain.Pageable pageable);
}
