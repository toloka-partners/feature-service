package com.sivalabs.ft.features.domain.dtos;

import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;

public record DependencyDto(
        Long id,
        String featureCode,
        String dependsOnFeatureCode,
        DependencyType dependencyType,
        String notes,
        Instant createdAt) {}
