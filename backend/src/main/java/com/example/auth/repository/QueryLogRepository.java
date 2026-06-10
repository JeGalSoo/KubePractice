package com.example.auth.repository;

import com.example.auth.entity.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
}
