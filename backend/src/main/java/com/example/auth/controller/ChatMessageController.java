package com.example.auth.controller;

import com.example.auth.dto.chat.ChatMessageRequestDto;
import com.example.auth.kafka.ChatKafkaProducer;
import com.example.auth.kafka.ChatMessagePayload;
import com.example.auth.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatKafkaProducer chatKafkaProducer;
    private final ChatService chatService;

    /**
     * 클라이언트가 웹소켓을 통해 메시지를 전송할 때 호출되는 핸들러입니다. (/app/chat.send)
     * 메시지를 직접 DB에 저장하지 않고 카프카(Kafka) 토픽으로 발행하여 비동기 처리합니다.
     * @param requestDto 송신할 메시지 데이터 (방 번호, 내용 등)
     * @param authentication 웹소켓 세션에 바인딩된 사용자 인증 정보
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequestDto requestDto, Authentication authentication) {
        String email = authentication.getName();
        String senderName = chatService.getUserName(email);
        String profileImageUrl = chatService.getUserProfileImageUrl(email);
        java.util.List<String> notifiedEmails = chatService.getNotifiedUsers(requestDto.getChatRoomId());
        
        // 메시지를 바로 DB에 넣는 대신 Kafka로 발행 (빠른 응답)
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .chatRoomId(requestDto.getChatRoomId())
                .senderEmail(email)
                .senderName(senderName)
                .content(requestDto.getContent())
                .sentAt(java.time.Instant.now().toString())
                .notifiedEmails(notifiedEmails)
                .type("MESSAGE")
                .senderProfileImageUrl(profileImageUrl)
                .build();
                
        chatKafkaProducer.sendMessage(payload);
    }

    /**
     * 클라이언트가 온라인 접속 상태를 유지하기 위해 1분 간격으로 보내는 Heartbeat(Ping)를 처리합니다.
     * Redis에 사용자의 세션 만료 시간을 갱신합니다.
     * @param authentication 사용자 인증 정보
     */
    @MessageMapping("/chat.ping")
    public void receivePing(Authentication authentication) {
        String email = authentication.getName();
        chatService.updateOnlineStatus(email);
        log.debug("Received ping from: {}. Status updated in Redis.", email);
    }
}
