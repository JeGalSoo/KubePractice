package com.example.chat.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagePayload {
    private Long chatRoomId;
    private String senderEmail;
    private String senderName;
    private String content;
    private String sentAt;
    private java.util.List<String> notifiedEmails;
    private String type;
    private String senderProfileImageUrl;
}
