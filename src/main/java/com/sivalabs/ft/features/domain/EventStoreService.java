package com.sivalabs.ft.features.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.entities.EventStore;
import com.sivalabs.ft.features.domain.events.DomainEvent;
import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.events.FeatureDeletedEvent;
import com.sivalabs.ft.features.domain.events.FeatureUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventStoreService {
    private static final Logger logger = LoggerFactory.getLogger(EventStoreService.class);

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventStoreService(
            EventStoreRepository eventStoreRepository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Get events for a specific feature within a time range
     */
    @Transactional(readOnly = true)
    public List<EventStore> getEventsByFeatureAndTimeRange(String featureCode, Instant fromTime, Instant toTime) {
        return eventStoreRepository.findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc(featureCode, fromTime, toTime);
    }

    /**
     * Get events for multiple features within a time range
     */
    @Transactional(readOnly = true)
    public List<EventStore> getEventsByFeaturesAndTimeRange(
            Set<String> featureCodes, Instant fromTime, Instant toTime) {
        return eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime).stream()
                .filter(event -> featureCodes.contains(event.getCode()))
                .toList();
    }

    /**
     * Get all events within a time range
     */
    @Transactional(readOnly = true)
    public List<EventStore> getEventsByTimeRange(Instant fromTime, Instant toTime) {
        return eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime);
    }

    /**
     * Get events for a specific feature
     */
    @Transactional(readOnly = true)
    public List<EventStore> getEventsByFeature(String featureCode) {
        return eventStoreRepository.findByCodeOrderByCreatedAtAsc(featureCode);
    }

    /**
     * Replay events by republishing them to Kafka topics (idempotent operation)
     */
    @Transactional
    public ReplayResult replayEvents(List<EventStore> events, boolean dryRun) {
        ReplayResult result = new ReplayResult();

        for (EventStore eventStore : events) {
            try {
                DomainEvent domainEvent = deserializeEvent(eventStore);

                if (!dryRun) {
                    republishEvent(domainEvent, eventStore.getEventType());
                    result.incrementSuccessCount();
                    logger.info("Replayed event: {} for feature: {}", eventStore.getEventType(), eventStore.getCode());
                } else {
                    result.incrementSuccessCount();
                    logger.info(
                            "Dry run - would replay event: {} for feature: {}",
                            eventStore.getEventType(),
                            eventStore.getCode());
                }

            } catch (Exception e) {
                result.incrementFailureCount();
                result.addError("Failed to replay event ID " + eventStore.getEventId() + ": " + e.getMessage());
                logger.error(
                        "Failed to replay event: {} for feature: {}",
                        eventStore.getEventType(),
                        eventStore.getCode(),
                        e);
            }
        }

        return result;
    }

    /**
     * Replay events for a specific feature within a time range
     */
    @Transactional
    public ReplayResult replayEventsByFeatureAndTimeRange(
            String featureCode, Instant fromTime, Instant toTime, boolean dryRun) {
        List<EventStore> events = getEventsByFeatureAndTimeRange(featureCode, fromTime, toTime);
        return replayEvents(events, dryRun);
    }

    /**
     * Replay events for multiple features within a time range
     */
    @Transactional
    public ReplayResult replayEventsByFeaturesAndTimeRange(
            Set<String> featureCodes, Instant fromTime, Instant toTime, boolean dryRun) {
        List<EventStore> events = getEventsByFeaturesAndTimeRange(featureCodes, fromTime, toTime);
        return replayEvents(events, dryRun);
    }

    /**
     * Replay all events within a time range
     */
    @Transactional
    public ReplayResult replayEventsByTimeRange(Instant fromTime, Instant toTime, boolean dryRun) {
        List<EventStore> events = getEventsByTimeRange(fromTime, toTime);
        return replayEvents(events, dryRun);
    }

    private DomainEvent deserializeEvent(EventStore eventStore) throws JsonProcessingException {
        return switch (eventStore.getEventType()) {
            case "FeatureCreatedEvent" -> objectMapper.readValue(eventStore.getEventData(), FeatureCreatedEvent.class);
            case "FeatureUpdatedEvent" -> objectMapper.readValue(eventStore.getEventData(), FeatureUpdatedEvent.class);
            case "FeatureDeletedEvent" -> objectMapper.readValue(eventStore.getEventData(), FeatureDeletedEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + eventStore.getEventType());
        };
    }

    private void republishEvent(DomainEvent event, String eventType) {
        String topic =
                switch (eventType) {
                    case "FeatureCreatedEvent" -> "feature-created-replay";
                    case "FeatureUpdatedEvent" -> "feature-updated-replay";
                    case "FeatureDeletedEvent" -> "feature-deleted-replay";
                    default -> "unknown-event-replay";
                };

        kafkaTemplate.send(topic, event);
    }

    /**
     * Result class for replay operations
     */
    public static class ReplayResult {
        private int successCount = 0;
        private int failureCount = 0;
        private final List<String> errors = new java.util.ArrayList<>();

        public void incrementSuccessCount() {
            successCount++;
        }

        public void incrementFailureCount() {
            failureCount++;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getTotalCount() {
            return successCount + failureCount;
        }

        public boolean hasErrors() {
            return failureCount > 0;
        }
    }
}
