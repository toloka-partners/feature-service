package com.sivalabs.ft.features.domain.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.NotificationRecipientService;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.ReleaseService;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.NotificationEventType;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka event listener with deduplication support for release events
 * Uses EventDeduplicationService for constraint-based deduplication
 * Uses @KafkaListener at class level with @KafkaHandler methods for type-based routing
 * Uses separate groupId to avoid rebalance issues with feature event consumers
 */
@Component
@KafkaListener(topics = "${ft.events.releases}", groupId = "release-service-group")
public class ReleaseEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseEventListener.class);

    private final EventDeduplicationService eventDeduplicationService;
    private final NotificationService notificationService;
    private final NotificationRecipientService recipientService;
    private final ReleaseService releaseService;
    private final FeatureService featureService;
    private final ObjectMapper objectMapper;

    public ReleaseEventListener(
            EventDeduplicationService eventDeduplicationService,
            NotificationService notificationService,
            NotificationRecipientService recipientService,
            ReleaseService releaseService,
            FeatureService featureService,
            ObjectMapper objectMapper) {
        this.eventDeduplicationService = eventDeduplicationService;
        this.notificationService = notificationService;
        this.recipientService = recipientService;
        this.releaseService = releaseService;
        this.featureService = featureService;
        this.objectMapper = objectMapper;
    }

    @KafkaHandler
    @Transactional
    public void handleReleaseCreatedEvent(
            @Payload ReleaseCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received ReleaseCreatedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processReleaseCreatedEvent(event);
            logger.info(
                    "Successfully processed ReleaseCreatedEvent for release: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    @KafkaHandler
    @Transactional
    public void handleReleaseUpdatedEvent(
            @Payload ReleaseUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received ReleaseUpdatedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processReleaseUpdatedEvent(event);
            logger.info(
                    "Successfully processed ReleaseUpdatedEvent for release: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    @KafkaHandler
    @Transactional
    public void handleReleaseDeletedEvent(
            @Payload ReleaseDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received ReleaseDeletedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processReleaseDeletedEvent(event);
            logger.info(
                    "Successfully processed ReleaseDeletedEvent for release: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    /**
     * Process release created event - implement your business logic here
     */
    private void processReleaseCreatedEvent(ReleaseCreatedEvent event) {
        logger.info(
                "EventListener business logic: Processing release created: {} (eventId: {})",
                event.code(),
                event.eventId());

        // Get release from database via service
        ReleaseDto releaseDto = releaseService
                .findReleaseByCode(event.code())
                .orElseThrow(() -> new RuntimeException("Release not found: " + event.code()));

        // Determine notification recipients - for release created, notify the creator
        Set<String> recipients = new HashSet<>();
        if (releaseDto.createdBy() != null) {
            recipients.add(releaseDto.createdBy());
        }

        // Create notifications for each recipient
        int recipientIndex = 0;
        for (String recipientUserId : recipients) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "created");
                eventDetails.put("releaseCode", event.code());
                eventDetails.put("description", event.description());

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/releases/" + event.code();

                // Create unique eventId for each recipient to avoid deduplication
                String notificationEventId = event.eventId() + "-recipient-" + recipientIndex++;

                notificationService.createNotification(
                        recipientUserId,
                        notificationEventId,
                        NotificationEventType.RELEASE_CREATED,
                        eventDetailsJson,
                        link);

                logger.debug("Created notification for user {} about release {}", recipientUserId, event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for release {}", event.code(), e);
            }
        }

        logger.debug("Release created event processed successfully for: {}", event.code());
    }

    /**
     * Process release updated event - implement your business logic here
     * Includes cascade notifications when release status changes to RELEASED
     */
    private void processReleaseUpdatedEvent(ReleaseUpdatedEvent event) {
        logger.info(
                "EventListener business logic: Processing release updated: {} (eventId: {})",
                event.code(),
                event.eventId());

        // Get release from database via service
        ReleaseDto releaseDto = releaseService
                .findReleaseByCode(event.code())
                .orElseThrow(() -> new RuntimeException("Release not found: " + event.code()));

        // Determine notification recipients - for release updated, notify the creator
        Set<String> recipients = new HashSet<>();
        if (releaseDto.createdBy() != null) {
            recipients.add(releaseDto.createdBy());
        }

        // Create notifications for each recipient
        int recipientIndex = 0;
        for (String recipientUserId : recipients) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "updated");
                eventDetails.put("releaseCode", event.code());
                eventDetails.put("description", event.description());
                if (event.status() != null) {
                    eventDetails.put("status", event.status().name());
                }

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/releases/" + event.code();

                // Create unique eventId for each recipient to avoid deduplication
                String notificationEventId = event.eventId() + "-recipient-" + recipientIndex++;

                notificationService.createNotification(
                        recipientUserId,
                        notificationEventId,
                        NotificationEventType.RELEASE_UPDATED,
                        eventDetailsJson,
                        link);

                logger.debug("Created notification for user {} about release update {}", recipientUserId, event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for release {}", event.code(), e);
            }
        }

        // Handle cascade notifications when release status changes to RELEASED
        if (event.status() == ReleaseStatus.RELEASED && event.previousStatus() != ReleaseStatus.RELEASED) {
            logger.info("Release {} status changed to RELEASED, sending cascade notifications", event.code());
            processCascadeNotifications(event);
        }

        logger.debug("Release updated event processed successfully for: {}", event.code());
    }

    /**
     * Process release deleted event - implement your business logic here
     */
    private void processReleaseDeletedEvent(ReleaseDeletedEvent event) {
        logger.info(
                "EventListener business logic: Processing release deleted: {} (eventId: {})",
                event.code(),
                event.eventId());

        // For deleted releases, we can't get from DB, so use event data
        // Send notification to the user who deleted it (from event)
        if (event.deletedBy() != null) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "deleted");
                eventDetails.put("releaseCode", event.code());
                eventDetails.put("description", event.description());

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/releases/" + event.code();

                notificationService.createNotification(
                        event.deletedBy(),
                        event.eventId(),
                        NotificationEventType.RELEASE_DELETED,
                        eventDetailsJson,
                        link);

                logger.debug(
                        "Created notification for user {} about release deletion {}", event.deletedBy(), event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for release {}", event.code(), e);
            }
        }

        logger.debug("Release deleted event processed successfully for: {}", event.code());
    }

    /**
     * Process cascade notifications when release status changes to RELEASED
     * Notifies all users with features in that release
     */
    private void processCascadeNotifications(ReleaseUpdatedEvent event) {
        logger.info("Processing cascade notifications for release: {}", event.code());

        // Get all features in this release
        List<FeatureDto> features = featureService.findFeaturesByRelease(null, event.code());

        if (features.isEmpty()) {
            logger.debug("No features found in release {}, skipping cascade notifications", event.code());
            return;
        }

        // Collect all unique users who have features in this release
        Set<String> affectedUsers = new HashSet<>();
        for (FeatureDto feature : features) {
            if (feature.createdBy() != null) {
                affectedUsers.add(feature.createdBy());
            }
            if (feature.assignedTo() != null) {
                affectedUsers.add(feature.assignedTo());
            }
        }

        logger.info(
                "Found {} features and {} affected users for release {}",
                features.size(),
                affectedUsers.size(),
                event.code());

        // Send notifications to all affected users
        int recipientIndex = 0;
        for (String userId : affectedUsers) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "released");
                eventDetails.put("releaseCode", event.code());
                eventDetails.put("description", event.description());
                eventDetails.put("featureCount", features.size());

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/releases/" + event.code();

                // Create unique eventId for each cascade notification recipient
                String notificationEventId = event.eventId() + "-cascade-" + recipientIndex++;

                notificationService.createNotification(
                        userId, notificationEventId, NotificationEventType.RELEASE_UPDATED, eventDetailsJson, link);

                logger.debug(
                        "Created cascade notification for user {} about release {} being RELEASED",
                        userId,
                        event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize cascade event details for release {}", event.code(), e);
            }
        }

        logger.info("Successfully sent {} cascade notifications for release {}", affectedUsers.size(), event.code());
    }
}
