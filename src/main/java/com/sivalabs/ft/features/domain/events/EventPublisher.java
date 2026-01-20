package com.sivalabs.ft.features.domain.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.ApplicationProperties;
import com.sivalabs.ft.features.domain.EventStoreRepository;
import com.sivalabs.ft.features.domain.entities.EventStore;
import com.sivalabs.ft.features.domain.entities.Feature;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties properties;
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    public EventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            ApplicationProperties properties,
            EventStoreRepository eventStoreRepository,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishFeatureCreatedEvent(Feature feature) {
        Long version = getNextVersion(feature.getCode());
        FeatureCreatedEvent event = new FeatureCreatedEvent(
                feature.getId(),
                feature.getCode(),
                feature.getTitle(),
                feature.getDescription(),
                feature.getStatus(),
                feature.getRelease() == null ? null : feature.getRelease().getCode(),
                feature.getAssignedTo(),
                feature.getCreatedBy(),
                feature.getCreatedAt());

        persistEventToStore(event, version);
        kafkaTemplate.send(properties.events().newFeatures(), event);
    }

    @Transactional
    public void publishFeatureUpdatedEvent(Feature feature) {
        Long version = getNextVersion(feature.getCode());
        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                feature.getId(),
                feature.getCode(),
                feature.getTitle(),
                feature.getDescription(),
                feature.getStatus(),
                feature.getRelease() == null ? null : feature.getRelease().getCode(),
                feature.getAssignedTo(),
                feature.getCreatedBy(),
                feature.getCreatedAt(),
                feature.getUpdatedBy(),
                feature.getUpdatedAt());

        persistEventToStore(event, version);
        kafkaTemplate.send(properties.events().updatedFeatures(), event);
    }

    @Transactional
    public void publishFeatureDeletedEvent(Feature feature, String deletedBy, Instant deletedAt) {
        Long version = getNextVersion(feature.getCode());
        FeatureDeletedEvent event = new FeatureDeletedEvent(
                feature.getId(),
                feature.getCode(),
                feature.getTitle(),
                feature.getDescription(),
                feature.getStatus(),
                feature.getRelease() == null ? null : feature.getRelease().getCode(),
                feature.getAssignedTo(),
                feature.getCreatedBy(),
                feature.getCreatedAt(),
                feature.getUpdatedBy(),
                feature.getUpdatedAt(),
                deletedBy,
                deletedAt);

        persistEventToStore(event, version);
        kafkaTemplate.send(properties.events().deletedFeatures(), event);
    }

    private void persistEventToStore(DomainEvent event, Long version) {
        try {
            // Check for idempotency
            if (eventStoreRepository.existsByEventId(event.getEventId())) {
                logger.warn("Event with ID {} already exists, skipping persistence", event.getEventId());
                return;
            }

            String eventData = objectMapper.writeValueAsString(event);
            Map<String, Object> metadata = createMetadata(event);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            EventStore eventStore = new EventStore(
                    event.getEventId(),
                    event.getEventType(),
                    event.getCode(),
                    event.getAggregateType(),
                    eventData,
                    metadataJson,
                    event.getTimestamp() != null ? event.getTimestamp() : Instant.now(),
                    version);

            eventStoreRepository.save(eventStore);
            logger.debug("Persisted event: {} for aggregate: {}", event.getEventType(), event.getCode());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to persist event", e);
        }
    }

    private Map<String, Object> createMetadata(DomainEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventId", event.getEventId());
        metadata.put("eventType", event.getEventType());
        metadata.put("code", event.getCode());
        metadata.put("aggregateType", event.getAggregateType());
        metadata.put(
                "timestamp",
                event.getTimestamp() != null
                        ? event.getTimestamp().toString()
                        : Instant.now().toString());
        metadata.put("version", event.getVersion());
        return metadata;
    }

    private Long getNextVersion(String aggregateId) {
        Long maxVersion = eventStoreRepository.findMaxVersionByCode(aggregateId);
        return maxVersion == null ? 1L : maxVersion + 1L;
    }
}
