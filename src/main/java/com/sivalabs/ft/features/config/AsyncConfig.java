package com.sivalabs.ft.features.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for async task execution (email sending)
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot autoconfiguration will handle thread pool setup
    // based on spring.task.execution.* properties in application.properties
}
