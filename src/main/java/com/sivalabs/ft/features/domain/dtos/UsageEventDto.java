package com.sivalabs.ft.features.domain.dtos;

import com.sivalabs.ft.features.domain.models.UsageEventType;
import java.io.Serializable;
import java.time.Instant;

public record UsageEventDto(
        Long id,
        String featureCode,
        String productCode,
        String userId,
        UsageEventType eventType,
        String metadata,
        Instant createdAt)
        implements Serializable {}
