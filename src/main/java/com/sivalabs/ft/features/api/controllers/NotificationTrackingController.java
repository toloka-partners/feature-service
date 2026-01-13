package com.sivalabs.ft.features.api.controllers;

import com.sivalabs.ft.features.domain.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for email notification tracking pixel
 * This endpoint must be publicly accessible for email clients
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Tracking", description = "Public API for email read tracking")
public class NotificationTrackingController {
    private static final Logger log = LoggerFactory.getLogger(NotificationTrackingController.class);

    // 1x1 transparent GIF image (base64 encoded)
    private static final byte[] TRANSPARENT_GIF =
            Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final NotificationService notificationService;

    public NotificationTrackingController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{id}/read")
    @Operation(
            summary = "Email read tracking pixel",
            description =
                    "Public endpoint for tracking email opens. Returns a 1x1 transparent GIF and marks notification as read.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Tracking pixel image returned"),
                @ApiResponse(responseCode = "400", description = "Invalid notification ID format"),
                @ApiResponse(responseCode = "404", description = "Notification not found")
            })
    public ResponseEntity<byte[]> trackEmailRead(@PathVariable String id) {
        try {
            // Parse and validate UUID
            UUID notificationId = UUID.fromString(id);

            // Check if notification exists
            boolean exists = notificationService.notificationExists(notificationId);
            if (!exists) {
                log.warn("Tracking pixel accessed for non-existent notification: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Notification not found".getBytes());
            }

            // Mark notification as read (idempotent)
            notificationService.markAsReadViaTrackingPixel(notificationId);

            log.debug("Tracking pixel served for notification: {}", notificationId);

            // Return 1x1 transparent GIF with cache control headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/gif"))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(TRANSPARENT_GIF);

        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            log.warn("Invalid UUID format in tracking pixel request: {}", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Invalid notification ID format".getBytes());
        }
    }
}
