package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for moving a feature to another release.
 */
public record MoveFeaturePayload(@NotBlank(message = "Rationale is required") String rationale) {}
