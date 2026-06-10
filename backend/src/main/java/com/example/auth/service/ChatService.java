package com.example.auth.service;

import com.example.auth.dto.chat.ChatMessageRequestDto;
import com.example.auth.dto.chat.ChatMessageResponseDto;
import com.example.auth.dto.chat.ChatRoomResponseDto;
import com.example.auth.entity.ChatMessage;
import com.example.auth.entity.ChatRoom;
import com.example.auth.entity.ChatRoomParticipant;
import com.example.auth.entity.User;
import com.example.auth.repository.ChatMessageRepository;
import com.example.auth.repository.ChatRoomParticipantRepository;
import com.example.auth.repository.ChatRoomRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 새로운 채팅방을 생성합니다.
     * @param name 채팅방 이름
     * @param isGroupChat 그룹 채팅 여부
     * @param email 생성자 이메일
     * @return 생성된 채팅방 응답 DTO
     */
    @Transactional
    public ChatRoomResponseDto createRoom(String name, boolean isGroupChat, String email) {
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .isGroupChat(isGroupChat)
                .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        ChatRoomParticipant participant = ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .user(creator)
                .build();
        chatRoomParticipantRepository.save(participant);

        return ChatRoomResponseDto.from(chatRoom);
    }

    /**
     * 사용자를 특정 채팅방에 참여시킵니다.
     * @param roomId 참여할 채팅방 ID
     * @param email 사용자 이메일
     */
    @Transactional
    public void joinRoom(Long roomId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!chatRoomParticipantRepository.existsByChatRoomIdAndUserId(roomId, user.getId())) {
            ChatRoomParticipant participant = ChatRoomParticipant.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .build();
            chatRoomParticipantRepository.save(participant);
        }
    }

    /**
     * 사용자가 참여 중인 채팅방 목록을 조회합니다.
     * @param email 사용자 이메일
     * @return 참여 중인 채팅방 목록 DTO
     */
    public List<ChatRoomResponseDto> getMyRooms(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        return chatRoomParticipantRepository.findByUserId(user.getId()).stream()
                .map(p -> ChatRoomResponseDto.from(p.getChatRoom()))
                .collect(Collectors.toList());
    }

    /**
     * 공개 그룹 채팅방 목록을 조회합니다 (참여/검색 용도, 인증 사용자 누구나 접근 가능).
     * @return 그룹 채팅방 목록 DTO
     */
    public List<ChatRoomResponseDto> getAllRooms() {
        return chatRoomRepository.findAll().stream()
                .filter(ChatRoom::isGroupChat)
                .map(ChatRoomResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 채팅방의 과거 메시지들을 페이징하여 조회합니다.
     * @param roomId 채팅방 ID
     * @param email 사용자 이메일
     * @param pageable 페이징 정보
     * @return 페이징된 메시지 응답 DTO 목록
     */
    public org.springframework.data.domain.Page<ChatMessageResponseDto> getMessages(Long roomId, String email, org.springframework.data.domain.Pageable pageable) {
        return chatMessageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageable)
                .map(ChatMessageResponseDto::from);
    }

    /**
     * 사용자가 보낸 채팅 메시지를 데이터베이스에 저장합니다.
     * @param requestDto 송신할 메시지 정보 (chatRoomId, content 등)
     * @param email 발신자 이메일
     * @return 저장된 메시지 DTO
     */
    @Transactional
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto requestDto, String email) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(requestDto.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(requestDto.getContent())
                .build();

        message = chatMessageRepository.save(message);
        return ChatMessageResponseDto.from(message);
    }

    /**
     * 클라이언트로부터 Ping을 받아 현재 상태를 Redis에 TTL 70초로 업데이트합니다 (온라인 상태 유지).
     * @param email 사용자 이메일
     */
    public void updateOnlineStatus(String email) {
        redisTemplate.opsForValue().set("user:online:" + email, "ONLINE", Duration.ofSeconds(70));
    }

    /**
     * Redis를 조회하여 해당 유저가 현재 접속(온라인) 상태인지 확인합니다.
     * @param email 사용자 이메일
     * @return 온라인 여부 (true/false)
     */
    public boolean isUserOnline(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("user:online:" + email));
    }

    // 유저 이름 조회
    public String getUserName(String email) {
        return userRepository.findByEmail(email)
                .map(User::getName)
                .orElse("Unknown");
    }

    // 유저 프로필 이미지 URL 조회 (설정된 타입에 따라 다른 URL 반환)
    public String getUserProfileImageUrl(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    String type = user.getActiveProfileType();
                    if ("CUSTOM".equals(type)) {
                        return user.getProfileImageUrl();
                    } else if (type != null && type.startsWith("DEFAULT_")) {
                        // 예: /assets/images/profiles/default_1.png
                        return "/assets/images/profiles/" + type.toLowerCase() + ".png";
                    }
                    return "/assets/images/profiles/default_1.png"; // 기본값
                })
                .orElse("/assets/images/profiles/default_1.png");
    }

    @Transactional
    public void toggleNotification(Long roomId, String email, boolean enabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        ChatRoomParticipant participant = chatRoomParticipantRepository.findByChatRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 참여자가 아닙니다."));
        
        participant.setNotificationEnabled(enabled);
    }

    public List<String> getNotifiedUsers(Long roomId) {
        return chatRoomParticipantRepository.findByChatRoomId(roomId).stream()
                .filter(ChatRoomParticipant::isNotificationEnabled)
                .map(p -> p.getUser().getEmail())
                .collect(Collectors.toList());
    }
}
