package com.example.reservation.dto;

import com.example.reservation.entity.Concert;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConcertResponse {
    private Long id;
    private String title;
    private String artist;
    private String venue;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime concertDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime saleOpenAt;
    private String status;
    private boolean saleOpen;

    public static ConcertResponse from(Concert c) {
        return ConcertResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .artist(c.getArtist())
                .venue(c.getVenue())
                .concertDate(c.getConcertDate())
                .saleOpenAt(c.getSaleOpenAt())
                .status(c.getStatus().name())
                .saleOpen(c.isSaleOpen())
                .build();
    }
}
