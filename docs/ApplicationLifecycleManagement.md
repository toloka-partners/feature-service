# Application Lifecycle Management

This document describes the implementation of Spring ApplicationContext lifecycle event listeners for the feature-service application. The implementation handles critical resource initialization and cleanup to ensure reliable application startup and graceful shutdown.

## Overview

The [`ApplicationLifecycleManager`](../src/main/java/com/sivalabs/ft/features/config/ApplicationLifecycleManager.java) component provides:

- **Startup Verification**: Database connectivity and Kafka producer warm-up
- **Graceful Shutdown**: Proper resource cleanup with timeout handling
- **Failure Handling**: Clear error messages and appropriate failure modes

## Architecture

### Core Components

1. **ApplicationLifecycleManager**: Main component handling lifecycle events
2. **ApplicationProperties**: Configuration properties for timeouts and Kafka topics
3. **Event Listeners**: Spring event-driven architecture for startup/shutdown hooks

### Event Handling

- `ContextRefreshedEvent`: Triggered during application startup
- `ContextClosedEvent`: Triggered during application shutdown

## Startup Process

### Startup Flow

1. **ContextRefreshedEvent** is fired when ApplicationContext is initialized
2. **Database Connectivity Verification**:
   - Tests PostgreSQL connection using DataSource
   - Validates connection within 5-second timeout
   - **Failure Mode**: Throws `RuntimeException` with "Database unavailable" message
3. **Kafka Verification**:
   - Tests Kafka connectivity using AdminClient
   - Verifies all required topics exist:
     - `new_features`
     - `updated_features` 
     - `deleted_features`
   - Warms up Kafka producer connections
   - **Failure Modes**:
     - Kafka unavailable: Throws `RuntimeException` with "Kafka unavailable" message
     - Missing topic: Throws `RuntimeException` with "Required Kafka topic not found with topic name: {topic}"

### Configuration

```properties
# Kafka topics (required)
ft.events.new-features=new_features
ft.events.updated-features=updated_features
ft.events.deleted-features=deleted_features

# Lifecycle timeouts (optional, with defaults)
ft.lifecycle.kafka-flush-timeout=10s
ft.lifecycle.total-shutdown-timeout=30s
```

### Error Messages

| Scenario | Error Message |
|----------|---------------|
| Database connection failure | "Database unavailable" |
| Database connection invalid | "Database connection validation failed" |
| Kafka connectivity failure | "Kafka unavailable" |
| Missing Kafka topic | "Required Kafka topic not found with topic name: {topicName}" |
| Producer warm-up failure | "Kafka producer warm-up failed" |

## Shutdown Process

### Shutdown Flow

1. **ContextClosedEvent** is fired when ApplicationContext is closing
2. **Kafka Message Flush**:
   - Flushes all pending messages using configurable timeout
   - Default timeout: 10 seconds (configurable via `ft.lifecycle.kafka-flush-timeout`)
   - **Behavior**: Continues shutdown even if flush fails (no blocking)
3. **Database Cleanup**:
   - Relies on Spring Boot's automatic connection cleanup via DisposableBean

### Timeout Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `ft.lifecycle.kafka-flush-timeout` | 10s | Maximum time to wait for Kafka message flush |
| `ft.lifecycle.total-shutdown-timeout` | 30s | Total shutdown timeout (reserved for future use) |

### Graceful Failure Handling

- Kafka flush failures are logged but don't block shutdown
- Database connection cleanup is handled by Spring Boot automatically
- All errors are logged with appropriate levels

## Usage Examples

### Basic Configuration

```java
@Configuration
@EnableConfigurationProperties(ApplicationProperties.class)
public class ApplicationConfig {
    // ApplicationLifecycleManager is automatically registered as @Component
}
```

### Custom Timeout Configuration

```properties
# Custom timeouts
ft.lifecycle.kafka-flush-timeout=15s
ft.lifecycle.total-shutdown-timeout=45s
```

### Monitoring Startup/Shutdown

```java
@Component
public class LifecycleMonitor {
    
    @EventListener
    public void onStartup(ContextRefreshedEvent event) {
        log.info("Application startup completed at: {}", Instant.now());
    }
    
    @EventListener  
    public void onShutdown(ContextClosedEvent event) {
        log.info("Application shutdown initiated at: {}", Instant.now());
    }
}
```

## Testing

### Unit Tests

The [`ApplicationLifecycleManagerTest`](../src/test/java/com/sivalabs/ft/features/config/ApplicationLifecycleManagerTest.java) covers:

- Successful startup scenarios
- Database failure scenarios
- Kafka failure scenarios (unavailable, missing topics, producer failures)
- Shutdown scenarios (successful and with failures)

### Integration Tests

The [`ApplicationLifecycleManagerIntegrationTest`](../src/test/java/com/sivalabs/ft/features/config/ApplicationLifecycleManagerIntegrationTest.java) validates:

- End-to-end lifecycle management
- Real database and Kafka connectivity
- Message flushing during shutdown
- Event listener coordination

### Failure Scenario Tests

The [`ApplicationLifecycleFailureTest`](../src/test/java/com/sivalabs/ft/features/config/ApplicationLifecycleFailureTest.java) tests:

- Application startup failures with invalid database configuration
- Application startup failures with invalid Kafka configuration
- Proper error message propagation

### Running Tests

```bash
# Run all lifecycle tests
./mvnw test -Dtest="*Lifecycle*"

# Run specific test classes
./mvnw test -Dtest="ApplicationLifecycleManagerTest"
./mvnw test -Dtest="ApplicationLifecycleManagerIntegrationTest"
./mvnw test -Dtest="ApplicationLifecycleFailureTest"
```

## Logging

The ApplicationLifecycleManager uses structured logging for monitoring:

### Startup Logs

```
INFO  - Starting application lifecycle initialization...
INFO  - Verifying database connectivity...
INFO  - Database connectivity verified successfully
INFO  - Verifying Kafka connectivity and topics...
INFO  - Kafka connectivity verified. Available topics: [new_features, updated_features, deleted_features]
INFO  - All required Kafka topics verified: [new_features, updated_features, deleted_features]
INFO  - Warming up Kafka producer...
INFO  - Kafka producer warmed up successfully
INFO  - Kafka connectivity and topic verification completed successfully
INFO  - Application startup completed successfully
```

### Shutdown Logs

```
INFO  - Starting application graceful shutdown...
INFO  - Flushing pending Kafka messages with timeout: PT10S
INFO  - Kafka messages flushed successfully
INFO  - Application shutdown completed successfully
```

### Error Logs

```
ERROR - Database connectivity verification failed: Connection refused
ERROR - Kafka connectivity verification failed: Connection refused
ERROR - Required Kafka topic not found with topic name: missing_topic
ERROR - Failed to flush Kafka messages within timeout: Timeout exception
```

## Best Practices

1. **Environment-Specific Configuration**: Use different timeout values for different environments
2. **Monitoring**: Set up alerts for startup/shutdown failures
3. **Graceful Degradation**: The shutdown process continues even if individual components fail
4. **Resource Management**: Database connections are managed by Spring Boot's connection pooling
5. **Topic Management**: Ensure all required Kafka topics are created before application startup

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Database unavailable" | PostgreSQL not running or wrong connection config | Check database status and connection properties |
| "Kafka unavailable" | Kafka broker not running or wrong bootstrap servers | Check Kafka broker status and bootstrap server config |
| "Required Kafka topic not found" | Topic doesn't exist in Kafka | Create missing topic manually or via configuration |
| Slow shutdown | Large number of pending Kafka messages | Increase `kafka-flush-timeout` or reduce message volume |

### Debug Configuration

```properties
# Enable debug logging for lifecycle management
logging.level.com.sivalabs.ft.features.config.ApplicationLifecycleManager=DEBUG

# Enable Kafka debug logging
logging.level.org.apache.kafka=DEBUG
```

## Future Enhancements

1. **Health Checks**: Integration with Spring Boot Actuator health endpoints
2. **Metrics**: Prometheus metrics for startup/shutdown times
3. **Circuit Breaker**: Fallback mechanisms for non-critical failures
4. **Async Shutdown**: Parallel resource cleanup for faster shutdown times
5. **Custom Validators**: Pluggable validation for additional resources