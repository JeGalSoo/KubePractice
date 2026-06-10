package com.example.auth.dto.chat;

import com.example.auth.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDto {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderEmail;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;

    public static ChatMessageResponseDto from(ChatMessage message) {
        return ChatMessageResponseDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderEmail(message.getSender().getEmail())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}
