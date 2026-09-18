package com.example.reservation.service;

import com.example.reservation.dto.ReservationRequest;
import com.example.reservation.dto.ReservationResponse;
import com.example.reservation.entity.Concert;
import com.example.reservation.entity.Reservation;
import com.example.reservation.entity.Seat;
import com.example.reservation.exception.ConcertException;
import com.example.reservation.exception.SeatAlreadyHeldException;
import com.example.reservation.kafka.ReservationEvent;
import com.example.reservation.repository.ConcertRepository;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ===================================================
 * 전략 C: Redis SETNX 임시점유 + Kafka 비동기 확정
 * ===================================================
 *
 * 동작 방식:
 *   1. Redis SETNX "seat:hold:{seatId}" userId TTL=5분
 *      → 성공: 내가 임시 점유 성공
 *      → 실패: 이미 누군가 점유 중
 *   2. DB에 PENDING 예약 저장
 *   3. Kafka produce("reservation.confirm") → 즉시 리턴 (비동기)
 *   4. Kafka Consumer → 예약 CONFIRMED로 변경 (별도 스레드)
 *   5. TTL 만료 시 Redis Keyspace Notification → 자동 취소
 *
 * 장점:
 *   - Redis 원자적 연산(SETNX)으로 락 없이 선점
 *   - Kafka 비동기로 DB 쓰기 분리 → 엄청난 TPS
 *   - 백프레셔 자연스럽게 처리
 *   - TTL로 유령 점유 자동 해제
 *
 * 단점:
 *   - 최종 일관성(Eventually Consistent) → 즉시 결과 확인 불가
 *   - 복잡한 롤백 시나리오 처리 필요
 *
 * 기대 TPS: ~3,000+
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationKafkaService {

    private static final String SEAT_HOLD_KEY = "seat:hold:";
    private static final String TOPIC_RESERVATION_CONFIRM = "reservation.confirm";

    @Value("${reservation.seat-hold.ttl-seconds:300}")
    private long seatHoldTtlSeconds;

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;
    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse reserve(Long userId, ReservationRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ConcertException("콘서트를 찾을 수 없습니다."));

        if (!concert.isSaleOpen()) {
            throw new ConcertException("티켓 판매 기간이 아닙니다.");
        }

        String holdKey = SEAT_HOLD_KEY + request.getSeatId();

        // ===== 핵심: Redis SETNX (SET if Not eXists) =====
        // 원자적 연산: 키가 없을 때만 세팅 성공 → 선착순 보장
        Boolean held = redisTemplate.opsForValue()
                .setIfAbsent(holdKey, String.valueOf(userId), Duration.ofSeconds(seatHoldTtlSeconds));

        if (Boolean.FALSE.equals(held)) {
            // 이미 다른 사용자가 점유 중
            String currentHolder = redisTemplate.opsForValue().get(holdKey);
            throw new SeatAlreadyHeldException("이미 선택된 좌석입니다. (점유자 ID: " + currentHolder + ")");
        }

        log.debug("[KAFKA] Redis 임시점유 성공 userId={} seatId={} TTL={}s",
                userId, request.getSeatId(), seatHoldTtlSeconds);

        // 좌석 상태를 HELD로 변경
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ConcertException("좌석을 찾을 수 없습니다."));

        seat.hold();

        // DB에 PENDING 예약 저장
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .seat(seat)
                .concert(concert)
                .strategy(Reservation.ReservationStrategy.KAFKA)
                .status(Reservation.ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusSeconds(seatHoldTtlSeconds))
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // Kafka로 비동기 확정 이벤트 발행 → 즉시 응답 (결제 시뮬레이션 생략)
        ReservationEvent event = ReservationEvent.builder()
                .reservationId(saved.getId())
                .userId(userId)
                .seatId(request.getSeatId())
                .concertId(request.getConcertId())
                .holdKey(holdKey)
                .build();

        kafkaTemplate.send(TOPIC_RESERVATION_CONFIRM, String.valueOf(request.getSeatId()), event);
        log.debug("[KAFKA] 확정 이벤트 발행 reservationId={}", saved.getId());

        return ReservationResponse.from(saved);
    }
}
