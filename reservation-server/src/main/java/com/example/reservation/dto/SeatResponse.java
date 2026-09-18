package com.example.reservation.dto;

import com.example.reservation.entity.Seat;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatResponse {
    private Long id;
    private Long sectionId;
    private String sectionName;
    private int rowNum;
    private int colNum;
    private String seatLabel;
    private String status;
    private int price;

    public static SeatResponse from(Seat s) {
        return SeatResponse.builder()
                .id(s.getId())
                .sectionId(s.getSection().getId())
                .sectionName(s.getSection().getName())
                .rowNum(s.getRowNum())
                .colNum(s.getColNum())
                .seatLabel(s.getSeatLabel())
                .status(s.getStatus().name())
                .price(s.getSection().getPrice())
                .build();
    }
}
