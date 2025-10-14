package com.sivalabs.ft.features.api.models;

import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateReleasePayload(
        String description,
        ReleaseStatus status,
        Instant releasedAt,
        Instant plannedStartDate,
        Instant plannedReleaseDate,
        Instant actualReleaseDate,
        @Size(max = 255, message = "Owner cannot exceed 255 characters") String owner,
        String notes) {}
