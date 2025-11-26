package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.ApplicationProperties;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties properties;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publishFeatureCreatedEvent(String eventId, Feature feature) {
        FeatureCreatedEvent event = new FeatureCreatedEvent(
                eventId,
                feature.getId(),
                feature.getCode(),
                feature.getTitle(),
                feature.getDescription(),
                feature.getStatus(),
                feature.getRelease() == null ? null : feature.getRelease().getCode(),
                feature.getAssignedTo(),
                feature.getCreatedBy(),
                feature.getCreatedAt());
        kafkaTemplate.send(properties.events().newFeatures(), event);
    }

    public void publishFeatureUpdatedEvent(String eventId, Feature feature) {
        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                eventId,
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
        kafkaTemplate.send(properties.events().updatedFeatures(), event);
    }

    public void publishFeatureDeletedEvent(String eventId, Feature feature, String deletedBy, Instant deletedAt) {
        FeatureDeletedEvent event = new FeatureDeletedEvent(
                eventId,
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
        kafkaTemplate.send(properties.events().deletedFeatures(), event);
    }

    public void publishReleaseCreatedEvent(String eventId, Release release) {
        ReleaseCreatedEvent event = new ReleaseCreatedEvent(
                eventId,
                release.getId(),
                release.getCode(),
                release.getDescription(),
                release.getStatus(),
                release.getProduct().getCode(),
                release.getCreatedBy(),
                release.getCreatedAt());
        log.info(
                "Publishing ReleaseCreatedEvent to topic: {}, eventId: {}",
                properties.events().releases(),
                eventId);
        kafkaTemplate.send(properties.events().releases(), event);
    }

    public void publishReleaseUpdatedEvent(String eventId, Release release, ReleaseStatus previousStatus) {
        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                release.getId(),
                release.getCode(),
                release.getDescription(),
                release.getStatus(),
                previousStatus,
                release.getReleasedAt(),
                release.getProduct().getCode(),
                release.getCreatedBy(),
                release.getCreatedAt(),
                release.getUpdatedBy(),
                release.getUpdatedAt());
        log.info(
                "Publishing ReleaseUpdatedEvent to topic: {}, eventId: {}",
                properties.events().releases(),
                eventId);
        kafkaTemplate.send(properties.events().releases(), event);
    }

    public void publishReleaseDeletedEvent(String eventId, Release release, String deletedBy, Instant deletedAt) {
        ReleaseDeletedEvent event = new ReleaseDeletedEvent(
                eventId,
                release.getId(),
                release.getCode(),
                release.getDescription(),
                release.getStatus(),
                release.getReleasedAt(),
                release.getProduct().getCode(),
                release.getCreatedBy(),
                release.getCreatedAt(),
                release.getUpdatedBy(),
                release.getUpdatedAt(),
                deletedBy,
                deletedAt);
        log.info(
                "Publishing ReleaseDeletedEvent to topic: {}, eventId: {}",
                properties.events().releases(),
                eventId);
        kafkaTemplate.send(properties.events().releases(), event);
    }
}
