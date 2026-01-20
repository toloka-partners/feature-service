package com.sivalabs.ft.features.config;

import com.sivalabs.ft.features.ApplicationProperties;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class for asynchronous event processing.
 * Configures a custom ThreadPoolTaskExecutor for handling async events
 * and sets up the ApplicationEventMulticaster to use this executor.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    private final ApplicationProperties properties;

    public AsyncConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates and configures a ThreadPoolTaskExecutor for async event processing.
     * Thread pool parameters are configurable via application properties.
     *
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        ApplicationProperties.AsyncProperties asyncProps = properties.async();

        executor.setCorePoolSize(asyncProps.corePoolSize());
        executor.setMaxPoolSize(asyncProps.maxPoolSize());
        executor.setQueueCapacity(asyncProps.queueCapacity());
        executor.setThreadNamePrefix(asyncProps.threadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        logger.info(
                "Initialized event task executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProps.corePoolSize(),
                asyncProps.maxPoolSize(),
                asyncProps.queueCapacity());

        return executor;
    }

    /**
     * Configures the ApplicationEventMulticaster to use the custom thread pool
     * for asynchronous event delivery.
     *
     * @param eventTaskExecutor the custom ThreadPoolTaskExecutor
     * @return configured ApplicationEventMulticaster
     */
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster(ThreadPoolTaskExecutor eventTaskExecutor) {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(eventTaskExecutor);

        logger.info("Configured ApplicationEventMulticaster with custom task executor");

        return eventMulticaster;
    }

    /**
     * Returns the default async executor for @Async methods.
     *
     * @return the event task executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return eventTaskExecutor();
    }

    /**
     * Handles uncaught exceptions in async methods.
     *
     * @return AsyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error(
                    "Uncaught exception in async method '{}': {}", method.getName(), throwable.getMessage(), throwable);
        };
    }
}
