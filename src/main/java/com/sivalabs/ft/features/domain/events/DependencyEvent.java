package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;

public abstract class DependencyEvent {
    private final String featureCode;
    private final String dependsOnFeatureCode;
    private final DependencyType dependencyType;
    private final String notes;
    private final String performedBy;
    private final Instant performedAt;
    private final EventType eventType;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }

    protected DependencyEvent(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String performedBy,
            Instant performedAt,
            EventType eventType) {
        this.featureCode = featureCode;
        this.dependsOnFeatureCode = dependsOnFeatureCode;
        this.dependencyType = dependencyType;
        this.notes = notes;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.eventType = eventType;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public String getDependsOnFeatureCode() {
        return dependsOnFeatureCode;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public String getNotes() {
        return notes;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format(
                "%s dependency event: %s -> %s (type: %s) by %s at %s",
                eventType, featureCode, dependsOnFeatureCode, dependencyType, performedBy, performedAt);
    }
}
