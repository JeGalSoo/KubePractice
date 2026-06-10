package com.example.auth.controller;

import com.example.auth.dto.chat.ChatMessageResponseDto;
import com.example.auth.dto.chat.ChatRoomResponseDto;
import com.example.auth.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 현재 인증된 사용자가 참여 중인 채팅방 목록을 반환합니다.
     * @param userDetails 스프링 시큐리티의 사용자 정보
     * @return 사용자의 채팅방 목록
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getMyRooms(userDetails.getUsername()));
    }

    /**
     * 시스템 전체의 채팅방 목록을 조회합니다 (참여 및 검색 용도).
     * @return 전체 채팅방 목록
     */
    @GetMapping("/rooms/all")
    public ResponseEntity<List<ChatRoomResponseDto>> getAllRooms() {
        return ResponseEntity.ok(chatService.getAllRooms());
    }

    /**
     * 새로운 채팅방을 생성합니다.
     * @param request 생성할 채팅방 정보 (name, isGroupChat)
     * @param userDetails 방 생성자의 인증 정보
     * @return 생성된 채팅방 정보
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponseDto> createRoom(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String name = (String) request.get("name");
        boolean isGroupChat = (Boolean) request.getOrDefault("isGroupChat", false);
        return ResponseEntity.ok(chatService.createRoom(name, isGroupChat, userDetails.getUsername()));
    }

    /**
     * 기존 채팅방에 현재 사용자를 논리적으로 입장(참여)시킵니다.
     * @param roomId 입장할 채팅방 ID
     * @param userDetails 입장하는 사용자 정보
     * @return 상태코드 200 OK
     */
    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<Void> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.joinRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 사용자를 기존 채팅방에 초대(입장)시킵니다.
     * @param roomId 채팅방 ID
     * @param request 초대할 사용자의 이메일
     * @return 상태코드 200 OK
     */
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Void> inviteUser(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request) {
        String targetEmail = request.get("email");
        chatService.joinRoom(roomId, targetEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 사용자와 1:1 채팅방을 생성하고, 둘 다 즉시 입장시킵니다.
     * @param request 초대할 사용자의 이메일
     * @param userDetails 방 생성자의 인증 정보
     * @return 생성된 채팅방 정보
     */
    @PostMapping("/rooms/direct")
    public ResponseEntity<ChatRoomResponseDto> createDirectRoom(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String targetEmail = request.get("email");
        String myEmail = userDetails.getUsername();
        
        // 방 이름은 임시로 1:1 채팅으로 지정
        ChatRoomResponseDto room = chatService.createRoom("1:1 Chat with " + targetEmail, false, myEmail);
        chatService.joinRoom(room.getId(), targetEmail);
        
        return ResponseEntity.ok(room);
    }

    /**
     * 특정 채팅방의 과거 메시지 내역을 페이징하여 조회합니다.
     * @param roomId 조회할 채팅방 ID
     * @param pageable 페이징 인자(page, size, sort)
     * @param userDetails 요청 사용자 정보
     * @return 메시지 페이징 응답 객체
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<org.springframework.data.domain.Page<ChatMessageResponseDto>> getMessages(
            @PathVariable Long roomId,
            org.springframework.data.domain.Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getMessages(roomId, userDetails.getUsername(), pageable));
    }

    /**
     * 특정 이메일 경로의 사용자가 현재 온라인 상태인지 여부를 반환합니다.
     * @param email 유저 이메일
     * @return 온라인 상태면 true, 아니면 false
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable String email) {
        return ResponseEntity.ok(chatService.isUserOnline(email));
    }

    /**
     * 특정 채팅방에 대한 알림 수신 여부를 토글하거나 설정합니다.
     * @param roomId 대상 채팅방 ID
     * @param requestDto 알림 켜기/끄기 설정값
     * @param userDetails 요청한 사용자 정보
     * @return 상태코드 200 OK
     */
    @PatchMapping("/rooms/{roomId}/notification")
    public ResponseEntity<Void> toggleNotification(
            @PathVariable Long roomId,
            @RequestBody com.example.auth.dto.chat.ChatRoomNotificationRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.toggleNotification(roomId, userDetails.getUsername(), requestDto.isEnabled());
        return ResponseEntity.ok().build();
    }
}
