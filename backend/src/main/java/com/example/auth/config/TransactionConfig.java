package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 트랜잭션 관련 빈을 등록하는 설정 클래스.
 *
 * <p>{@link PerformanceAspect}에서 사용하는 {@code PROPAGATION_REQUIRES_NEW} 트랜잭션 템플릿을
 * 싱글톤 빈으로 등록합니다. 요청마다 {@code new TransactionTemplate(...)}을 생성하는 것을 방지하여
 * GC 부담을 줄입니다.</p>
 */
@Configuration
public class TransactionConfig {

    /**
     * PROPAGATION_REQUIRES_NEW 전파 속성을 가진 TransactionTemplate 빈.
     * 기존 트랜잭션과 완전히 독립된 새 트랜잭션을 열어 AOP 성능 로그 저장에 활용됩니다.
     *
     * @param transactionManager Spring이 자동 구성하는 PlatformTransactionManager
     * @return REQUIRES_NEW 전파 속성을 가진 TransactionTemplate
     */
    @Bean
    public TransactionTemplate requiresNewTx(PlatformTransactionManager transactionManager) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tt;
    }
}
