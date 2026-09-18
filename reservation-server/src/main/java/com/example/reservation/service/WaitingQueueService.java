package com.example.reservation.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ===================================================
 * 대기열 서비스 (Redis Sorted Set 기반)
 * ===================================================
 *
 * 구조:
 *   - Sorted Set Key: "queue:concert:{concertId}"
 *   - Score: 입장 요청 시각 (epochMilli) → 먼저 요청한 순서대로 정렬
 *   - Member: userId
 *
 * 흐름:
 *   1. 사용자 → POST /api/concerts/{id}/queue/enter (대기열 진입)
 *   2. GET /api/concerts/{id}/queue/status?userId=X (내 순위 조회)
 *   3. 스케줄러가 3초마다 앞 100명에게 입장 토큰 발급
 *      → Redis에 "token:{concertId}:{userId}" 저장 (TTL=5분)
 *   4. 사용자는 토큰 보유 시 예약 API 호출 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private static final String QUEUE_KEY_PREFIX = "queue:concert:";
    private static final String TOKEN_KEY_PREFIX = "token:concert:";

    @Value("${reservation.queue.batch-size:100}")
    private int batchSize;

    @Value("${reservation.queue.token-ttl-seconds:300}")
    private long tokenTtlSeconds;

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private final AtomicLong totalQueueSize = new AtomicLong(0);

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("concert.queue.size", totalQueueSize, AtomicLong::get)
                .description("현재 전체 대기열 인원")
                .register(meterRegistry);
    }

    /**
     * 대기열 진입
     * @return 현재 내 대기 순위 (0-based, 0이면 즉시 입장 가능)
     */
    public QueueStatus enterQueue(Long concertId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + concertId;
        double score = System.currentTimeMillis();

        // 이미 입장 토큰이 있는지 확인
        if (hasToken(concertId, userId)) {
            return QueueStatus.builder()
                    .userId(userId)
                    .rank(0L)
                    .hasToken(true)
                    .message("입장 토큰이 있습니다. 바로 예약할 수 있습니다.")
                    .build();
        }

        // Sorted Set에 추가 (이미 있으면 점수 유지 — NX 옵션)
        redisTemplate.opsForZSet().addIfAbsent(queueKey, String.valueOf(userId), score);

        Long rank = redisTemplate.opsForZSet().rank(queueKey, String.valueOf(userId));
        Long totalSize = redisTemplate.opsForZSet().size(queueKey);
        totalQueueSize.set(totalSize != null ? totalSize : 0L);

        log.debug("[QUEUE] 대기열 진입 concertId={} userId={} rank={}", concertId, userId, rank);

        return QueueStatus.builder()
                .userId(userId)
                .rank(rank != null ? rank + 1 : 1L)  // 1-based
                .totalWaiting(totalSize)
                .hasToken(false)
                .estimatedWaitSeconds(calculateWaitSeconds(rank))
                .message(rank != null && rank < batchSize
                        ? "곧 입장 가능합니다!"
                        : "대기 중입니다. 순서가 되면 알림을 드립니다.")
                .build();
    }

    /**
     * 대기 순위 조회
     */
    public QueueStatus getQueueStatus(Long concertId, Long userId) {
        if (hasToken(concertId, userId)) {
            return QueueStatus.builder()
                    .userId(userId)
                    .rank(0L)
                    .hasToken(true)
                    .message("입장 토큰이 있습니다. 바로 예약할 수 있습니다.")
                    .build();
        }

        String queueKey = QUEUE_KEY_PREFIX + concertId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, String.valueOf(userId));
        Long totalSize = redisTemplate.opsForZSet().size(queueKey);

        if (rank == null) {
            return QueueStatus.builder()
                    .userId(userId)
                    .rank(-1L)
                    .hasToken(false)
                    .message("대기열에 없습니다. 먼저 대기열에 진입하세요.")
                    .build();
        }

        return QueueStatus.builder()
                .userId(userId)
                .rank(rank + 1)
                .totalWaiting(totalSize)
                .hasToken(false)
                .estimatedWaitSeconds(calculateWaitSeconds(rank))
                .message("대기 중: " + (rank + 1) + "번째")
                .build();
    }

    /**
     * 스케줄러: 3초마다 앞 N명에게 입장 토큰 발급
     * 모든 콘서트 대기열을 순회 (실제 운영에서는 활성 콘서트만 처리)
     */
    @Scheduled(fixedDelayString = "${reservation.queue.scheduler-delay-ms:3000}")
    public void processQueue() {
        Set<String> queueKeys = redisTemplate.keys(QUEUE_KEY_PREFIX + "*");
        if (queueKeys == null || queueKeys.isEmpty()) return;

        for (String queueKey : queueKeys) {
            String concertId = queueKey.replace(QUEUE_KEY_PREFIX, "");
            processQueueForConcert(concertId, queueKey);
        }
    }

    private void processQueueForConcert(String concertId, String queueKey) {
        // 상위 batchSize명 조회
        Set<String> nextBatch = redisTemplate.opsForZSet()
                .range(queueKey, 0, batchSize - 1);

        if (nextBatch == null || nextBatch.isEmpty()) return;

        for (String userIdStr : nextBatch) {
            String tokenKey = TOKEN_KEY_PREFIX + concertId + ":" + userIdStr;
            // 입장 토큰 발급 (TTL=5분)
            redisTemplate.opsForValue().set(tokenKey, "GRANTED", Duration.ofSeconds(tokenTtlSeconds));
            // 대기열에서 제거
            redisTemplate.opsForZSet().remove(queueKey, userIdStr);

            log.debug("[QUEUE] 입장 토큰 발급 concertId={} userId={}", concertId, userIdStr);
        }

        Long remaining = redisTemplate.opsForZSet().size(queueKey);
        totalQueueSize.set(remaining != null ? remaining : 0L);
        log.info("[QUEUE] 토큰 발급 완료 concertId={} 발급={}명 잔여={}명",
                concertId, nextBatch.size(), remaining);
    }

    public boolean hasToken(Long concertId, Long userId) {
        String tokenKey = TOKEN_KEY_PREFIX + concertId + ":" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    public void consumeToken(Long concertId, Long userId) {
        String tokenKey = TOKEN_KEY_PREFIX + concertId + ":" + userId;
        redisTemplate.delete(tokenKey);
    }

    private long calculateWaitSeconds(Long rank) {
        if (rank == null) return 0;
        // 배치 크기와 스케줄러 주기로 예상 대기 시간 계산
        long batches = rank / batchSize + 1;
        return batches * 3; // 3초마다 한 배치
    }

    @lombok.Builder
    @lombok.Getter
    public static class QueueStatus {
        private Long userId;
        private Long rank;
        private Long totalWaiting;
        private boolean hasToken;
        private long estimatedWaitSeconds;
        private String message;
    }
}
