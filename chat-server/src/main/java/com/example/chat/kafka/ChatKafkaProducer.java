package com.example.chat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "chat-messages";
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public void sendMessage(ChatMessagePayload payload) {
        try {
            log.info("Produce message to Kafka from WebFlux: {}", payload.getContent());
            kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to send message to Kafka from WebFlux", e);
        }
    }
}
