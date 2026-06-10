package com.example.auth.repository;

import com.example.auth.entity.RefreshToken;
import com.example.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 데이터 접근 레포지토리.
 *
 * <p>멀티 디바이스 지원을 위해 user 단위 전체 삭제 메서드는 제거되었습니다.
 * 대신 (user, deviceId) 단위로 특정 디바이스의 토큰만 삭제합니다.</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 문자열로 RefreshToken 조회. (갱신 API에서 사용)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 특정 사용자의 특정 디바이스 토큰 조회. (재로그인 시 덮어쓰기용)
     */
    Optional<RefreshToken> findByUserAndDeviceId(User user, String deviceId);

    /**
     * 토큰 문자열로 삭제. (로그아웃 시 해당 디바이스 토큰만 제거)
     */
    void deleteByToken(String token);

    /**
     * 특정 사용자의 특정 디바이스 토큰 삭제. (해당 디바이스 로그아웃)
     */
    void deleteByUserAndDeviceId(User user, String deviceId);

    /**
     * 만료된 토큰 일괄 삭제. (배치 서버 등에서 정기 실행)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredTokens(LocalDateTime now);

    boolean existsByToken(String token);
}
