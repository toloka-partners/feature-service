package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.AssignFeatureToReleasePayload;
import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.api.models.MoveFeaturePayload;
import com.sivalabs.ft.features.api.models.RemoveFeaturePayload;
import com.sivalabs.ft.features.api.models.UpdateFeaturePlanningPayload;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.Commands.AssignFeatureToReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureBetweenReleasesCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureFromReleaseCommand;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
    @Operation(
            summary = "Create a new release",
            description = "Create a new release",
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

    @PutMapping("/{code}")
    @Operation(
            summary = "Update an existing release",
            description = "Update an existing release",
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
    @Operation(
            summary = "Delete an existing release",
            description = "Delete an existing release",
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

    @GetMapping("/{releaseCode}/features")
    @Operation(
            summary = "Get features assigned to a release",
            description =
                    "Get features assigned to a release with optional filtering by planning status, owner, overdue, or blocked",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array = @ArraySchema(schema = @Schema(implementation = FeatureDto.class)))),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
            })
    List<FeatureDto> getReleaseFeatures(
            @PathVariable String releaseCode,
            @RequestParam(required = false) FeaturePlanningStatus planningStatus,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean blocked) {
        var username = SecurityUtils.getCurrentUsername();
        return featureService.findFeaturesByReleaseWithFilters(
                username, releaseCode, planningStatus, owner, overdue, blocked);
    }

    @PostMapping("/{releaseCode}/features")
    @Operation(
            summary = "Assign feature to release",
            description = "Assign a feature to a release with planning details",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or release not found"),
            })
    ResponseEntity<Void> assignFeatureToRelease(
            @PathVariable String releaseCode, @RequestBody @Valid AssignFeatureToReleasePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new AssignFeatureToReleaseCommand(
                releaseCode,
                payload.featureCode(),
                payload.plannedCompletionDate(),
                payload.featureOwner(),
                payload.notes(),
                username);
        featureService.assignFeatureToRelease(cmd);
        log.info("Feature {} assigned to release {} by {}", payload.featureCode(), releaseCode, username);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{releaseCode}/features/{featureCode}/planning")
    @Operation(
            summary = "Update feature planning details",
            description = "Update feature planning details such as dates, planning status, owner, and blockage reason",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or release not found"),
            })
    ResponseEntity<Void> updateFeaturePlanning(
            @PathVariable String releaseCode,
            @PathVariable String featureCode,
            @RequestBody UpdateFeaturePlanningPayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new UpdateFeaturePlanningCommand(
                releaseCode,
                featureCode,
                payload.plannedCompletionDate(),
                payload.planningStatus(),
                payload.featureOwner(),
                payload.blockageReason(),
                username);
        featureService.updateFeaturePlanning(cmd);
        log.info("Feature {} planning updated in release {} by {}", featureCode, releaseCode, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{targetReleaseCode}/features/{featureCode}/move")
    @Operation(
            summary = "Move feature between releases",
            description = "Move a feature from one release to another with a rationale",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or release not found"),
            })
    ResponseEntity<Void> moveFeatureBetweenReleases(
            @PathVariable String targetReleaseCode,
            @PathVariable String featureCode,
            @RequestBody @Valid MoveFeaturePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new MoveFeatureBetweenReleasesCommand(featureCode, targetReleaseCode, payload.rationale(), username);
        featureService.moveFeatureBetweenReleases(cmd);
        log.info("Feature {} moved to release {} by {}", featureCode, targetReleaseCode, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{releaseCode}/features/{featureCode}")
    @Operation(
            summary = "Remove feature from release",
            description = "Remove a feature from a release with a rationale",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Feature or release not found"),
            })
    ResponseEntity<Void> removeFeatureFromRelease(
            @PathVariable String releaseCode,
            @PathVariable String featureCode,
            @RequestBody @Valid RemoveFeaturePayload payload) {
        var username = SecurityUtils.getCurrentUsername();
        var cmd = new RemoveFeatureFromReleaseCommand(releaseCode, featureCode, payload.rationale(), username);
        featureService.removeFeatureFromRelease(cmd);
        log.info("Feature {} removed from release {} by {}", featureCode, releaseCode, username);
        return ResponseEntity.ok().build();
    }
}
