package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;

public record RoadmapSummaryDto(
        Integer totalReleases,
        Integer completedReleases,
        Integer draftReleases,
        Integer totalFeatures,
        Double overallCompletionPercentage)
        implements Serializable {}
