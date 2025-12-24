package com.sivalabs.ft.features.domain.dtos;

import com.sivalabs.ft.features.domain.models.GroupBy;
import java.io.Serializable;
import java.time.LocalDate;

public record AppliedFiltersDto(
        String productCode,
        LocalDate startDate,
        LocalDate endDate,
        Boolean includeCompleted,
        GroupBy groupBy,
        String owner)
        implements Serializable {}
