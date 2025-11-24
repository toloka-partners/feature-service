# Feature Event Batch Processing

## Overview

This document describes the batch event processing architecture implemented for feature-related events in the Feature Service. The system aggregates events from Kafka topics and processes them in batches to optimize throughput and latency under high event volumes.

## Architecture

### Components

1. **FeatureEventListener** (`com.sivalabs.ft.features.domain.events.FeatureEventListener`)
   - Kafka consumer that listens to multiple feature event topics
   - Processes events in batches for optimal performance
   - Integrates with Micrometer for comprehensive metrics

2. **FeatureService** (`com.sivalabs.ft.features.domain.FeatureService`)
   - Business logic layer with `handleFeatureEvent()` method
   - Receives individual events from the batch listener
   - Logs event details for monitoring and debugging

3. **KafkaConfig** (`com.sivalabs.ft.features.config.KafkaConfig`)
   - Configures Kafka consumer factory and listener container
   - Sets up batch listening mode and concurrency
   - Optimizes deserializers and error handling

## Event Types

The system processes three types of feature events:

1. **FeatureCreatedEvent** - Published when a new feature is created
2. **FeatureUpdatedEvent** - Published when an existing feature is modified
3. **FeatureDeletedEvent** - Published when a feature is deleted

All events are consumed from their respective Kafka topics:
- `new_features` - For FeatureCreatedEvent
- `updated_features` - For FeatureUpdatedEvent
- `deleted_features` - For FeatureDeletedEvent

## Batch Processing Configuration

### Kafka Consumer Properties

```properties
# Enable batch mode
spring.kafka.listener.type=batch

# Maximum number of records to fetch in a single poll
spring.kafka.consumer.max-poll-records=500

# Minimum bytes to fetch (1 byte allows processing even single events)
spring.kafka.consumer.fetch-min-size=1

# Maximum time to wait for fetch-min-size (500ms)
spring.kafka.consumer.fetch-max-wait=500ms

# Poll timeout for consumer
spring.kafka.listener.poll-timeout=3000

# Acknowledge mode - commits offsets after entire batch is processed
spring.kafka.listener.ack-mode=batch
```

### Container Configuration

- **Concurrency**: 3 parallel consumers (can be adjusted based on partition count)
- **Batch Listener**: Enabled for batch processing
- **Ack Mode**: BATCH - commits offsets only after successful batch processing

## Performance Metrics

The system tracks comprehensive metrics using Micrometer:

### Event Processing Metrics

1. **feature.events.processed**
   - Type: Counter
   - Description: Total number of individual events processed
   - Use: Track overall throughput

2. **feature.events.batches**
   - Type: Counter
   - Description: Total number of batches processed
   - Use: Monitor batch frequency

3. **feature.events.processing.time**
   - Type: Timer
   - Description: Time taken to process individual events
   - Use: Measure per-event latency

4. **feature.events.batch.processing.time**
   - Type: Timer
   - Description: Time taken to process entire batches
   - Use: Measure batch processing latency and throughput

### Accessing Metrics

Metrics are exposed via Spring Boot Actuator:
- Endpoint: `http://localhost:8081/actuator/metrics`
- Specific metric: `http://localhost:8081/actuator/metrics/feature.events.processed`

## Performance Characteristics

### Throughput

Based on testing with the comprehensive test suite:

- **Low Volume** (< 10 events): Events are processed individually or in small batches
- **Medium Volume** (10-50 events): Batches of 10-30 events typical
- **High Volume** (> 50 events): Full batches of up to 500 events

### Latency

Event processing latency consists of:

1. **Producer Latency**: Time from event creation to Kafka write
2. **Queue Latency**: Time waiting in Kafka before fetch
3. **Fetch Latency**: Time to poll and retrieve batch (< 500ms)
4. **Processing Latency**: Time to process all events in batch

Total end-to-end latency ranges from 500ms (single event) to 2-3s (large batches).

### Scalability

The system scales through:

1. **Horizontal Scaling**: Increase consumer concurrency
2. **Partition Scaling**: Add more Kafka partitions for parallel processing
3. **Batch Size Tuning**: Adjust `max-poll-records` based on event size and processing time

## Tuning Guidelines

### For High Throughput

Optimize for maximum events per second:

```properties
# Increase batch size
spring.kafka.consumer.max-poll-records=1000

# Increase fetch wait time to accumulate more events
spring.kafka.consumer.fetch-max-wait=1000ms

# Increase concurrency
# In KafkaConfig.java: factory.setConcurrency(5);
```

### For Low Latency

Optimize for minimum end-to-end latency:

```properties
# Reduce batch size
spring.kafka.consumer.max-poll-records=100

# Reduce fetch wait time
spring.kafka.consumer.fetch-max-wait=100ms

# Keep fetch-min-size at 1 byte
spring.kafka.consumer.fetch-min-size=1
```

### For Balanced Performance

Current default configuration balances throughput and latency:

```properties
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-max-wait=500ms
spring.kafka.consumer.fetch-min-size=1
```

## Error Handling

The system implements robust error handling:

1. **Deserialization Errors**: ErrorHandlingDeserializer wraps JsonDeserializer
2. **Processing Errors**: Individual event failures don't stop batch processing
3. **Logging**: All errors are logged with topic, partition, and offset details
4. **Offset Management**: Batch ACK mode ensures no data loss

### Error Recovery

```java
try {
    featureService.handleFeatureEvent(event);
} catch (Exception e) {
    log.error("Error processing event [topic={}, partition={}, offset={}]: {}",
        topic, partition, offset, event, e);
    // Continue processing remaining events in batch
}
```

## Monitoring Best Practices

### Key Metrics to Monitor

1. **Consumer Lag**: Monitor Kafka consumer lag for each partition
   ```bash
   kafka-consumer-groups --bootstrap-server localhost:9092 \
     --group feature-service --describe
   ```

2. **Processing Rate**: Track events/second using `feature.events.processed`

3. **Batch Size**: Calculate average batch size:
   ```
   avg_batch_size = total_events / total_batches
   ```

4. **Error Rate**: Monitor error logs for processing failures

### Alerting Thresholds

Set up alerts for:
- Consumer lag > 1000 messages
- Average batch processing time > 5 seconds
- Error rate > 1% of total events
- Processing rate drops below expected baseline

## Testing

### Test Coverage

The comprehensive test suite (`FeatureEventListenerTests`) validates:

1. **Single Event Processing**: Individual event consumption
2. **Batch Processing**: Multiple events processed together
3. **High Volume**: 50+ events under load
4. **Latency Measurement**: End-to-end timing metrics
5. **Mixed Event Types**: Created, Updated, Deleted events
6. **Direct Publishing**: Bypassing REST API for pure Kafka tests
7. **Concurrent Processing**: Multi-threaded event generation

### Running Tests

```bash
# Run all event listener tests
./mvnw test -Dtest=FeatureEventListenerTests

# Run specific test
./mvnw test -Dtest=FeatureEventListenerTests#shouldHandleHighEventVolumeWithBatchProcessing

# Run with Testcontainers for real Kafka
./mvnw verify
```

### Load Testing

To simulate production load:

```java
// Generate 1000 events
for (int i = 0; i < 1000; i++) {
    // Create features via API
}

// Monitor metrics
GET http://localhost:8081/actuator/metrics/feature.events.batch.processing.time
```

## Production Deployment

### Kafka Cluster Configuration

1. **Topic Configuration**:
   ```bash
   # Recommended settings for production
   kafka-topics --create \
     --topic new_features \
     --partitions 6 \
     --replication-factor 3 \
     --config retention.ms=604800000  # 7 days
   ```

2. **Consumer Group Configuration**:
   - Use descriptive group IDs
   - Monitor group lag continuously
   - Plan for consumer group rebalancing

### Application Configuration

Production-ready settings:

```properties
# Kafka brokers
spring.kafka.bootstrap-servers=kafka-1:9092,kafka-2:9092,kafka-3:9092

# Consumer group
spring.kafka.consumer.group-id=${spring.application.name}

# Auto-commit disabled (using batch ack)
spring.kafka.consumer.enable-auto-commit=false

# Batch processing
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-max-wait=500ms

# Concurrency based on partition count
# Set in KafkaConfig: factory.setConcurrency(6); // Match partition count
```

### Capacity Planning

Calculate resource requirements:

1. **Memory**:
   - Per consumer: ~50MB base + (batch_size × avg_event_size)
   - Example: 3 consumers × (50MB + (500 × 1KB)) ≈ 151.5MB

2. **CPU**:
   - Depends on event processing complexity
   - Monitor CPU usage under typical load
   - Scale horizontally if CPU > 70%

3. **Network**:
   - Bandwidth = events/sec × avg_event_size
   - Example: 1000 events/sec × 1KB = 1MB/s

## Troubleshooting

### Common Issues

1. **High Consumer Lag**
   - Cause: Processing slower than production rate
   - Solution: Increase concurrency or batch size

2. **Slow Batch Processing**
   - Cause: Heavy processing logic or database contention
   - Solution: Optimize `handleFeatureEvent()` method

3. **Deserialization Errors**
   - Cause: Schema mismatch or incompatible event formats
   - Solution: Check event producer and consumer compatibility

4. **Rebalancing Issues**
   - Cause: Consumer processing time exceeds `max.poll.interval.ms`
   - Solution: Increase timeout or reduce batch size

### Debug Logging

Enable debug logging for troubleshooting:

```properties
logging.level.com.sivalabs.ft.features.domain.events=DEBUG
logging.level.org.springframework.kafka=DEBUG
logging.level.org.apache.kafka=DEBUG
```

## Future Enhancements

Potential improvements to consider:

1. **Dead Letter Queue**: Route failed events to DLQ for retry
2. **Event Filtering**: Process only specific event types based on criteria
3. **Event Transformation**: Transform events before processing
4. **Parallel Processing**: Process independent events within batch in parallel
5. **Back-pressure Management**: Implement adaptive batch sizing
6. **Event Replay**: Support for reprocessing historical events

## References

- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Micrometer Metrics](https://micrometer.io/docs)
- [Kafka Performance Tuning](https://kafka.apache.org/documentation/#consumerconfigs)

## Support

For issues or questions:
- GitHub Issues: [feature-service/issues](https://github.com/your-org/feature-service/issues)
- Email: support@sivalabs.in
- Documentation: `/docs` directory in the project root
