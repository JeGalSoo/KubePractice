package com.example.auth.controller;

import com.example.auth.entity.PerformanceLog;
import com.example.auth.entity.QueryLog;
import com.example.auth.repository.PerformanceLogRepository;
import com.example.auth.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceLogRepository performanceLogRepository;
    private final QueryLogRepository queryLogRepository;

    /**
     * 시스템에서 수집된 성능 모니터링 로그를 조회합니다.
     * @param pageable 페이징 인자(최신순 등)
     * @return 페이징된 성능 로그 목록
     */
    @GetMapping("/logs")
    public Page<PerformanceLog> getPerformanceLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return performanceLogRepository.findAll(pageable);
    }

    /**
     * 시스템의 데이터베이스 쿼리 실행 로그를 조회합니다.
     * @param pageable 페이징 인자
     * @return 페이징된 쿼리 로그 목록
     */
    @GetMapping("/queries")
    public Page<QueryLog> getQueryLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return queryLogRepository.findAll(pageable);
    }

    /**
     * 성능 로그를 기반으로 캐시 적중률(Hit Ratio) 등 전반적인 통계 수치를 집계하여 반환합니다.
     * @return 성능 통계 정보 (Map 단위 응답)
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<PerformanceLog> allLogs = performanceLogRepository.findAll();
        
        long totalCount = allLogs.size();
        long cacheHits = allLogs.stream().filter(PerformanceLog::isCacheHit).count();
        double avgExecutionTime = allLogs.stream()
                .mapToLong(PerformanceLog::getExecutionTimeMs)
                .average()
                .orElse(0.0);
        
        double avgCacheHitTime = allLogs.stream()
                .filter(PerformanceLog::isCacheHit)
                .mapToLong(PerformanceLog::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        double avgCacheMissTime = allLogs.stream()
                .filter(log -> !log.isCacheHit())
                .mapToLong(PerformanceLog::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalCount);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheHitRatio", totalCount > 0 ? (double) cacheHits / totalCount : 0);
        stats.put("avgExecutionTimeMs", avgExecutionTime);
        stats.put("avgCacheHitTimeMs", avgCacheHitTime);
        stats.put("avgCacheMissTimeMs", avgCacheMissTime);
        stats.put("performanceImprovement", avgCacheHitTime > 0 ? (avgCacheMissTime / avgCacheHitTime) : 0);

        return stats;
    }
}
