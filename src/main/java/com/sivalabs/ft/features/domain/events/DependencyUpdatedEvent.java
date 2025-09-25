package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;

public class DependencyUpdatedEvent extends DependencyEvent {
    public DependencyUpdatedEvent(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String updatedBy,
            Instant updatedAt) {
        super(featureCode, dependsOnFeatureCode, dependencyType, notes, updatedBy, updatedAt, EventType.UPDATED);
    }
}
