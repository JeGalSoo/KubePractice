package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자의 Refresh Token을 디바이스별로 관리하는 엔티티.
 *
 * <p>하나의 사용자({@link User})는 여러 디바이스(PC, 모바일 등)에서 로그인할 수 있으며,
 * 각 디바이스는 고유한 {@code deviceId}(UUID)를 가집니다.
 * 새 디바이스 로그인 시 기존 다른 디바이스의 토큰을 삭제하지 않아
 * 멀티 디바이스 동시 세션이 유지됩니다.</p>
 *
 * <p>같은 유저가 같은 디바이스에서 재로그인하면 해당 디바이스의 토큰만 교체됩니다.
 * ({@code user_id + device_id} 복합 유니크 제약)</p>
 */
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
        // (user_id, device_id) 조합으로 "같은 유저의 같은 기기" 유일성 보장
        @UniqueConstraint(name = "uq_user_device", columnNames = {"user_id", "device_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT Refresh Token 문자열.
     * 각 로그인마다 새로 생성되는 고유값.
     */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /**
     * 이 토큰을 발급받은 사용자.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 디바이스/세션 식별자 (UUID 형식).
     * 클라이언트가 로컬스토리지 등에 저장하여 재로그인 시 같이 보냅니다.
     * 없으면 새 UUID를 부여하여 새 세션으로 처리합니다.
     */
    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;

    /**
     * 토큰 만료 시각.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 현재 시각 기준으로 토큰이 만료되었는지 확인합니다.
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
