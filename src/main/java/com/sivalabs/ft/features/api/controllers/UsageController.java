package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.models.CreateUsageEventPayload;
import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.UsageEventService;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.ProductUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import com.sivalabs.ft.features.domain.models.UsageEventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Usage Analytics API")
class UsageController {
    private static final Logger log = LoggerFactory.getLogger(UsageController.class);
    private final UsageEventService usageEventService;

    UsageController(UsageEventService usageEventService) {
        this.usageEventService = usageEventService;
    }

    @PostMapping("")
    @Operation(
            summary = "Post a feature usage event",
            description = "Post a feature usage event for analytics tracking",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Usage event created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UsageEventDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "404", description = "Feature or Product not found")
            })
    ResponseEntity<UsageEventDto> createUsageEvent(@RequestBody @Valid CreateUsageEventPayload payload) {
        var userId = SecurityUtils.getCurrentUsername();
        log.info("Creating usage event for feature {} by user {}", payload.featureCode(), userId);

        UsageEventDto usageEventDto = usageEventService.createUsageEvent(
                payload.featureCode(), payload.productCode(), payload.eventType(), payload.metadata(), userId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(usageEventDto.id())
                .toUri();

        return ResponseEntity.created(location).body(usageEventDto);
    }

    @GetMapping("/feature/{featureCode}")
    @Operation(
            summary = "Get feature usage statistics",
            description = "Get usage statistics for a specific feature with optional filtering",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = FeatureUsageStatsDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "404", description = "Feature not found")
            })
    ResponseEntity<FeatureUsageStatsDto> getFeatureUsageStats(
            @PathVariable String featureCode,
            @RequestParam(required = false) UsageEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        log.info("Getting usage stats for feature {}", featureCode);

        FeatureUsageStatsDto stats = usageEventService.getFeatureUsageStats(featureCode, eventType, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/feature/{featureCode}/events")
    @Operation(
            summary = "Get feature usage events",
            description = "Get detailed usage events for a specific feature with optional filtering",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "404", description = "Feature not found")
            })
    ResponseEntity<List<UsageEventDto>> getFeatureUsageEvents(
            @PathVariable String featureCode,
            @RequestParam(required = false) UsageEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        log.info("Getting usage events for feature {}", featureCode);

        List<UsageEventDto> events =
                usageEventService.getFeatureUsageEvents(featureCode, eventType, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/product/{productCode}")
    @Operation(
            summary = "Get product usage statistics",
            description = "Get usage statistics for a specific product with optional filtering",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProductUsageStatsDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "404", description = "Product not found")
            })
    ResponseEntity<ProductUsageStatsDto> getProductUsageStats(
            @PathVariable String productCode,
            @RequestParam(required = false) UsageEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        log.info("Getting usage stats for product {}", productCode);

        ProductUsageStatsDto stats = usageEventService.getProductUsageStats(productCode, eventType, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/product/{productCode}/events")
    @Operation(
            summary = "Get product usage events",
            description = "Get detailed usage events for a specific product with optional filtering",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful response"),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "404", description = "Product not found")
            })
    ResponseEntity<List<UsageEventDto>> getProductUsageEvents(
            @PathVariable String productCode,
            @RequestParam(required = false) UsageEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        log.info("Getting usage events for product {}", productCode);

        List<UsageEventDto> events =
                usageEventService.getProductUsageEvents(productCode, eventType, startDate, endDate);
        return ResponseEntity.ok(events);
    }
}
