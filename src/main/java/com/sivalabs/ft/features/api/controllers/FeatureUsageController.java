package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.CreateUsageEventRequest;
import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.FeatureUsageService;
import com.sivalabs.ft.features.domain.dtos.FeatureStatsDto;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageDto;
import com.sivalabs.ft.features.domain.dtos.ProductStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageStatsDto;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.models.ActionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/usage")
@Tag(name = "Usage Tracking API")
class FeatureUsageController {
    private static final Logger log = LoggerFactory.getLogger(FeatureUsageController.class);
    private final FeatureUsageService featureUsageService;

    FeatureUsageController(FeatureUsageService featureUsageService) {
        this.featureUsageService = featureUsageService;
    }

    @PostMapping("")
    @Operation(
            summary = "Create a new usage event",
            description = "Create a new usage event. Auto-captures userId, timestamp, ipAddress, and userAgent.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Usage event created successfully",
                        headers =
                                @Header(
                                        name = "Location",
                                        required = true,
                                        description = "URI of the created usage event"),
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FeatureUsageDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<FeatureUsageDto> createUsageEvent(
            @RequestBody @Valid CreateUsageEventRequest request, HttpServletRequest httpRequest) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BadRequestException("User not authenticated");
        }

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        FeatureUsage featureUsage = featureUsageService.createUsageEvent(
                username,
                request.featureCode(),
                request.productCode(),
                request.actionType(),
                request.context(),
                ipAddress,
                userAgent);

        if (featureUsage == null) {
            throw new BadRequestException("Usage tracking is disabled");
        }

        FeatureUsageDto dto = new FeatureUsageDto(
                featureUsage.getId(),
                featureUsage.getUserId(),
                featureUsage.getFeatureCode(),
                featureUsage.getProductCode(),
                featureUsage.getActionType(),
                featureUsage.getTimestamp(),
                featureUsage.getContext(),
                featureUsage.getIpAddress(),
                featureUsage.getUserAgent());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(featureUsage.getId())
                .toUri();

        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/feature/{featureCode}/stats")
    @Operation(
            summary = "Get feature statistics",
            description = "Get aggregated statistics for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FeatureStatsDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<FeatureStatsDto> getFeatureStats(
            @PathVariable String featureCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        FeatureStatsDto stats = featureUsageService.getFeatureStats(featureCode, actionType, start, end);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/product/{productCode}/stats")
    @Operation(
            summary = "Get product statistics",
            description = "Get aggregated statistics for a specific product",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProductStatsDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<ProductStatsDto> getProductStats(
            @PathVariable String productCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        ProductStatsDto stats = featureUsageService.getProductStats(productCode, actionType, start, end);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/feature/{featureCode}/events")
    @Operation(
            summary = "Get feature events",
            description = "Get list of usage events for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array =
                                                @ArraySchema(
                                                        schema = @Schema(implementation = FeatureUsageDto.class)))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<List<FeatureUsageDto>> getFeatureEvents(
            @PathVariable String featureCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        List<FeatureUsageDto> events = featureUsageService.getFeatureEvents(featureCode, actionType, start, end);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/product/{productCode}/events")
    @Operation(
            summary = "Get product events",
            description = "Get list of usage events for a specific product",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array =
                                                @ArraySchema(
                                                        schema = @Schema(implementation = FeatureUsageDto.class)))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<List<FeatureUsageDto>> getProductEvents(
            @PathVariable String productCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        List<FeatureUsageDto> events = featureUsageService.getProductEvents(productCode, actionType, start, end);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events")
    @Operation(
            summary = "Get all usage events",
            description = "Get list of all usage events with optional filters",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        array =
                                                @ArraySchema(
                                                        schema = @Schema(implementation = FeatureUsageDto.class)))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<List<FeatureUsageDto>> getAllEvents(
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String featureCode,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        List<FeatureUsageDto> events =
                featureUsageService.getAllEvents(actionType, userId, featureCode, productCode, start, end);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get overall usage statistics",
            description = "Get aggregated statistics across all features and products",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UsageStatsDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<UsageStatsDto> getOverallStats(
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Instant start = parseDate(startDate);
        Instant end = parseDate(endDate);
        validateDateRange(start, end);

        UsageStatsDto stats = featureUsageService.getOverallStats(actionType, start, end);
        return ResponseEntity.ok(stats);
    }

    private Instant parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format: " + dateString + ". Expected ISO 8601 format.");
        }
    }

    private void validateDateRange(Instant startDate, Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
    }
}
