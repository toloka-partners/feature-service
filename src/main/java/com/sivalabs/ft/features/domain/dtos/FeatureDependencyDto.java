package com.sivalabs.ft.features.domain.dtos;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.io.Serializable;
import java.time.Instant;

public record FeatureDependencyDto(
        Long id,
        FeatureDto feature,
        FeatureDto dependsOnFeature,
        DependencyType dependencyType,
        String notes,
        Instant createdAt)
        implements Serializable {}
