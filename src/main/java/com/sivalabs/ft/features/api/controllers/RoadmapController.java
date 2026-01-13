package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.domain.ReportingService;
import com.sivalabs.ft.features.domain.RoadmapService;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roadmap")
@Tag(name = "Roadmap API")
class RoadmapController {
    private static final Logger log = LoggerFactory.getLogger(RoadmapController.class);

    private final RoadmapService roadmapService;
    private final ReportingService reportingService;

    RoadmapController(RoadmapService roadmapService, ReportingService reportingService) {
        this.roadmapService = roadmapService;
        this.reportingService = reportingService;
    }

    @GetMapping("")
    @Operation(
            summary = "Get product roadmap",
            description = "Get product roadmap with releases, features, progress metrics and health indicators",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RoadmapResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            })
    ResponseEntity<RoadmapResponseDto> getRoadmap(
            @RequestParam(value = "productCodes", required = false) String[] productCodes,
            @RequestParam(value = "statuses", required = false) String[] statuses,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestParam(value = "groupBy", required = false) String groupBy,
            @RequestParam(value = "owner", required = false) String owner) {

        try {
            // Validate and convert parameters
            List<String> productCodesList = productCodes != null ? Arrays.asList(productCodes) : null;
            List<String> statusesList = statuses != null ? Arrays.asList(statuses) : null;

            // Validate statuses
            if (statusesList != null) {
                validateStatuses(statusesList);
            }

            // Parse dates
            LocalDate dateFromParsed = dateFrom != null ? LocalDate.parse(dateFrom) : null;
            LocalDate dateToParsed = dateTo != null ? LocalDate.parse(dateTo) : null;

            // Validate date range
            if (dateFromParsed != null && dateToParsed != null && dateFromParsed.isAfter(dateToParsed)) {
                throw new BadRequestException("dateFrom must be before or equal to dateTo");
            }

            // Validate groupBy
            if (groupBy != null) {
                validateGroupBy(groupBy);
            }

            RoadmapResponseDto roadmap = roadmapService.getRoadmap(
                    productCodesList, statusesList, dateFromParsed, dateToParsed, groupBy, owner);

            return ResponseEntity.ok(roadmap);

        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use ISO date format (YYYY-MM-DD)");
        }
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export roadmap as file",
            description = "Export roadmap as CSV or PDF file with all applied filters and sorting",
            responses = {
                @ApiResponse(responseCode = "200", description = "Successful export"),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            })
    ResponseEntity<byte[]> exportRoadmap(
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "productCodes", required = false) String[] productCodes,
            @RequestParam(value = "statuses", required = false) String[] statuses,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestParam(value = "groupBy", required = false) String groupBy,
            @RequestParam(value = "owner", required = false) String owner) {

        try {
            // Validate format
            if (format == null || format.trim().isEmpty()) {
                throw new BadRequestException("format parameter is mandatory");
            }

            if (!format.toUpperCase().matches("^(CSV|PDF)$")) {
                throw new BadRequestException("format must be CSV or PDF (case-insensitive)");
            }

            // Validate and convert parameters (same validation as getRoadmap)
            List<String> productCodesList = productCodes != null ? Arrays.asList(productCodes) : null;
            List<String> statusesList = statuses != null ? Arrays.asList(statuses) : null;

            if (statusesList != null) {
                validateStatuses(statusesList);
            }

            LocalDate dateFromParsed = dateFrom != null ? LocalDate.parse(dateFrom) : null;
            LocalDate dateToParsed = dateTo != null ? LocalDate.parse(dateTo) : null;

            if (dateFromParsed != null && dateToParsed != null && dateFromParsed.isAfter(dateToParsed)) {
                throw new BadRequestException("dateFrom must be before or equal to dateTo");
            }

            if (groupBy != null) {
                validateGroupBy(groupBy);
            }

            // Generate export
            ReportingService.ExportResult exportResult = reportingService.exportRoadmap(
                    format, productCodesList, statusesList, dateFromParsed, dateToParsed, groupBy, owner);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportResult.getFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, exportResult.getContentType());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(exportResult.getContent().length)
                    .body(exportResult.getContent());

        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use ISO date format (YYYY-MM-DD)");
        }
    }

    private void validateStatuses(List<String> statuses) {
        List<String> validStatuses =
                Arrays.asList("DRAFT", "PLANNED", "IN_PROGRESS", "COMPLETED", "DELAYED", "CANCELLED", "RELEASED");

        for (String status : statuses) {
            if (status != null && !validStatuses.contains(status.toUpperCase())) {
                throw new BadRequestException("Invalid status: " + status + ". Valid statuses are: " + validStatuses);
            }
        }
    }

    private void validateGroupBy(String groupBy) {
        List<String> validGroupBy = Arrays.asList("productcode", "status", "owner");

        if (!validGroupBy.contains(groupBy.toLowerCase())) {
            throw new BadRequestException("Invalid groupBy value: " + groupBy
                    + ". Valid values are: productCode, status, owner (case-insensitive)");
        }
    }
}
