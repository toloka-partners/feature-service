package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;

public record ReleaseUpdatedEvent(
        String eventId,
        Long id,
        String code,
        String description,
        ReleaseStatus status,
        ReleaseStatus previousStatus,
        Instant releasedAt,
        String productCode,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt) {}
