package com.example.reservation.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * ===================================================
 * 예약 전략 성능 측정 AOP
 * ===================================================
 *
 * @MeasureReservationPerf 어노테이션이 붙은 메서드에 대해:
 * - 실행 시간 → Prometheus Timer (strategy 태그)
 * - 성공 횟수 → Counter
 * - 실패(예외) 횟수 → Counter
 *
 * Grafana에서 strategy 별 TPS, p95, p99 응답시간 비교 가능
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ReservationPerfAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(measure)")
    public Object measure(ProceedingJoinPoint pjp, MeasureReservationPerf measure) throws Throwable {
        String strategy = measure.strategy();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = pjp.proceed();

            // 성공 카운터
            Counter.builder("concert.reservation.success")
                    .tag("strategy", strategy)
                    .description("예약 성공 횟수")
                    .register(meterRegistry)
                    .increment();

            return result;

        } catch (Exception e) {
            // 실패 카운터 (좌석 충돌 포함)
            String errorType = e.getClass().getSimpleName();
            Counter.builder("concert.reservation.failure")
                    .tag("strategy", strategy)
                    .tag("error", errorType)
                    .description("예약 실패 횟수")
                    .register(meterRegistry)
                    .increment();

            log.debug("[PERF] 예약 실패 strategy={} error={}", strategy, e.getMessage());
            throw e;

        } finally {
            // 응답 시간 측정 (성공/실패 모두)
            sample.stop(Timer.builder("concert.reservation.duration")
                    .tag("strategy", strategy)
                    .description("예약 처리 시간 (ms)")
                    .publishPercentiles(0.5, 0.95, 0.99)  // p50, p95, p99
                    .register(meterRegistry));
        }
    }
}
