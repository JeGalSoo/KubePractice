package com.example.auth.service;

import com.example.auth.dto.request.FindIdRequest;
import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.request.ResetPasswordRequest;
import com.example.auth.dto.response.AuthResponse;

public interface AuthService {
    
    /**
     * 신규 사용자 회원가입 처리를 수행합니다.
     * @param request 회원가입 정보
     */
    void register(RegisterRequest request);
    
    /**
     * 사용자의 로그인 요청을 검증하고 인증 토큰을 발급합니다.
     * @param request 로그인(이메일, 비밀번호) 정보
     * @return 인증 결과 (JWT 토큰 등)
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * 가입된 이메일(아이디)을 찾습니다.
     * @param request 아이디 찾기를 위한 사용자 정보
     * @return 찾은 이메일 문자열
     */
    String findId(FindIdRequest request);
    
    /**
     * 사용자의 비밀번호를 임시로 초기화하고 새로운 비밀번호를 반환합니다.
     * @param request 사용자 식별 정보
     * @return 발급된 임시 비밀번호
     */
    String resetPassword(ResetPasswordRequest request);
}
