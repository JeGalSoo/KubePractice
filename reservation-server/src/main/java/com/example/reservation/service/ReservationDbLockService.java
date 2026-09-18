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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ===================================================
 * 전략 A: DB 비관적 락 (Pessimistic Lock)
 * ===================================================
 *
 * 동작 방식:
 *   SELECT ... FOR UPDATE → 한 트랜잭션이 락 해제 시까지 다른 트랜잭션 대기
 *
 * 장점:
 *   - 구현 단순, 데이터 정합성 완벽 보장
 *   - 별도 인프라 불필요
 *
 * 단점:
 *   - DB 커넥션 고갈 위험 (대기 중 커넥션 유지)
 *   - TPS 매우 낮음 (직렬화)
 *   - 데드락 발생 가능
 *
 * 기대 TPS: ~30~50
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationDbLockService {

    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse reserve(Long userId, ReservationRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ConcertException("콘서트를 찾을 수 없습니다."));

        if (!concert.isSaleOpen()) {
            throw new ConcertException("티켓 판매 기간이 아닙니다.");
        }

        // 핵심: SELECT ... FOR UPDATE — 이 시점부터 해당 행에 배타적 락
        Seat seat = seatRepository.findByIdWithPessimisticLock(request.getSeatId())
                .orElseThrow(() -> new ConcertException("좌석을 찾을 수 없습니다."));

        if (!seat.isAvailable()) {
            throw new SeatAlreadyHeldException("이미 선택된 좌석입니다: " + seat.getSeatLabel());
        }

        // 좌석 상태 변경 (AVAILABLE → HELD)
        seat.hold();

        // 예약 생성 (PENDING 상태, 5분 후 만료)
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .seat(seat)
                .concert(concert)
                .strategy(Reservation.ReservationStrategy.DB_LOCK)
                .status(Reservation.ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusSeconds(300))
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.debug("[DB_LOCK] 예약 성공 userId={} seatId={}", userId, seat.getId());
        return ReservationResponse.from(saved);
    }
}
