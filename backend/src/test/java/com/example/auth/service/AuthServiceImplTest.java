package com.example.auth.service;

import com.example.auth.dto.request.FindIdRequest;
import com.example.auth.dto.request.LoginRequest;
import com.example.auth.dto.request.RegisterRequest;
import com.example.auth.dto.request.ResetPasswordRequest;
import com.example.auth.dto.response.AuthResponse;
import com.example.auth.entity.Gender;
import com.example.auth.entity.Role;
import com.example.auth.entity.User;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .phone("01012345678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void register_Success() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setName("신규유저");
        request.setPhone("01099998888");
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        request.setGender(Gender.FEMALE);

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword123");
        given(userRepository.save(any(User.class))).willReturn(new User());

        // when
        assertDoesNotThrow(() -> authService.register(request));

        // then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_Fail_DuplicateEmail() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        given(userRepository.existsByEmail(anyString())).willReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("이미 사용 중인 이메일입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(null); // Return mock authentication
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        given(jwtUtil.generateAccessToken(anyString(), anyString())).willReturn("mockAccessToken");
        given(jwtUtil.generateRefreshToken(anyString())).willReturn("mockRefreshToken");

        // when
        AuthResponse response = authService.login(request);

        // then
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("아이디 찾기 성공 테스트")
    void findId_Success() {
        // given
        FindIdRequest request = new FindIdRequest("홍길동", "01012345678");
        given(userRepository.findByNameAndPhone(anyString(), anyString())).willReturn(Optional.of(testUser));

        // when
        String email = authService.findId(request);

        // then
        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 테스트")
    void resetPassword_Success() {
        // given
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "홍길동");
        given(userRepository.findByEmailAndName(anyString(), anyString())).willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(anyString())).willReturn("newEncodedPassword");

        // when
        String tempPassword = authService.resetPassword(request);

        // then
        assertNotNull(tempPassword);
        assertEquals(8, tempPassword.length());
        verify(userRepository).save(testUser); // JPA dirty checking is sufficient, but we verify explicit save call
    }
}
