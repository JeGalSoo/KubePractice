package com.example.auth.service;

import com.example.auth.dto.request.FindIdRequest;
import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.request.ResetPasswordRequest;
import com.example.auth.dto.response.AuthResponse;
import com.example.auth.entity.RefreshToken;
import com.example.auth.entity.Role;
import com.example.auth.entity.User;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    /**
     * 사용자 로그인을 처리합니다.
     *
     * <p><b>멀티 디바이스 처리 흐름:</b></p>
     * <ol>
     *   <li>요청에 {@code deviceId}가 있으면 해당 기기의 기존 토큰을 찾아 교체합니다.</li>
     *   <li>{@code deviceId}가 없으면 새 UUID를 발급하여 새 세션을 생성합니다.</li>
     *   <li>다른 기기(다른 {@code deviceId})의 토큰은 건드리지 않습니다.</li>
     * </ol>
     *
     * <p><b>이전 방식의 문제점:</b> {@code deleteByUser()}로 모든 기기 토큰을 삭제하여
     * 다른 기기가 강제 로그아웃되고, 동시 로그인 시 InnoDB Lock Contention이 발생했습니다.</p>
     *
     * @param request 로그인 요청 (email, password, deviceId)
     * @return 발급된 토큰 및 사용자 정보
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // deviceId 결정: 클라이언트가 보내지 않으면 새 UUID 발급
        String deviceId = (request.getDeviceId() != null && !request.getDeviceId().isBlank())
                ? request.getDeviceId()
                : UUID.randomUUID().toString();

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshTokenString = jwtUtil.generateRefreshToken(user.getEmail());

        // 같은 기기(deviceId)의 기존 토큰만 삭제 후 새 토큰 저장
        // → 다른 기기(다른 deviceId)의 토큰은 그대로 유지 (멀티 디바이스 세션 보장)
        refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .deviceId(deviceId)
                .expiresAt(java.time.LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .deviceId(deviceId)          // 클라이언트가 로컬스토리지에 저장해야 함
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String findId(FindIdRequest request) {
        User user = userRepository.findByNameAndPhone(request.getName(), request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원을 찾을 수 없습니다."));
        return user.getEmail();
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#request.email")
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailAndName(request.getEmail(), request.getName())
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원을 찾을 수 없습니다."));

        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        return tempPassword;
    }
}
