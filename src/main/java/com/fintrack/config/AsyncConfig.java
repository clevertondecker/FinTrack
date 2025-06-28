package com.fintrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * Enables background processing of invoice imports.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures the task executor for async operations.
     *
     * @return the configured task executor. Never null.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("InvoiceImport-");
        executor.initialize();
        return executor;
    }
} 