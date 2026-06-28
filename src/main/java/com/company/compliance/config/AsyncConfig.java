package com.company.compliance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Async and scheduling thread-pool configuration.
 *
 * <p>Two pools are configured:
 * <ul>
 *   <li>{@code auditExecutor}  — dedicated pool for async audit log publishing
 *       (isolated so audit tasks never compete with business logic)</li>
 *   <li>{@code reportExecutor} — report generation pool with bounded concurrency</li>
 *   <li>{@code taskScheduler}  — scheduler for cron jobs (policy evaluation, alerts)</li>
 * </ul>
 *
 * <p>MDC propagation is handled per-task so that traceId/userId flow into
 * async threads correctly.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final AppProperties appProperties;

    // ── Default async executor ────────────────────────────────────

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("compliance-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // ── Dedicated audit executor ──────────────────────────────────

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10_000);          // large queue — audit never blocks
        executor.setThreadNamePrefix("audit-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);    // finish draining on shutdown
        executor.initialize();
        return executor;
    }

    // ── Report generation executor ────────────────────────────────

    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        int maxJobs = appProperties.getReports().getMaxConcurrentJobs();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxJobs);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-gen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);   // reports can take a while
        executor.initialize();
        return executor;
    }

    // ── Task scheduler (cron jobs) ────────────────────────────────

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("compliance-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(t ->
                log.error("Unhandled error in scheduled task: {}", t.getMessage(), t));
        scheduler.initialize();
        return scheduler;
    }

    // ── Async exception handler ───────────────────────────────────

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex,
                                                Method method,
                                                Object... params) {
                log.error("Unhandled async exception in method [{}] with params {}: {}",
                        method.getName(),
                        Arrays.toString(params),
                        ex.getMessage(), ex);
            }
        };
    }
}
