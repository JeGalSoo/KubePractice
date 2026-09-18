package com.example.reservation.repository;

import com.example.reservation.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySectionId(Long sectionId);

    List<Seat> findBySectionIdAndStatus(Long sectionId, Seat.SeatStatus status);

    /**
     * 전략 A: 비관적 락 (SELECT ... FOR UPDATE)
     * - 동시 접근을 DB 레벨에서 직렬화
     * - 처리량 낮음, 데드락 위험
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * 전략 B: 낙관적 락 (version 기반)
     * - @Version 필드를 통해 충돌 감지
     * - OptimisticLockException 발생 시 재시도
     */
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithOptimisticLock(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.section.id = :sectionId AND s.status = 'AVAILABLE'")
    int countAvailableBySectionId(@Param("sectionId") Long sectionId);
}
