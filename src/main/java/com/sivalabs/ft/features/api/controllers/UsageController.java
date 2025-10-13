package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.domain.UsageEvent;
import com.sivalabs.ft.features.domain.UsageEventService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usage")
public class UsageController {
    private final UsageEventService usageEventService;

    public UsageController(UsageEventService usageEventService) {
        this.usageEventService = usageEventService;
    }

    @PostMapping
    public ResponseEntity<?> postUsageEvent(@RequestBody UsageEvent usageEvent) {
        if (usageEvent.getFeatureCode() == null
                || usageEvent.getProductCode() == null
                || usageEvent.getUserId() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        UsageEvent saved = usageEventService.saveUsageEvent(usageEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS')")
    @GetMapping("/feature/{featureCode}")
    public ResponseEntity<List<UsageEvent>> getFeatureUsage(
            @PathVariable String featureCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        List<UsageEvent> events = usageEventService.getFeatureUsage(featureCode, start, end);
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS')")
    @GetMapping("/product/{productCode}")
    public ResponseEntity<List<UsageEvent>> getProductUsage(
            @PathVariable String productCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        List<UsageEvent> events = usageEventService.getProductUsage(productCode, start, end);
        return ResponseEntity.ok(events);
    }
}
