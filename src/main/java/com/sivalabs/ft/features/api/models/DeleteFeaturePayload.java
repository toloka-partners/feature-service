package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotEmpty;

public record DeleteFeaturePayload(@NotEmpty(message = "Event ID is required") String eventId) {}
