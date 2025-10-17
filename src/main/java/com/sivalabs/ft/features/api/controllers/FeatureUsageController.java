package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.CreateUsageEventPayload;
import com.sivalabs.ft.features.domain.FeatureUsageService;
import com.sivalabs.ft.features.domain.dtos.FeatureStatsDto;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageDto;
import com.sivalabs.ft.features.domain.dtos.ProductStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageStatsDto;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.models.ActionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
@Tag(name = "Usage Analytics API")
@Validated
class FeatureUsageController {

    private final FeatureUsageService featureUsageService;

    FeatureUsageController(FeatureUsageService featureUsageService) {
        this.featureUsageService = featureUsageService;
    }

    @PostMapping
    @Operation(
            summary = "Create usage event",
            description = "Post a new usage event",
            responses = {
                @ApiResponse(responseCode = "201", description = "Usage event created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<FeatureUsageDto> createUsageEvent(
            @Valid @RequestBody CreateUsageEventPayload payload,
            Authentication authentication,
            HttpServletRequest request) {
        String userId = authentication.getName();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        FeatureUsageDto createdUsage = featureUsageService.createUsageEvent(
                userId,
                payload.featureCode(),
                payload.productCode(),
                payload.actionType(),
                payload.context(),
                ipAddress,
                userAgent);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUsage.id())
                .toUri();

        return ResponseEntity.created(location).body(createdUsage);
    }

    @GetMapping("/events")
    @Operation(
            summary = "Get usage events",
            description = "Retrieve paginated usage events with optional filters",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
            })
    ResponseEntity<Page<FeatureUsageDto>> getUsageEvents(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String featureCode,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<FeatureUsageDto> usageEvents = featureUsageService.findUsageEvents(
                userId, featureCode, productCode, actionType, startDate, endDate, pageable);
        return ResponseEntity.ok(usageEvents);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get usage statistics",
            description = "Retrieve aggregated usage statistics",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UsageStatsDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request parameters (e.g., invalid date format)"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<UsageStatsDto> getUsageStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        UsageStatsDto stats = featureUsageService.getUsageStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get usage events by user",
            description = "Retrieve usage events for a specific user",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
            })
    ResponseEntity<Page<FeatureUsageDto>> getUserUsage(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeatureUsageDto> usageEvents = featureUsageService.findByUserId(userId, pageable);
        return ResponseEntity.ok(usageEvents);
    }

    @GetMapping("/feature/{featureCode}")
    @Operation(
            summary = "Get usage events by feature",
            description = "Retrieve usage events for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
            })
    ResponseEntity<Page<FeatureUsageDto>> getFeatureUsage(
            @PathVariable String featureCode,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeatureUsageDto> usageEvents = featureUsageService.findByFeatureCode(featureCode, pageable);
        return ResponseEntity.ok(usageEvents);
    }

    @GetMapping("/product/{productCode}")
    @Operation(
            summary = "Get usage events by product",
            description = "Retrieve usage events for a specific product",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
            })
    ResponseEntity<Page<FeatureUsageDto>> getProductUsage(
            @PathVariable String productCode,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeatureUsageDto> usageEvents = featureUsageService.findByProductCode(productCode, pageable);
        return ResponseEntity.ok(usageEvents);
    }

    @GetMapping("/top-features")
    @Operation(
            summary = "Get top features",
            description = "Retrieve most accessed features",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content = @Content(mediaType = "application/json"))
            })
    ResponseEntity<Map<String, Long>> getTopFeatures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") int limit) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        Map<String, Long> topFeatures = featureUsageService.getTopFeatures(startDate, endDate, limit);
        return ResponseEntity.ok(topFeatures);
    }

    @GetMapping("/top-users")
    @Operation(
            summary = "Get top users",
            description = "Retrieve most active users",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content = @Content(mediaType = "application/json"))
            })
    ResponseEntity<Map<String, Long>> getTopUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") int limit) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        Map<String, Long> topUsers = featureUsageService.getTopUsers(startDate, endDate, limit);
        return ResponseEntity.ok(topUsers);
    }

    @GetMapping("/feature/{featureCode}/stats")
    @Operation(
            summary = "Get feature statistics",
            description = "Retrieve aggregated statistics for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FeatureStatsDto.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<FeatureStatsDto> getFeatureStats(
            @PathVariable String featureCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        FeatureStatsDto stats = featureUsageService.getFeatureStats(featureCode, actionType, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/product/{productCode}/stats")
    @Operation(
            summary = "Get product statistics",
            description = "Retrieve aggregated statistics for a specific product",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProductStatsDto.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<ProductStatsDto> getProductStats(
            @PathVariable String productCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        ProductStatsDto stats = featureUsageService.getProductStats(productCode, actionType, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/feature/{featureCode}/events")
    @Operation(
            summary = "Get feature usage events",
            description = "Retrieve usage events for a specific feature",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content = @Content(mediaType = "application/json")),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<List<FeatureUsageDto>> getFeatureEvents(
            @PathVariable String featureCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        List<FeatureUsageDto> events =
                featureUsageService.getFeatureEvents(featureCode, actionType, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/product/{productCode}/events")
    @Operation(
            summary = "Get product usage events",
            description = "Retrieve usage events for a specific product",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content = @Content(mediaType = "application/json")),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    ResponseEntity<List<FeatureUsageDto>> getProductEvents(
            @PathVariable String productCode,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        List<FeatureUsageDto> events =
                featureUsageService.getProductEvents(productCode, actionType, startDate, endDate);
        return ResponseEntity.ok(events);
    }
}
