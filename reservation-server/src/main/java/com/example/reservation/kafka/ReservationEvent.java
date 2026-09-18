package com.example.reservation.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEvent {
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private Long concertId;
    private String holdKey;  // Redis 임시점유 키 (확정 후 삭제)
}
