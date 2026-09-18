package com.example.reservation.kafka;

import com.example.reservation.entity.Reservation;
import com.example.reservation.entity.Seat;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.repository.SeatRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventConsumer {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private Counter confirmedCounter;
    private Counter failedCounter;

    @PostConstruct
    public void initMetrics() {
        confirmedCounter = Counter.builder("concert.reservation.kafka.confirmed")
                .description("Kafka 비동기 예약 확정 횟수")
                .register(meterRegistry);
        failedCounter = Counter.builder("concert.reservation.kafka.failed")
                .description("Kafka 비동기 예약 확정 실패 횟수")
                .register(meterRegistry);
    }

    /**
     * reservation.confirm 토픽 소비
     * PENDING → CONFIRMED 변경 + Redis 임시점유 키 삭제
     */
    @KafkaListener(topics = "reservation.confirm", groupId = "reservation-group",
            containerFactory = "reservationKafkaListenerContainerFactory")
    @Transactional
    public void handleReservationConfirm(ReservationEvent event) {
        log.debug("[KAFKA] 예약 확정 처리 시작 reservationId={}", event.getReservationId());

        try {
            Reservation reservation = reservationRepository.findById(event.getReservationId())
                    .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다: " + event.getReservationId()));

            if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
                log.warn("[KAFKA] 이미 처리된 예약 reservationId={} status={}",
                        event.getReservationId(), reservation.getStatus());
                return;
            }

            // 좌석 상태 RESERVED로 변경
            Seat seat = seatRepository.findById(event.getSeatId())
                    .orElseThrow(() -> new RuntimeException("좌석을 찾을 수 없습니다: " + event.getSeatId()));
            seat.confirm();

            // 예약 CONFIRMED
            reservation.confirm();

            // Redis 임시점유 키 삭제 (TTL 기다릴 필요 없이 즉시 해제)
            redisTemplate.delete(event.getHoldKey());

            confirmedCounter.increment();
            log.info("[KAFKA] 예약 확정 완료 reservationId={} seatId={}",
                    event.getReservationId(), event.getSeatId());

        } catch (Exception e) {
            failedCounter.increment();
            log.error("[KAFKA] 예약 확정 실패 reservationId={} error={}",
                    event.getReservationId(), e.getMessage(), e);
        }
    }
}
