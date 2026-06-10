package com.example.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 요청 DTO.
 *
 * <p>{@code deviceId}는 선택 필드입니다.
 * 클라이언트(브라우저, 앱)가 로컬스토리지 등에 저장해 둔 UUID를 보내면
 * 해당 디바이스의 기존 리프레시 토큰을 교체합니다.
 * 없으면 서버가 새 UUID를 자동 부여하여 새로운 세션으로 처리합니다.</p>
 *
 * <p>덕분에 노트북에서 로그인해도 휴대폰의 세션이 유지됩니다.</p>
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    /**
     * 디바이스/세션 식별자 (UUID).
     * 선택 필드: null이면 서버에서 새 UUID를 자동 생성합니다.
     */
    private String deviceId;
}
