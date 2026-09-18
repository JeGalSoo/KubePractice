package com.example.reservation.service;

import com.example.reservation.dto.ReservationRequest;
import com.example.reservation.dto.ReservationResponse;
import com.example.reservation.entity.Concert;
import com.example.reservation.entity.Reservation;
import com.example.reservation.entity.Seat;
import com.example.reservation.exception.ConcertException;
import com.example.reservation.exception.SeatAlreadyHeldException;
import com.example.reservation.repository.ConcertRepository;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * ===================================================
 * 전략 B: Redis 분산 락 (Redisson)
 * ===================================================
 *
 * 동작 방식:
 *   Redis에 분산 락 키 생성 → 락 획득 후 트랜잭션 실행 → 락 해제
 *   DB에는 락 없이 READ → DB 부하 최소화
 *
 * 장점:
 *   - DB 커넥션 점유 시간 최소화
 *   - Redis 메모리 기반 → 락 획득/해제 초고속
 *   - 공정한 락 획득 순서 보장 (Redisson FairLock 옵션)
 *
 * 단점:
 *   - Redis 장애 시 락 기능 마비 (Redis HA 필요)
 *   - 네트워크 왕복 추가
 *
 * 기대 TPS: ~500~800
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRedisLockService {

    private static final String SEAT_LOCK_KEY = "seat:lock:";
    private static final long WAIT_TIME = 3L;       // 락 획득 대기 시간 (초)
    private static final long LEASE_TIME = 10L;     // 락 보유 최대 시간 (초)

    private final RedissonClient redissonClient;
    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final ReservationRepository reservationRepository;

    public ReservationResponse reserve(Long userId, ReservationRequest request) {
        String lockKey = SEAT_LOCK_KEY + request.getSeatId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new SeatAlreadyHeldException("다른 사용자가 처리 중입니다. 잠시 후 다시 시도하세요.");
            }

            log.debug("[REDIS_LOCK] 락 획득 userId={} seatId={}", userId, request.getSeatId());
            return doReserve(userId, request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcertException("예약 처리 중 인터럽트가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[REDIS_LOCK] 락 해제 seatId={}", request.getSeatId());
            }
        }
    }

    @Transactional
    protected ReservationResponse doReserve(Long userId, ReservationRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ConcertException("콘서트를 찾을 수 없습니다."));

        if (!concert.isSaleOpen()) {
            throw new ConcertException("티켓 판매 기간이 아닙니다.");
        }

        // 락을 Redis가 쥐고 있으므로 DB에는 일반 SELECT
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ConcertException("좌석을 찾을 수 없습니다."));

        if (!seat.isAvailable()) {
            throw new SeatAlreadyHeldException("이미 선택된 좌석입니다: " + seat.getSeatLabel());
        }

        seat.hold();

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .seat(seat)
                .concert(concert)
                .strategy(Reservation.ReservationStrategy.REDIS_LOCK)
                .status(Reservation.ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusSeconds(300))
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.debug("[REDIS_LOCK] 예약 성공 userId={} seatId={}", userId, seat.getId());
        return ReservationResponse.from(saved);
    }
}
