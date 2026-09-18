package com.example.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int remainSeats;

    public void decreaseRemainSeats() {
        if (remainSeats <= 0) {
            throw new IllegalStateException("잔여 좌석이 없습니다.");
        }
        this.remainSeats--;
    }

    public void increaseRemainSeats() {
        if (remainSeats >= totalSeats) {
            throw new IllegalStateException("잔여 좌석이 이미 최대입니다.");
        }
        this.remainSeats++;
    }
}
