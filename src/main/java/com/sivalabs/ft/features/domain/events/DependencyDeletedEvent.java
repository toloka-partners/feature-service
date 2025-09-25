package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;

public class DependencyDeletedEvent extends DependencyEvent {
    public DependencyDeletedEvent(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String deletedBy,
            Instant deletedAt) {
        super(featureCode, dependsOnFeatureCode, dependencyType, notes, deletedBy, deletedAt, EventType.DELETED);
    }
}
