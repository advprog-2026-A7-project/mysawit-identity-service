package com.mysawit.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EVENT_EXECUTOR = "eventExecutor";

    @Bean(name = EVENT_EXECUTOR)
    public TaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-exec-");
        executor.setRejectedExecutionHandler((r, ex) -> {
            org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                    .warn("Event task rejected by executor (queue full): {}", r);
        });
        executor.initialize();
        return executor;
    }
}
