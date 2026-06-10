package com.example.auth.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDto {
    private Long chatRoomId;
    private String content;
}
