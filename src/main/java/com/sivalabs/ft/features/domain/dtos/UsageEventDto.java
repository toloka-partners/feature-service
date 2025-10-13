package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.time.Instant;

public record UsageEventDto(
        Long id,
        String featureCode,
        String productCode,
        String userId,
        String eventType,
        String metadata,
        Instant createdAt)
        implements Serializable {}
