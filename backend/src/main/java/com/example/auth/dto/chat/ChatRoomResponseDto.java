package com.example.auth.dto.chat;

import com.example.auth.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponseDto {
    private Long id;
    private String name;
    private boolean isGroupChat;
    private LocalDateTime createdAt;

    public static ChatRoomResponseDto from(ChatRoom room) {
        return ChatRoomResponseDto.builder()
                .id(room.getId())
                .name(room.getName())
                .isGroupChat(room.isGroupChat())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
