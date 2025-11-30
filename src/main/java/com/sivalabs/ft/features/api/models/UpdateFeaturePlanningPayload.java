package com.sivalabs.ft.features.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import java.time.Instant;

/**
 * Payload for updating feature planning details.
 */
public record UpdateFeaturePlanningPayload(
        Instant plannedCompletionDate,
        @JsonProperty("status") FeaturePlanningStatus planningStatus,
        String featureOwner,
        String blockageReason,
        String notes) {}
