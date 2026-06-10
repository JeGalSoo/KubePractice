package com.example.chat.kafka;

import com.example.chat.handler.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "chat-messages", groupId = "chat-netty-group")
    public void consume(String messageStr) {
        try {
            log.info("Consumed message from Kafka in WebFlux Server: {}", messageStr);
            ChatMessagePayload payload = objectMapper.readValue(messageStr, ChatMessagePayload.class);
            
            // Broadcast to all connected WebSocket sessions on this node
            chatWebSocketHandler.broadcastFromKafka(payload);
        } catch (Exception e) {
            log.error("Failed to parse Kafka message in WebFlux server", e);
        }
    }
}
