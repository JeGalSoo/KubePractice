package com.example.auth.controller;

import com.example.auth.dto.request.FindIdRequest;
import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.request.ResetPasswordRequest;
import com.example.auth.dto.response.AuthResponse;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 신규 사용자 회원가입을 처리합니다.
     * @param request 회원가입 정보 (이메일, 비밀번호 등)
     * @return 성공 메시지 객체
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입이 완료되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     * @param request 로그인 정보 (이메일, 비밀번호)
     * @return 발급된 토큰 및 인증 응답 DTO
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 이름과 등 기타 정보로 이메일(아이디)을 찾습니다.
     * @param request 아이디 찾기 요청 데이터
     * @return 찾은 이메일 정보
     */
    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(@Valid @RequestBody FindIdRequest request) {
        String email = authService.findId(request);
        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    /**
     * 등록된 정보를 바탕으로 비밀번호를 초기화(임시 비밀번호 발급)합니다.
     * @param request 비밀번호 초기화 요청 데이터
     * @return 임시 비밀번호 및 안내 메시지
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String tempPassword = authService.resetPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "임시 비밀번호가 발급되었습니다. 로그인 후 비밀번호를 변경해주세요.");
        response.put("temporaryPassword", tempPassword); // 현재는 이메일 발송 대신 직접 반환
        return ResponseEntity.ok(response);
    }
}
