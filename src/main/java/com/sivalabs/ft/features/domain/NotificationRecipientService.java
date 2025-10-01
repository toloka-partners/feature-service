package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to determine notification recipients based on business rules
 * Simple approach: automatic notifications for createdBy and assignedTo users
 */
@Service
public class NotificationRecipientService {
    private static final Logger log = LoggerFactory.getLogger(NotificationRecipientService.class);

    /**
     * Get notification recipients for feature events
     * Returns createdBy and assignedTo users (if they exist and are different)
     */
    public Set<String> getFeatureNotificationRecipients(Feature feature) {
        Set<String> recipients = new HashSet<>();

        // Add feature creator
        if (feature.getCreatedBy() != null) {
            recipients.add(feature.getCreatedBy());
        }

        // Add assigned user (if different from creator)
        if (feature.getAssignedTo() != null && !feature.getAssignedTo().equals(feature.getCreatedBy())) {
            recipients.add(feature.getAssignedTo());
        }

        log.debug("Feature {} notification recipients: {}", feature.getCode(), recipients);
        return recipients;
    }

    /**
     * Get notification recipients for feature events from DTO
     * Returns createdBy and assignedTo users (if they exist and are different)
     */
    public Set<String> getFeatureNotificationRecipients(FeatureDto featureDto) {
        Set<String> recipients = new HashSet<>();

        // Add feature creator
        if (featureDto.createdBy() != null) {
            recipients.add(featureDto.createdBy());
        }

        // Add assigned user (if different from creator)
        if (featureDto.assignedTo() != null && !featureDto.assignedTo().equals(featureDto.createdBy())) {
            recipients.add(featureDto.assignedTo());
        }

        log.debug("Feature {} notification recipients: {}", featureDto.code(), recipients);
        return recipients;
    }

    /**
     * Get notification recipients for release events
     * Returns createdBy user
     */
    public Set<String> getReleaseNotificationRecipients(Release release) {
        Set<String> recipients = new HashSet<>();

        // Add release creator
        if (release.getCreatedBy() != null) {
            recipients.add(release.getCreatedBy());
        }

        log.debug("Release {} notification recipients: {}", release.getCode(), recipients);
        return recipients;
    }

    /**
     * Get notification recipients for feature events with additional context
     * Can include users who might be interested (e.g., previous assignee)
     */
    public Set<String> getFeatureNotificationRecipientsWithContext(Feature feature, String previousAssignedTo) {
        Set<String> recipients = getFeatureNotificationRecipients(feature);

        // Add previous assignee if feature was reassigned
        if (previousAssignedTo != null
                && !previousAssignedTo.equals(feature.getAssignedTo())
                && !previousAssignedTo.equals(feature.getCreatedBy())) {
            recipients.add(previousAssignedTo);
        }

        log.debug("Feature {} notification recipients with context: {}", feature.getCode(), recipients);
        return recipients;
    }
}
