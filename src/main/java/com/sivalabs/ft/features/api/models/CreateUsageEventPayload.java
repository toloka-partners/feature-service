package com.sivalabs.ft.features.api.models;

import com.sivalabs.ft.features.domain.models.UsageEventType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateUsageEventPayload(
        @NotEmpty(message = "Feature code is required") String featureCode,
        @NotEmpty(message = "Product code is required") String productCode,
        @NotNull(message = "Event type is required") UsageEventType eventType,
        String metadata) {}
