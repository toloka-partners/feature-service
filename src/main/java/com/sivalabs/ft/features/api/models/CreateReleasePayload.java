package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateReleasePayload(
        @NotEmpty(message = "Product code is required") String productCode,
        @Size(max = 50, message = "Release code cannot exceed 50 characters") @NotEmpty(message = "Release code is required") String code,
        String description,
        Instant plannedStartDate,
        Instant plannedReleaseDate,
        @Size(max = 255, message = "Owner cannot exceed 255 characters") String owner,
        String notes) {}
