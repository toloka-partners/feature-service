package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;

public record ProgressMetricsDto(
        Integer totalFeatures,
        Integer completedFeatures,
        Integer inProgressFeatures,
        Integer newFeatures,
        Integer onHoldFeatures,
        Double completionPercentage)
        implements Serializable {}
