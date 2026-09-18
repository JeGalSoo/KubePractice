package com.example.reservation.repository;

import com.example.reservation.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
    List<Concert> findAllByOrderByConcertDateAsc();
    List<Concert> findByStatus(Concert.ConcertStatus status);
}
