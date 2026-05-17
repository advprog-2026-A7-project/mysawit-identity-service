package com.mysawit.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    @Test
    void eventExecutorIsConfigured() {
        AsyncConfig config = new AsyncConfig();
        TaskExecutor executor = config.eventExecutor();

        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
    }

    @Test
    void rejectedExecutionHandlerLogsAndDoesNotThrow() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.eventExecutor();
        ThreadPoolExecutor underlying = executor.getThreadPoolExecutor();

        Runnable rejectedTask = () -> { /* no-op */ };

        assertDoesNotThrow(() ->
                underlying.getRejectedExecutionHandler().rejectedExecution(rejectedTask, underlying));
    }
}
