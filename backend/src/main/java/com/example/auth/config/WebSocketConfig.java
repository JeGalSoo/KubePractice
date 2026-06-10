package com.example.auth.config;

//import com.example.auth.security.JwtUtil;
import com.example.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * 클라이언트가 최초로 웹소켓 서버에 연결할 엔드포인트를 등록합니다.
     * @param registry STOMP 엔드포인트 설정 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ws://localhost:8080/ws/chat 으로 연결
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*"); // 실제 배포시 프론트엔드 도메인 지정 권장
    }

    /**
     * 메시지를 브로커가 라우팅할 때 사용할 prefix를 설정합니다.
     * @param registry 메시지 브로커 설정 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /app 은 클라이언트에서 서버로 메시지를 보낼 때 사용하는 prefix (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
        // /topic 은 다대다 채팅방, /queue 는 1:1 메시지 등 구독(subscribe) prefix
        registry.enableSimpleBroker("/topic", "/queue");
    }

    /**
     * 클라이언트에서 수신되는 메시지의 채널을 가로채서(intercept) 헤더의 JWT를 검증하고 인증 정보를 설정합니다.
     * @param registration 채널 인터셉터 등록 공간
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.validateToken(token)) {
                            String email = jwtUtil.getEmailFromToken(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            accessor.setUser(authentication);
                            log.info("WebSocket Authenticated: {}", email);
                        } else {
                            log.error("Invalid JWT Token in WebSocket CONNECT");
                        }
                    }
                }
                return message;
            }
        });
    }
}
