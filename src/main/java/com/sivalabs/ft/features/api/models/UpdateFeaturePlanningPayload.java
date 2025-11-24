package com.sivalabs.ft.features.api.models;

import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import java.time.Instant;

/**
 * Payload for updating feature planning details.
 */
public record UpdateFeaturePlanningPayload(
        Instant plannedCompletionDate,
        FeaturePlanningStatus planningStatus,
        String featureOwner,
        String blockageReason,
        String notes) {}
