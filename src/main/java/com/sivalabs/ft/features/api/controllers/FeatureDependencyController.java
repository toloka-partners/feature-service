package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.CreateDependencyPayload;
import com.sivalabs.ft.features.api.models.UpdateDependencyPayload;
import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.FeatureDependencyService;
import com.sivalabs.ft.features.domain.dtos.DependencyDto;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.mappers.DependencyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/features/{featureCode}/dependencies")
@Tag(name = "Feature Dependencies API")
class FeatureDependencyController {
    private static final Logger log = LoggerFactory.getLogger(FeatureDependencyController.class);

    private final FeatureDependencyService featureDependencyService;
    private final DependencyMapper dependencyMapper;

    FeatureDependencyController(FeatureDependencyService featureDependencyService, DependencyMapper dependencyMapper) {
        this.featureDependencyService = featureDependencyService;
        this.dependencyMapper = dependencyMapper;
    }

    @GetMapping("")
    @Operation(
            summary = "Get feature dependencies",
            description = "Get all dependencies for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array = @ArraySchema(schema = @Schema(implementation = DependencyDto.class))))
            })
    List<DependencyDto> getFeatureDependencies(@PathVariable String featureCode) {
        List<FeatureDependency> dependencies = featureDependencyService.findDependenciesByFeatureCode(featureCode);
        return dependencies.stream().map(dependencyMapper::toDto).toList();
    }

    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a feature dependency",
            description = "Create a new dependency for a feature",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Dependency created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = DependencyDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature not found")
            })
    ResponseEntity<DependencyDto> createDependency(
            @PathVariable String featureCode, @RequestBody @Valid CreateDependencyPayload payload) {

        String username = SecurityUtils.getCurrentUsername();
        log.info(
                "User {} creating dependency for feature {} -> {}",
                username,
                featureCode,
                payload.dependsOnFeatureCode());

        FeatureDependency dependency = featureDependencyService.createDependency(
                featureCode, payload.dependsOnFeatureCode(), payload.dependencyType(), payload.notes(), username);

        DependencyDto dto = dependencyMapper.toDto(dependency);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{dependsOnFeatureCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a feature dependency",
            description = "Update an existing dependency between features",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Dependency updated successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = DependencyDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Dependency not found")
            })
    ResponseEntity<DependencyDto> updateDependency(
            @PathVariable String featureCode,
            @PathVariable String dependsOnFeatureCode,
            @RequestBody @Valid UpdateDependencyPayload payload) {

        String username = SecurityUtils.getCurrentUsername();
        log.info("User {} updating dependency {} -> {}", username, featureCode, dependsOnFeatureCode);

        FeatureDependency dependency = featureDependencyService.updateDependency(
                featureCode, dependsOnFeatureCode, payload.dependencyType(), payload.notes(), username);

        DependencyDto dto = dependencyMapper.toDto(dependency);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{dependsOnFeatureCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a feature dependency",
            description = "Remove a dependency between features",
            responses = {
                @ApiResponse(responseCode = "204", description = "Dependency deleted successfully"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Dependency not found")
            })
    ResponseEntity<Void> deleteDependency(@PathVariable String featureCode, @PathVariable String dependsOnFeatureCode) {

        String username = SecurityUtils.getCurrentUsername();
        log.info("User {} deleting dependency {} -> {}", username, featureCode, dependsOnFeatureCode);

        featureDependencyService.deleteDependency(featureCode, dependsOnFeatureCode, username);

        return ResponseEntity.noContent().build();
    }
}
