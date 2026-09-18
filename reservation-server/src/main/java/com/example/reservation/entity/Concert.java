package com.example.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "concert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String artist;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime concertDate;

    @Column(nullable = false)
    private LocalDateTime saleOpenAt;

    @Column(nullable = false)
    private LocalDateTime saleCloseAt;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String posterUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConcertStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ConcertStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isSaleOpen() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(saleOpenAt) && now.isBefore(saleCloseAt)
                && status == ConcertStatus.ON_SALE;
    }

    public enum ConcertStatus {
        SCHEDULED, ON_SALE, SOLD_OUT, ENDED
    }
}
