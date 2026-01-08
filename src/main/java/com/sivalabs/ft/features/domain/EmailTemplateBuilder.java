package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.NotificationDto;
import com.sivalabs.ft.features.domain.models.NotificationEventType;

/**
 * Utility class for building HTML email templates
 */
public class EmailTemplateBuilder {

    /**
     * Escape HTML characters to prevent XSS
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Build HTML email content with tracking pixel
     */
    public static String buildEmailHtml(NotificationDto notification, String trackingPixelUrl) {
        String eventTypeLabel = formatEventType(notification.eventType());
        String summary = extractSummary(notification);
        String actor = extractActor(notification);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        background-color: #4CAF50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .header h2 {
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        padding: 30px;
                        background-color: #ffffff;
                    }
                    .info-row {
                        margin-bottom: 15px;
                    }
                    .info-label {
                        font-weight: bold;
                        color: #333;
                    }
                    .info-value {
                        color: #666;
                        margin-left: 5px;
                    }
                    .details {
                        background-color: #f9f9f9;
                        padding: 15px;
                        border-left: 4px solid #4CAF50;
                        margin: 20px 0;
                    }
                    .button {
                        display: inline-block;
                        background-color: #4CAF50;
                        color: white;
                        padding: 12px 24px;
                        text-decoration: none;
                        border-radius: 4px;
                        margin-top: 20px;
                        font-weight: bold;
                    }
                    .button:hover {
                        background-color: #45a049;
                    }
                    .footer {
                        font-size: 12px;
                        color: #999;
                        padding: 20px;
                        text-align: center;
                        background-color: #f4f4f4;
                        border-top: 1px solid #ddd;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>%s</h2>
                    </div>
                    <div class="content">
                        <div class="info-row">
                            <span class="info-label">Actor:</span>
                            <span class="info-value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Summary:</span>
                            <span class="info-value">%s</span>
                        </div>
                        <div class="details">
                            <strong>Details:</strong><br>
                            %s
                        </div>
                        %s
                    </div>
                    <div class="footer">
                        <p>This is an automated notification from Feature Tracker.</p>
                        <p>If you have any questions, please contact your administrator.</p>
                    </div>
                </div>
                <!-- Tracking pixel -->
                <img src="%s" width="1" height="1" alt="" style="display:none;" />
            </body>
            </html>
            """
                .formatted(
                        escapeHtml(eventTypeLabel),
                        escapeHtml(actor),
                        escapeHtml(summary),
                        escapeHtml(
                                notification.eventDetails() != null
                                        ? notification.eventDetails()
                                        : "No additional details"),
                        notification.link() != null
                                ? "<a href=\"" + escapeHtml(notification.link())
                                        + "\" class=\"button\">View Details</a>"
                                : "",
                        trackingPixelUrl);
    }

    /**
     * Build plain text email content (fallback)
     */
    public static String buildEmailText(NotificationDto notification) {
        String eventTypeLabel = formatEventType(notification.eventType());
        String summary = extractSummary(notification);
        String actor = extractActor(notification);

        StringBuilder text = new StringBuilder();
        text.append(eventTypeLabel).append("\n\n");
        text.append("Actor: ").append(escapeHtml(actor)).append("\n");
        text.append("Summary: ").append(escapeHtml(summary)).append("\n\n");
        text.append("Details:\n")
                .append(escapeHtml(
                        notification.eventDetails() != null ? notification.eventDetails() : "No additional details"))
                .append("\n\n");

        if (notification.link() != null) {
            text.append("View details: ")
                    .append(escapeHtml(notification.link()))
                    .append("\n");
        }

        text.append("\n---\n");
        text.append("This is an automated notification from Feature Tracker.\n");

        return text.toString();
    }

    /**
     * Extract summary from notification
     */
    private static String extractSummary(NotificationDto notification) {
        String eventDetails = notification.eventDetails();
        if (eventDetails == null || eventDetails.isEmpty()) {
            return "A notification event has occurred";
        }

        // Try to extract a concise summary from event details
        // If event details are JSON, we might want to parse and extract specific fields
        // For now, truncate to first 100 characters
        if (eventDetails.length() > 100) {
            return eventDetails.substring(0, 97) + "...";
        }
        return eventDetails;
    }

    /**
     * Extract actor from notification details or use default
     */
    private static String extractActor(NotificationDto notification) {
        // Parse event details JSON to extract the actor
        String eventDetails = notification.eventDetails();
        if (eventDetails == null || eventDetails.isEmpty()) {
            return "System";
        }

        // Try to extract "createdBy", "updatedBy", "actor" or similar fields from JSON
        try {
            // Simple JSON parsing for common actor fields
            if (eventDetails.contains("\"createdBy\":")) {
                int start = eventDetails.indexOf("\"createdBy\":\"") + 13;
                int end = eventDetails.indexOf("\"", start);
                if (end > start) {
                    return eventDetails.substring(start, end);
                }
            }
            if (eventDetails.contains("\"updatedBy\":")) {
                int start = eventDetails.indexOf("\"updatedBy\":\"") + 13;
                int end = eventDetails.indexOf("\"", start);
                if (end > start) {
                    return eventDetails.substring(start, end);
                }
            }
            if (eventDetails.contains("\"actor\":")) {
                int start = eventDetails.indexOf("\"actor\":\"") + 9;
                int end = eventDetails.indexOf("\"", start);
                if (end > start) {
                    return eventDetails.substring(start, end);
                }
            }
        } catch (Exception e) {
            // If parsing fails, return System
        }

        return "System";
    }

    /**
     * Format event type for display
     */
    private static String formatEventType(NotificationEventType eventType) {
        if (eventType == null) {
            return "Notification";
        }

        return switch (eventType) {
            case FEATURE_CREATED -> "New Feature Created";
            case FEATURE_UPDATED -> "Feature Updated";
            case FEATURE_DELETED -> "Feature Deleted";
            case RELEASE_CREATED -> "New Release Created";
            case RELEASE_UPDATED -> "Release Updated";
            case RELEASE_DELETED -> "Release Deleted";
        };
    }

    /**
     * Build email subject line
     */
    public static String buildSubject(NotificationDto notification) {
        return "[Feature Tracker] " + formatEventType(notification.eventType());
    }
}
