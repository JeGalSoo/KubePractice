package com.example.auth.config;

import com.example.auth.entity.QueryLog;
import com.example.auth.repository.QueryLogRepository;
import com.example.auth.util.StaticContextAccessor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqlInterceptor implements StatementInspector {

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    public String inspect(String sql) {
        if (sql == null || sql.contains("query_log") || sql.contains("performance_log")) {
            return sql;
        }

        executorService.submit(() -> {
            try {
                QueryLogRepository repository = StaticContextAccessor.getBean(QueryLogRepository.class);
                QueryLog log = QueryLog.builder()
                        .sqlQuery(sql)
                        .executionTimeMs(0L)
                        .build();
                repository.save(log);
            } catch (Exception e) {
                // Ignore errors
            }
        });

        return sql;
    }
}
