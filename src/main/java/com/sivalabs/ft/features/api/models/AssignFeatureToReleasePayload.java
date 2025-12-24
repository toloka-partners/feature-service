package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;

public record AssignFeatureToReleasePayload(
        @NotEmpty(message = "Feature code is required") String featureCode,
        LocalDate plannedCompletionDate,
        String featureOwner,
        String notes) {}
