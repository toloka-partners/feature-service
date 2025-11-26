package com.sivalabs.ft.features.api.models;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for removing a feature from a release.
 */
public record RemoveFeaturePayload(@NotBlank(message = "Rationale is required") String rationale) {}
