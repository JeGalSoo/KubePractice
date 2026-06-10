package com.example.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인/토큰 갱신 응답 DTO.
 *
 * <p>클라이언트는 {@code deviceId}를 로컬스토리지(또는 앱 저장소)에 저장해야 합니다.
 * 다음 로그인 요청 시 {@code LoginRequest.deviceId}로 함께 전송하면
 * 동일 기기의 세션으로 인식되어 기존 세션이 교체됩니다.
 * (다른 기기 세션은 영향받지 않음)</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    /** 디바이스/세션 식별자. 클라이언트가 로컬스토리지에 저장하여 재로그인 시 전송. */
    private String deviceId;
    private Long userId;
    private String email;
    private String name;
    private String role;
}
