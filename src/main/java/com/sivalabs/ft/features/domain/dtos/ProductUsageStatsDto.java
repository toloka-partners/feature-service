package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.util.Map;

public record ProductUsageStatsDto(
        String productCode,
        Long totalEvents,
        Long uniqueUsers,
        Long uniqueFeatures,
        Map<String, Long> eventsByType,
        java.time.Instant firstEventAt,
        java.time.Instant lastEventAt)
        implements Serializable {}
