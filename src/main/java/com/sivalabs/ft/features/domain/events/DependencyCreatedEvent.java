package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;

public class DependencyCreatedEvent extends DependencyEvent {
    public DependencyCreatedEvent(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String createdBy,
            Instant createdAt) {
        super(featureCode, dependsOnFeatureCode, dependencyType, notes, createdBy, createdAt, EventType.CREATED);
    }
}
