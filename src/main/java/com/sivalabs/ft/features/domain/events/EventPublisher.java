package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.ApplicationProperties;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties properties;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publishFeatureCreatedEvent(Feature feature) {
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
        kafkaTemplate.send(properties.events().newFeatures(), event);
    }

    public void publishFeatureUpdatedEvent(Feature feature) {
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
        kafkaTemplate.send(properties.events().updatedFeatures(), event);
    }

    public void publishFeatureDeletedEvent(Feature feature, String deletedBy, Instant deletedAt) {
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
        kafkaTemplate.send(properties.events().deletedFeatures(), event);
    }

    public void publishReleaseCreatedEvent(Release release, String eventId) {
        ReleaseCreatedEvent event = new ReleaseCreatedEvent(
                eventId,
                release.getId(),
                release.getCode(),
                release.getDescription(),
                release.getStatus(),
                release.getProduct().getCode(),
                release.getCreatedBy(),
                release.getCreatedAt());
        kafkaTemplate.send(properties.events().newReleases(), event);
    }

    public void publishReleaseUpdatedEvent(Release release, String eventId) {
        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
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
                release.getUpdatedAt());
        kafkaTemplate.send(properties.events().updatedReleases(), event);
    }

    public void publishReleaseDeletedEvent(Release release, String deletedBy, Instant deletedAt, String eventId) {
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
        kafkaTemplate.send(properties.events().deletedReleases(), event);
    }
}
