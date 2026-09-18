package com.example.reservation.controller;

import com.example.reservation.dto.ConcertResponse;
import com.example.reservation.dto.ReservationRequest;
import com.example.reservation.dto.ReservationResponse;
import com.example.reservation.dto.SeatResponse;
import com.example.reservation.aop.MeasureReservationPerf;
import com.example.reservation.entity.Seat;
import com.example.reservation.repository.ConcertRepository;
import com.example.reservation.repository.SeatRepository;
import com.example.reservation.repository.SectionRepository;
import com.example.reservation.service.ReservationDbLockService;
import com.example.reservation.service.ReservationKafkaService;
import com.example.reservation.service.ReservationRedisLockService;
import com.example.reservation.service.WaitingQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 콘서트 예매 컨트롤러
 *
 * 예약 전략 선택:
 *   POST /api/concerts/{id}/reserve/db-lock    → 전략 A: DB 비관적 락
 *   POST /api/concerts/{id}/reserve/redis-lock → 전략 B: Redisson 분산 락
 *   POST /api/concerts/{id}/reserve/kafka      → 전략 C: Redis SETNX + Kafka 비동기
 */
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertRepository concertRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
    private final ReservationDbLockService dbLockService;
    private final ReservationRedisLockService redisLockService;
    private final ReservationKafkaService kafkaService;
    private final WaitingQueueService queueService;

    // ============================================================
    // 공연 조회 API
    // ============================================================

    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getAllConcerts() {
        List<ConcertResponse> concerts = concertRepository.findAllByOrderByConcertDateAsc()
                .stream().map(ConcertResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(concerts);
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        return concertRepository.findById(concertId)
                .map(c -> ResponseEntity.ok(ConcertResponse.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{concertId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(
            @PathVariable Long concertId,
            @RequestParam(required = false) Long sectionId) {

        List<Seat> seats = sectionId != null
                ? seatRepository.findBySectionIdAndStatus(sectionId, Seat.SeatStatus.AVAILABLE)
                : seatRepository.findBySectionId(sectionId);

        List<SeatResponse> response = seats.stream().map(s -> {
            // 섹션 정보 로드
            if (s.getSection() == null) return null;
            return SeatResponse.from(s);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 예약 API — 3가지 전략 (성능 비교용)
    // ============================================================

    /**
     * 전략 A: DB 비관적 락
     * 가장 단순하지만 처리량 낮음 (베이스라인)
     */
    @PostMapping("/{concertId}/reserve/db-lock")
    @MeasureReservationPerf(strategy = "DB_LOCK")
    public ResponseEntity<ReservationResponse> reserveWithDbLock(
            @PathVariable Long concertId,
            @Valid @RequestBody ReservationRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        ReservationResponse response = dbLockService.reserve(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 전략 B: Redisson 분산 락
     * Redis 락으로 DB 부하 최소화
     */
    @PostMapping("/{concertId}/reserve/redis-lock")
    @MeasureReservationPerf(strategy = "REDIS_LOCK")
    public ResponseEntity<ReservationResponse> reserveWithRedisLock(
            @PathVariable Long concertId,
            @Valid @RequestBody ReservationRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        ReservationResponse response = redisLockService.reserve(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 전략 C: Redis SETNX + Kafka 비동기
     * 가장 높은 TPS — 비동기 최종 일관성
     */
    @PostMapping("/{concertId}/reserve/kafka")
    @MeasureReservationPerf(strategy = "KAFKA")
    public ResponseEntity<ReservationResponse> reserveWithKafka(
            @PathVariable Long concertId,
            @Valid @RequestBody ReservationRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        ReservationResponse response = kafkaService.reserve(userId, request);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 대기열 API
    // ============================================================

    /**
     * 대기열 진입
     */
    @PostMapping("/{concertId}/queue/enter")
    public ResponseEntity<WaitingQueueService.QueueStatus> enterQueue(
            @PathVariable Long concertId,
            @RequestHeader("X-User-Id") Long userId) {

        WaitingQueueService.QueueStatus status = queueService.enterQueue(concertId, userId);
        return ResponseEntity.ok(status);
    }

    /**
     * 내 대기 순위 조회
     */
    @GetMapping("/{concertId}/queue/status")
    public ResponseEntity<WaitingQueueService.QueueStatus> getQueueStatus(
            @PathVariable Long concertId,
            @RequestHeader("X-User-Id") Long userId) {

        WaitingQueueService.QueueStatus status = queueService.getQueueStatus(concertId, userId);
        return ResponseEntity.ok(status);
    }
}
