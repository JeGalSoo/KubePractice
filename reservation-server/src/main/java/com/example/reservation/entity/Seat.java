package com.example.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat", indexes = {
        @Index(name = "idx_seat_section_status", columnList = "section_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private int rowNum;

    @Column(nullable = false)
    private int colNum;

    @Column(nullable = false, length = 20)
    private String seatLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    /**
     * 낙관적 락 버전 (전략 비교 실험용)
     * @Version 으로 자동 관리 — 동시 업데이트 시 OptimisticLockException 발생
     */
    @Version
    private Long version;

    public void hold() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("이미 점유된 좌석입니다: " + seatLabel);
        }
        this.status = SeatStatus.HELD;
    }

    public void confirm() {
        if (this.status != SeatStatus.HELD) {
            throw new IllegalStateException("임시 점유 상태가 아닙니다: " + seatLabel);
        }
        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public enum SeatStatus {
        AVAILABLE, HELD, RESERVED
    }
}
