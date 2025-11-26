package com.sivalabs.ft.features.api.models;

import com.sivalabs.ft.features.domain.models.ActionType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateUsageEventPayload(
        @NotNull(message = "actionType is required") ActionType actionType,
        String featureCode,
        String productCode,
        Map<String, Object> context) {}

