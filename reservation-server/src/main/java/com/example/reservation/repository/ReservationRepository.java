package com.example.reservation.repository;

import com.example.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    Optional<Reservation> findBySeatIdAndStatus(Long seatId, Reservation.ReservationStatus status);

    boolean existsBySeatIdAndStatusIn(Long seatId, List<Reservation.ReservationStatus> statuses);

    /**
     * 만료된 PENDING 예약 조회 (배치 정리용)
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.expiredAt < :now")
    List<Reservation> findExpiredPendingReservations(@Param("now") LocalDateTime now);

    /**
     * 전략별 예약 통계 (Grafana 시각화용)
     */
    @Query("SELECT r.strategy, COUNT(r) FROM Reservation r GROUP BY r.strategy")
    List<Object[]> countByStrategy();
}
