package com.example.auth.repository;

import com.example.auth.entity.PerformanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceLogRepository extends JpaRepository<PerformanceLog, Long> {
}
