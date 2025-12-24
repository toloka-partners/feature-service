package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.util.List;

public record RoadmapByOwnerResponseDto(
        String owner, List<RoadmapItemDto> roadmapItems, RoadmapSummaryDto summary, AppliedFiltersDto appliedFilters)
        implements Serializable {}
