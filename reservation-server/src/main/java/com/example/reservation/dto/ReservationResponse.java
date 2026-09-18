package com.example.reservation.dto;

import com.example.reservation.entity.Reservation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponse {
    private Long id;
    private Long userId;
    private Long seatId;
    private String seatLabel;
    private Long concertId;
    private String concertTitle;
    private String strategy;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .seatId(r.getSeat().getId())
                .seatLabel(r.getSeat().getSeatLabel())
                .concertId(r.getConcert().getId())
                .concertTitle(r.getConcert().getTitle())
                .strategy(r.getStrategy().name())
                .status(r.getStatus().name())
                .expiredAt(r.getExpiredAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
