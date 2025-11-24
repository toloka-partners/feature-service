package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Payload for assigning a feature to a release with planning details.
 */
public record AssignFeaturePayload(
        @NotBlank(message = "Feature code is required") String featureCode,
        Instant plannedCompletionDate,
        String featureOwner,
        String notes) {}
