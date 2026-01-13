package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public record RoadmapFiltersDto(
        List<String> productCodes,
        List<String> statuses,
        LocalDate dateFrom,
        LocalDate dateTo,
        String groupBy,
        String owner)
        implements Serializable {}
