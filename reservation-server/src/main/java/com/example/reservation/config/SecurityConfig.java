package com.example.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security 설정
 * Gateway에서 이미 JWT 인증을 처리하고 X-User-Id 헤더를 전달하므로
 * reservation-server는 내부 헤더 기반으로 사용자 식별 (별도 JWT 재검증 없음)
 *
 * 실제 운영: Gateway가 내부망에서만 접근 가능하도록 네트워크 격리 필요
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator (Prometheus 수집용)
                .requestMatchers("/actuator/**").permitAll()
                // 공연 조회는 인증 불필요
                .requestMatchers("/api/concerts", "/api/concerts/**").permitAll()
                .anyRequest().permitAll()  // Gateway에서 인증 위임
            );

        return http.build();
    }
}
