package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.ApplicationProperties;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
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

    public void publishDependencyCreatedEvent(FeatureDependency dependency, String createdBy) {
        DependencyCreatedEvent event = new DependencyCreatedEvent(
                dependency.getFeature().getCode(),
                dependency.getDependsOnFeature().getCode(),
                dependency.getDependencyType(),
                dependency.getNotes(),
                createdBy,
                dependency.getCreatedAt());
        kafkaTemplate.send("dependency-events", event);
    }

    public void publishDependencyUpdatedEvent(FeatureDependency dependency, String updatedBy) {
        DependencyUpdatedEvent event = new DependencyUpdatedEvent(
                dependency.getFeature().getCode(),
                dependency.getDependsOnFeature().getCode(),
                dependency.getDependencyType(),
                dependency.getNotes(),
                updatedBy,
                Instant.now());
        kafkaTemplate.send("dependency-events", event);
    }

    public void publishDependencyDeletedEvent(
            String featureCode,
            String dependsOnFeatureCode,
            com.sivalabs.ft.features.domain.models.DependencyType dependencyType,
            String notes,
            String deletedBy) {
        DependencyDeletedEvent event = new DependencyDeletedEvent(
                featureCode, dependsOnFeatureCode, dependencyType, notes, deletedBy, Instant.now());
        kafkaTemplate.send("dependency-events", event);
    }
}
