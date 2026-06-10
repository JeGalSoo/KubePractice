package com.example.auth.config;

import com.example.auth.entity.PerformanceLog;
import com.example.auth.repository.PerformanceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

/**
 * 서비스 계층 전체에 적용되는 AOP 성능 측정 Aspect.
 *
 * <p>모든 {@code com.example.auth.service.*} 메서드의 실행 시간을 측정하고,
 * 비즈니스 트랜잭션과 충돌하지 않도록 완전히 독립된 새 트랜잭션(REQUIRES_NEW)으로
 * 성능 로그를 DB에 저장합니다.</p>
 *
 * <p>{@link TransactionTemplate}은 요청마다 new 생성하지 않고 빈으로 주입받아 재사용합니다.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceAspect {

    private final PerformanceLogRepository performanceLogRepository;

    /**
     * PROPAGATION_REQUIRES_NEW로 설정된 TransactionTemplate 빈.
     * {@link TransactionConfig}에서 등록됩니다.
     * 매 요청마다 new 생성하지 않아 GC 부담을 줄입니다.
     */
    private final TransactionTemplate requiresNewTx;

    /**
     * 서비스 계층 메서드 실행 시간을 측정하고 성능 로그를 저장합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 원래 메서드의 반환값
     * @throws Throwable 원래 메서드에서 발생한 예외를 그대로 전파
     */
    @Around("execution(* com.example.auth.service.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;
        String methodName = joinPoint.getSignature().toShortString();
        String params = Arrays.toString(joinPoint.getArgs());

        // DB VARCHAR(255) 한도 초과 방지: 250자로 truncate
        if (params.length() > 250) {
            params = params.substring(0, 250) + "...";
        }

        // 10ms 미만이면 Redis 캐시 히트로 간주
        boolean cacheHit = executionTime < 10;

        PerformanceLog logEntry = PerformanceLog.builder()
                .methodName(methodName)
                .executionTimeMs(executionTime)
                .cacheHit(cacheHit)
                .params(params)
                .build();

        try {
            // 빈으로 주입받은 TransactionTemplate 재사용 (PROPAGATION_REQUIRES_NEW)
            requiresNewTx.executeWithoutResult(status -> {
                performanceLogRepository.save(logEntry);
            });
        } catch (Exception e) {
            log.error("Failed to save performance log for method [{}]", methodName, e);
        }

        log.info("Method [{}] executed in {} ms (Cache Hit: {})", methodName, executionTime, cacheHit);

        return result;
    }
}
