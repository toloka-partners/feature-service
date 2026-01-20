package com.sivalabs.ft.features.domain.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureDeletedEvent(
        String eventId,
        Long id,
        String code,
        String title,
        String description,
        FeatureStatus status,
        String releaseCode,
        String assignedTo,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,
        String deletedBy,
        Instant deletedAt,
        Long version)
        implements DomainEvent {

    public FeatureDeletedEvent(
            Long id,
            String code,
            String title,
            String description,
            FeatureStatus status,
            String releaseCode,
            String assignedTo,
            String createdBy,
            Instant createdAt,
            String updatedBy,
            Instant updatedAt,
            String deletedBy,
            Instant deletedAt) {
        this(
                UUID.randomUUID().toString(),
                id,
                code,
                title,
                description,
                status,
                releaseCode,
                assignedTo,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt,
                deletedBy,
                deletedAt,
                1L);
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "FeatureDeletedEvent";
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getAggregateType() {
        return "Feature";
    }

    @Override
    public Instant getTimestamp() {
        return deletedAt;
    }

    @Override
    public Long getVersion() {
        return version;
    }
}
