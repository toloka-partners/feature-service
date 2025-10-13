package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUsageEventPayload(
        @NotBlank(message = "Feature code is required") String featureCode,
        @NotBlank(message = "Product code is required") String productCode,
        @NotBlank(message = "Event type is required") @Pattern(regexp = "^[A-Z_]+$", message = "Event type must be uppercase letters and underscores only") String eventType,
        String metadata) {}
