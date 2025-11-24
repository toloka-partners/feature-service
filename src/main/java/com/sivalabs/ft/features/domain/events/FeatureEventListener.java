package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.FeatureService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FeatureEventListener {
    private static final Logger log = LoggerFactory.getLogger(FeatureEventListener.class);

    private final FeatureService featureService;
    private final Counter eventCounter;
    private final Counter batchCounter;
    private final Timer eventProcessingTimer;
    private final Timer batchProcessingTimer;

    public FeatureEventListener(FeatureService featureService, MeterRegistry meterRegistry) {
        this.featureService = featureService;
        this.eventCounter = Counter.builder("feature.events.processed")
                .description("Total number of feature events processed")
                .tag("type", "all")
                .register(meterRegistry);
        this.batchCounter = Counter.builder("feature.events.batches")
                .description("Total number of event batches processed")
                .register(meterRegistry);
        this.eventProcessingTimer = Timer.builder("feature.events.processing.time")
                .description("Time taken to process individual feature events")
                .register(meterRegistry);
        this.batchProcessingTimer = Timer.builder("feature.events.batch.processing.time")
                .description("Time taken to process event batches")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = {"${ft.events.new-features}", "${ft.events.updated-features}", "${ft.events.deleted-features}"},
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleFeatureEvents(List<ConsumerRecord<String, Object>> records) {

        Instant batchStartTime = Instant.now();
        log.info(
                "Starting batch processing: {} events from topics: {}",
                records.size(),
                records.stream().map(ConsumerRecord::topic).distinct().toList());

        for (ConsumerRecord<String, Object> record : records) {
            Object event = record.value();
            String topic = record.topic();
            int partition = record.partition();
            long offset = record.offset();
            long timestamp = record.timestamp();

            try {
                eventProcessingTimer.record(() -> {
                    featureService.handleFeatureEvent(event);
                });

                eventCounter.increment();

                // Calculate latency
                long latencyMs = System.currentTimeMillis() - timestamp;
                log.debug(
                        "Event processed [topic={}, partition={}, offset={}, latency={}ms]: {}",
                        topic,
                        partition,
                        offset,
                        latencyMs,
                        event.getClass().getSimpleName());
            } catch (Exception e) {
                log.error(
                        "Error processing event [topic={}, partition={}, offset={}]: {}",
                        topic,
                        partition,
                        offset,
                        event,
                        e);
                // Continue processing remaining events in batch
            }
        }

        batchCounter.increment();
        Duration batchDuration = Duration.between(batchStartTime, Instant.now());
        batchProcessingTimer.record(batchDuration);

        log.info(
                "Batch processing completed: {} events processed in {}ms (avg: {}ms per event)",
                records.size(),
                batchDuration.toMillis(),
                records.isEmpty() ? 0 : batchDuration.toMillis() / records.size());
    }
}
