package com.copap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${app.executor.pool-size:3}")
    private int poolSize;

    @Bean(name = "orderExecutor")
    public Executor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("order-async-");
        executor.initialize();
        return executor;
    }
}
