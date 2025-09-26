package com.sivalabs.ft.features.domain.dtos;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.io.Serializable;
import java.time.Instant;

public record FeatureDependencyDto(
        Long id,
        String featureCode,
        String featureTitle,
        String dependsOnFeatureCode,
        String dependsOnFeatureTitle,
        DependencyType dependencyType,
        String notes,
        Instant createdAt)
        implements Serializable {}
