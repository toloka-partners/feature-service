package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.api.utils.SecurityUtils;
import com.sivalabs.ft.features.domain.ReportingService;
import com.sivalabs.ft.features.domain.RoadmapService;
import com.sivalabs.ft.features.domain.dtos.MultiProductRoadmapResponseDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapByOwnerResponseDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import com.sivalabs.ft.features.domain.models.GroupBy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roadmap")
@Tag(name = "Roadmap API")
class RoadmapController {
    private final RoadmapService roadmapService;
    private final ReportingService reportingService;

    RoadmapController(RoadmapService roadmapService, ReportingService reportingService) {
        this.roadmapService = roadmapService;
        this.reportingService = reportingService;
    }

    @GetMapping("")
    @Operation(
            summary = "Get product roadmap",
            description =
                    "Get product roadmap with releases, features, progress metrics, and health indicators. Supports filtering by productCode, date range, and completion status.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoadmapResponseDto.class)))
            })
    RoadmapResponseDto getRoadmap(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean includeCompleted,
            @RequestParam(required = false) GroupBy groupBy,
            @RequestParam(required = false) String owner) {
        String username = SecurityUtils.getCurrentUsername();
        return roadmapService.getRoadmap(productCode, startDate, endDate, includeCompleted, groupBy, owner, username);
    }

    @GetMapping("/multi-product")
    @Operation(
            summary = "Get multi-product roadmap",
            description = "Get aggregated roadmap across multiple products",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = MultiProductRoadmapResponseDto.class)))
            })
    MultiProductRoadmapResponseDto getMultiProductRoadmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean includeCompleted,
            @RequestParam(required = false) GroupBy groupBy) {
        String username = SecurityUtils.getCurrentUsername();
        return roadmapService.getMultiProductRoadmap(startDate, endDate, includeCompleted, groupBy, username);
    }

    @GetMapping("/by-owner")
    @Operation(
            summary = "Get roadmap filtered by owner",
            description = "Get roadmap filtered by the specified owner/assignee",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoadmapByOwnerResponseDto.class)))
            })
    RoadmapByOwnerResponseDto getRoadmapByOwner(
            @RequestParam String owner,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean includeCompleted,
            @RequestParam(required = false) GroupBy groupBy) {
        String username = SecurityUtils.getCurrentUsername();
        return roadmapService.getRoadmapByOwner(
                owner, productCode, startDate, endDate, includeCompleted, groupBy, username);
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export roadmap",
            description =
                    "Export roadmap in CSV or PDF format. Supports filtering by productCode(s), date range, and completion status.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content = {@Content(mediaType = "text/csv"), @Content(mediaType = "application/pdf")}),
                @ApiResponse(responseCode = "400", description = "Invalid format specified")
            })
    ResponseEntity<byte[]> exportRoadmap(
            @RequestParam String format,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) List<String> productCodes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean includeCompleted,
            @RequestParam(required = false) GroupBy groupBy) {
        String username = SecurityUtils.getCurrentUsername();
        ReportingService.ExportResult result = reportingService.exportRoadmap(
                format, productCode, productCodes, startDate, endDate, includeCompleted, groupBy, username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}
