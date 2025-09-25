package com.sivalabs.ft.features.api.models;

import com.sivalabs.ft.features.domain.models.DependencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDependencyPayload(
        @NotNull(message = "Dependency type is required") DependencyType dependencyType,
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters") String notes) {}
