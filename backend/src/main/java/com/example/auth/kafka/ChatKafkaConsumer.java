package com.example.auth.kafka;

import com.example.auth.dto.chat.ChatMessageRequestDto;
import com.example.auth.dto.chat.ChatMessageResponseDto;
import com.example.auth.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @KafkaListener(topics = "chat-messages", groupId = "chat-group")
    public void consume(String messageStr) {
        try {
            ChatMessagePayload payload = objectMapper.readValue(messageStr, ChatMessagePayload.class);
            log.info("Consumed message from Kafka: {}", payload.getContent());
            // 1. DB에 저장 (비동기로 안전하게)
            ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
            // 리플렉션으로 필드주입 하거나 DTO 생성자 추가 필요. 여기서는 Service를 위해 DTO 생성 로직 우회
            // 임시용으로 DTO 재구성:
            requestDto = new ChatMessageRequestDto(payload.getChatRoomId(), payload.getContent());
            
            ChatMessageResponseDto responseDto = chatService.saveMessage(requestDto, payload.getSenderEmail());

            // 2. STOMP 클라이언트들에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/rooms/" + payload.getChatRoomId(), responseDto);
        } catch (Exception e) {
            log.error("Failed to process chat message from Kafka", e);
        }
    }
}
