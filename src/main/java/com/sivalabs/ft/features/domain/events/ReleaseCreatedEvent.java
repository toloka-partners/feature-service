package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;

public record ReleaseCreatedEvent(
        String eventId,
        Long id,
        String code,
        String description,
        ReleaseStatus status,
        String productCode,
        String createdBy,
        Instant createdAt) {}