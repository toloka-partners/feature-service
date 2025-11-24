package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.AssignFeaturePayload;
import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.api.models.MoveFeaturePayload;
import com.sivalabs.ft.features.api.models.RemoveFeaturePayload;
import com.sivalabs.ft.features.api.models.UpdateFeaturePlanningPayload;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.Commands.AssignFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeaturePlanningCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.ReleaseService;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/releases")
@Tag(name = "Releases API")
class ReleaseController {
    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);
    private final ReleaseService releaseService;
    private final FeatureService featureService;

    ReleaseController(ReleaseService releaseService, FeatureService featureService) {
        this.releaseService = releaseService;
        this.featureService = featureService;
    }

    @GetMapping("")
    @Operation(
            summary = "Find releases by product code",
            description = "Find releases by product code",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array = @ArraySchema(schema = @Schema(implementation = ReleaseDto.class))))
            })
    List<ReleaseDto> getProductReleases(@RequestParam("productCode") String productCode) {
        return releaseService.findReleasesByProductCode(productCode);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Find release by code",
            description = "Find release by code",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ReleaseDto.class))),
                @ApiResponse(responseCode = "404", description = "Release not found")
            })
    ResponseEntity<ReleaseDto> getRelease(@PathVariable String code) {
        return releaseService
                .findReleaseByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Create a new release",
            description = "Create a new release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Successful response",
                        headers =
                                @Header(
                                        name = "Location",
                                        required = true,
                                        description = "URI of the created release")),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
            })
    ResponseEntity<Void> createRelease(@RequestBody @Valid CreateReleasePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new CreateReleaseCommand(payload.productCode(), payload.code(), payload.description(), username);
        String code = releaseService.createRelease(cmd);
        log.info("Created release with code {}", code);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(code)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{releaseCode}/features")
    @Operation(
            summary = "Get features for a release with optional filters",
            description =
                    "Get all features assigned to a release with optional filters for status, owner, overdue, and blocked",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array = @ArraySchema(schema = @Schema(implementation = FeatureDto.class))))
            })
    List<FeatureDto> getReleaseFeatures(
            @PathVariable String releaseCode,
            @RequestParam(required = false) FeaturePlanningStatus status,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false, defaultValue = "false") boolean blocked) {
        var username = SecurityUtils.getCurrentUsername();
        return featureService.findFeaturesByReleaseWithFilters(username, releaseCode, status, owner, overdue, blocked);
    }

    @PostMapping("/{releaseCode}/features")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Assign a feature to a release",
            description = "Assign a feature to a release with planning details",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "204", description = "Feature assigned successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or Release not found")
            })
    ResponseEntity<Void> assignFeatureToRelease(
            @PathVariable String releaseCode, @RequestBody @Valid AssignFeaturePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new AssignFeatureCommand(
                releaseCode,
                payload.featureCode(),
                payload.plannedCompletionDate(),
                payload.featureOwner(),
                payload.notes(),
                username);
        featureService.assignFeatureToRelease(cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{releaseCode}/features/{featureCode}/planning")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update feature planning details",
            description = "Update planning details for a feature in a release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "204", description = "Planning details updated successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request or status transition"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature not found")
            })
    ResponseEntity<Void> updateFeaturePlanning(
            @PathVariable String releaseCode,
            @PathVariable String featureCode,
            @RequestBody UpdateFeaturePlanningPayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new UpdateFeaturePlanningCommand(
                featureCode,
                payload.plannedCompletionDate(),
                payload.planningStatus(),
                payload.featureOwner(),
                payload.blockageReason(),
                payload.notes(),
                username);
        featureService.updateFeaturePlanning(cmd);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{targetReleaseCode}/features/{featureCode}/move")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Move a feature to another release",
            description = "Move a feature from its current release to the target release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "204", description = "Feature moved successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or Release not found")
            })
    ResponseEntity<Void> moveFeatureBetweenReleases(
            @PathVariable String targetReleaseCode,
            @PathVariable String featureCode,
            @RequestBody @Valid MoveFeaturePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new MoveFeatureCommand(featureCode, targetReleaseCode, payload.rationale(), username);
        featureService.moveFeatureBetweenReleases(cmd);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{releaseCode}/features/{featureCode}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Remove a feature from a release",
            description = "Remove a feature from its assigned release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "204", description = "Feature removed successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature not found")
            })
    ResponseEntity<Void> removeFeatureFromRelease(
            @PathVariable String releaseCode,
            @PathVariable String featureCode,
            @RequestBody @Valid RemoveFeaturePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new RemoveFeatureCommand(featureCode, payload.rationale(), username);
        featureService.removeFeatureFromRelease(cmd);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Update an existing release",
            description = "Update an existing release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
            })
    void updateRelease(@PathVariable String code, @RequestBody UpdateReleasePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd =
                new UpdateReleaseCommand(code, payload.description(), payload.status(), payload.releasedAt(), username);
        releaseService.updateRelease(cmd);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete an existing release",
            description = "Delete an existing release",
            security = @SecurityRequirement(name = "oauth2"),
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
            })
    ResponseEntity<Void> deleteRelease(@PathVariable String code) {
        if (!releaseService.isReleaseExists(code)) {
            return ResponseEntity.notFound().build();
        }
        releaseService.deleteRelease(code);
        return ResponseEntity.ok().build();
    }
}
