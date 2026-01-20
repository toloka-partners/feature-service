package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.domain.EventStoreService;
import com.sivalabs.ft.features.domain.entities.EventStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Event Replay", description = "APIs for querying and replaying feature events")
public class EventReplayController {

    private final EventStoreService eventStoreService;

    public EventReplayController(EventStoreService eventStoreService) {
        this.eventStoreService = eventStoreService;
    }

    @GetMapping("/feature/{featureCode}")
    @Operation(summary = "Get events for a specific feature")
    public ResponseEntity<List<EventStore>> getEventsByFeature(
            @Parameter(description = "Feature code") @PathVariable String featureCode) {
        List<EventStore> events = eventStoreService.getEventsByFeature(featureCode);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get events within a time range")
    public ResponseEntity<List<EventStore>> getEventsByTimeRange(
            @Parameter(description = "Start time (ISO 8601 format)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant fromTime,
            @Parameter(description = "End time (ISO 8601 format)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant toTime) {
        List<EventStore> events = eventStoreService.getEventsByTimeRange(fromTime, toTime);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/feature/{featureCode}/time-range")
    @Operation(summary = "Get events for a specific feature within a time range")
    public ResponseEntity<List<EventStore>> getEventsByFeatureAndTimeRange(
            @Parameter(description = "Feature code") @PathVariable String featureCode,
            @Parameter(description = "Start time (ISO 8601 format)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant fromTime,
            @Parameter(description = "End time (ISO 8601 format)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant toTime) {
        List<EventStore> events = eventStoreService.getEventsByFeatureAndTimeRange(featureCode, fromTime, toTime);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/replay/feature/{featureCode}")
    @Operation(summary = "Replay events for a specific feature within a time range")
    public ResponseEntity<ReplayResponse> replayEventsByFeature(
            @Parameter(description = "Feature code") @PathVariable String featureCode,
            @Valid @RequestBody ReplayRequest request) {
        EventStoreService.ReplayResult result = eventStoreService.replayEventsByFeatureAndTimeRange(
                featureCode, request.fromTime(), request.toTime(), request.dryRun());
        return ResponseEntity.ok(new ReplayResponse(result));
    }

    @PostMapping("/replay/features")
    @Operation(summary = "Replay events for multiple features within a time range")
    public ResponseEntity<ReplayResponse> replayEventsByFeatures(@Valid @RequestBody ReplayFeaturesRequest request) {
        EventStoreService.ReplayResult result = eventStoreService.replayEventsByFeaturesAndTimeRange(
                request.featureCodes(), request.fromTime(), request.toTime(), request.dryRun());
        return ResponseEntity.ok(new ReplayResponse(result));
    }

    @PostMapping("/replay/time-range")
    @Operation(summary = "Replay all events within a time range")
    public ResponseEntity<ReplayResponse> replayEventsByTimeRange(@Valid @RequestBody ReplayRequest request) {
        EventStoreService.ReplayResult result =
                eventStoreService.replayEventsByTimeRange(request.fromTime(), request.toTime(), request.dryRun());
        return ResponseEntity.ok(new ReplayResponse(result));
    }

    /**
     * Request payload for replaying events with time range
     */
    public record ReplayRequest(
            @NotNull @Parameter(description = "Start time (ISO 8601 format)") Instant fromTime,
            @NotNull @Parameter(description = "End time (ISO 8601 format)") Instant toTime,
            @Parameter(description = "Dry run mode (default: false)") boolean dryRun) {}

    /**
     * Request payload for replaying events for specific features
     */
    public record ReplayFeaturesRequest(
            @NotEmpty @Parameter(description = "Set of feature codes") Set<String> featureCodes,
            @NotNull @Parameter(description = "Start time (ISO 8601 format)") Instant fromTime,
            @NotNull @Parameter(description = "End time (ISO 8601 format)") Instant toTime,
            @Parameter(description = "Dry run mode (default: false)") boolean dryRun) {}

    /**
     * Response for replay operations
     */
    public record ReplayResponse(
            int totalEvents, int successCount, int failureCount, boolean hasErrors, List<String> errors) {
        public ReplayResponse(EventStoreService.ReplayResult result) {
            this(
                    result.getTotalCount(),
                    result.getSuccessCount(),
                    result.getFailureCount(),
                    result.hasErrors(),
                    result.getErrors());
        }
    }
}
