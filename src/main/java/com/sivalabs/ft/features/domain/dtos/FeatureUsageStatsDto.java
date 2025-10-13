package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.util.Map;

public record FeatureUsageStatsDto(
        String featureCode,
        Long totalEvents,
        Long uniqueUsers,
        Map<String, Long> eventsByType,
        java.time.Instant firstEventAt,
        java.time.Instant lastEventAt)
        implements Serializable {}
