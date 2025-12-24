package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotEmpty;

public record MoveFeaturePayload(@NotEmpty(message = "Rationale is required") String rationale) {}
