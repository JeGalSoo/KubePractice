package com.example.reservation.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 예약 전략 성능 측정 어노테이션
 * 이 어노테이션이 붙은 메서드의 실행 시간을 Prometheus에 기록
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MeasureReservationPerf {

    /**
     * 예약 전략 식별자 (Prometheus 태그로 사용)
     * DB_LOCK / REDIS_LOCK / KAFKA
     */
    String strategy();
}
