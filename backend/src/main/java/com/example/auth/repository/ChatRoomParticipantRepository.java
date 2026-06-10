package com.example.auth.repository;

import com.example.auth.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {
    List<ChatRoomParticipant> findByUserId(Long userId);
    List<ChatRoomParticipant> findByChatRoomId(Long chatRoomId);
    java.util.Optional<ChatRoomParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
