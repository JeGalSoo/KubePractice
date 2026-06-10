package com.example.chat.handler;

import com.example.chat.kafka.ChatKafkaProducer;
import com.example.chat.kafka.ChatMessagePayload;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatKafkaProducer kafkaProducer;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Global broadcasting sink. All incoming Kafka messages are dumped here.
    private final Sinks.Many<ChatMessagePayload> chatSink = Sinks.many().multicast().directBestEffort();

    // Mapping session ID to roomId
    private final Map<String, Long> sessionRoomMap = new ConcurrentHashMap<>();
    // Mapping session ID to User Email
    private final Map<String, String> sessionEmailMap = new ConcurrentHashMap<>();

    // Called by KafkaConsumer when a new message arrives from Kafka
    public void broadcastFromKafka(ChatMessagePayload payload) {
        chatSink.tryEmitNext(payload);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Parse roomId and token from query param: ws://localhost/ws/chat?roomId=1&token=...
        String query = session.getHandshakeInfo().getUri().getQuery();
        Long configuredRoomId = parseParam(query, "roomId") != null ? Long.parseLong(parseParam(query, "roomId")) : null;
        String token = parseParam(query, "token");
        
        String email = null;
        if (token != null && jwtUtil.validateToken(token)) {
            email = jwtUtil.extractEmail(token);
            sessionEmailMap.put(session.getId(), email);
        }

        if (configuredRoomId != null) {
            sessionRoomMap.put(session.getId(), configuredRoomId);
        }

        final String finalEmail = email;

        Mono<Void> output = session.send(
            chatSink.asFlux()
                .filter(payload -> {
                    // 1. 방에 참여 중이면 무조건 보냄
                    if (configuredRoomId != null && configuredRoomId.equals(payload.getChatRoomId())) {
                        return true;
                    }
                    // 2. 다른 방에 있거나 메인 페이지에 있더라도, 알림 목록에 포함되어 있으면 보냄
                    if (finalEmail != null && payload.getNotifiedEmails() != null && payload.getNotifiedEmails().contains(finalEmail)) {
                        return true;
                    }
                    return false;
                })
                .map(payload -> {
                    try {
                        // 만약 현재 방이 아닌 곳에서 온 메시지라면 타입을 NOTIFICATION으로 변경하여 전달 전송
                        if (configuredRoomId == null || !configuredRoomId.equals(payload.getChatRoomId())) {
                            ChatMessagePayload notification = ChatMessagePayload.builder()
                                    .chatRoomId(payload.getChatRoomId())
                                    .senderEmail(payload.getSenderEmail())
                                    .senderName(payload.getSenderName())
                                    .content(payload.getContent())
                                    .sentAt(payload.getSentAt())
                                    .type("NOTIFICATION")
                                    .senderProfileImageUrl(payload.getSenderProfileImageUrl())
                                    .build();
                            return session.textMessage(objectMapper.writeValueAsString(notification));
                        }
                        return session.textMessage(objectMapper.writeValueAsString(payload));
                    } catch (JsonProcessingException e) {
                        return session.textMessage("{}");
                    }
                })
        );

        Mono<Void> input = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .doOnNext(msgText -> {
                try {
                    ChatMessagePayload payload = objectMapper.readValue(msgText, ChatMessagePayload.class);
                    // sentAt이 없으면 서버 수신 시각으로 채워 일관성 유지
                    if (payload.getSentAt() == null || payload.getSentAt().isBlank()) {
                        payload = ChatMessagePayload.builder()
                                .chatRoomId(payload.getChatRoomId())
                                .senderEmail(payload.getSenderEmail())
                                .senderName(payload.getSenderName())
                                .content(payload.getContent())
                                .sentAt(Instant.now().toString())
                                .notifiedEmails(payload.getNotifiedEmails())
                                .type(payload.getType() != null ? payload.getType() : "MESSAGE")
                                .senderProfileImageUrl(payload.getSenderProfileImageUrl())
                                .build();
                    }
                    kafkaProducer.sendMessage(payload);
                } catch (Exception e) {
                    log.error("Failed to parse incoming WS message: {}", msgText, e);
                }
            })
            .doFinally(sig -> {
                sessionRoomMap.remove(session.getId());
                sessionEmailMap.remove(session.getId());
            })
            .then();

        return Mono.when(output, input);
    }

    private String parseParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && paramName.equals(pair[0])) {
                return pair[pair.length - 1];
            }
        }
        return null;
    }

    private Long parseRoomId(String query) {
        String val = parseParam(query, "roomId");
        return val != null ? Long.parseLong(val) : null;
    }
}
