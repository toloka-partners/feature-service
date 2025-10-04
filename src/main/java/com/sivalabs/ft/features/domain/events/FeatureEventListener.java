package com.sivalabs.ft.features.domain.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.NotificationRecipientService;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.NotificationEventType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka event listener with deduplication support for feature events
 * Uses EventDeduplicationService for constraint-based deduplication
 */
@Component
public class FeatureEventListener {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEventListener.class);

    private final EventDeduplicationService eventDeduplicationService;
    private final NotificationService notificationService;
    private final NotificationRecipientService recipientService;
    private final FeatureService featureService;
    private final ObjectMapper objectMapper;

    public FeatureEventListener(
            EventDeduplicationService eventDeduplicationService,
            NotificationService notificationService,
            NotificationRecipientService recipientService,
            FeatureService featureService,
            ObjectMapper objectMapper) {
        this.eventDeduplicationService = eventDeduplicationService;
        this.notificationService = notificationService;
        this.recipientService = recipientService;
        this.featureService = featureService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ft.events.new-features}")
    @Transactional
    public void handleFeatureCreatedEvent(
            @Payload FeatureCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received FeatureCreatedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processFeatureCreatedEvent(event);
            logger.info(
                    "Successfully processed FeatureCreatedEvent for feature: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    @KafkaListener(topics = "${ft.events.updated-features}")
    @Transactional
    public void handleFeatureUpdatedEvent(
            @Payload FeatureUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received FeatureUpdatedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processFeatureUpdatedEvent(event);
            logger.info(
                    "Successfully processed FeatureUpdatedEvent for feature: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    @KafkaListener(topics = "${ft.events.deleted-features}")
    @Transactional
    public void handleFeatureDeletedEvent(
            @Payload FeatureDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info(
                "Received FeatureDeletedEvent from topic: {}, partition: {}, offset: {}, eventId: {}",
                topic,
                partition,
                offset,
                event.eventId());

        eventDeduplicationService.executeIdempotent(event.eventId(), EventType.EVENT, () -> {
            processFeatureDeletedEvent(event);
            logger.info(
                    "Successfully processed FeatureDeletedEvent for feature: {} with eventId: {}",
                    event.code(),
                    event.eventId());
            return "processed";
        });
    }

    /**
     * Process feature created event - implement your business logic here
     */
    private void processFeatureCreatedEvent(FeatureCreatedEvent event) {
        // Get feature from database via service FIRST (before logging business logic)
        FeatureDto featureDto = featureService
                .findFeatureByCode(null, event.code())
                .orElseThrow(() -> new RuntimeException("Feature not found: " + event.code()));

        logger.info(
                "EventListener business logic: Processing feature created: {} - {} (eventId: {})",
                event.code(),
                event.title(),
                event.eventId());

        // Determine notification recipients using NotificationRecipientService
        Set<String> recipients = recipientService.getFeatureNotificationRecipients(featureDto);

        // Create notifications for each recipient
        // Use unique eventId per recipient to avoid deduplication issues
        int recipientIndex = 0;
        for (String recipientUserId : recipients) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "created");
                eventDetails.put("featureCode", event.code());
                eventDetails.put("title", event.title());

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/features/" + event.code();

                // Create unique eventId for each recipient to avoid deduplication
                String notificationEventId = event.eventId() + "-recipient-" + recipientIndex++;

                notificationService.createNotification(
                        recipientUserId,
                        notificationEventId,
                        NotificationEventType.FEATURE_CREATED,
                        eventDetailsJson,
                        link);

                logger.debug("Created notification for user {} about feature {}", recipientUserId, event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for feature {}", event.code(), e);
            }
        }

        logger.debug("Feature created event processed successfully for: {}", event.code());
    }

    /**
     * Process feature updated event - implement your business logic here
     */
    private void processFeatureUpdatedEvent(FeatureUpdatedEvent event) {
        // Get feature from database via service FIRST (before logging business logic)
        FeatureDto featureDto = featureService
                .findFeatureByCode(null, event.code())
                .orElseThrow(() -> new RuntimeException("Feature not found: " + event.code()));

        logger.info(
                "EventListener business logic: Processing feature updated: {} - {} (eventId: {})",
                event.code(),
                event.title(),
                event.eventId());

        // Determine notification recipients using NotificationRecipientService
        Set<String> recipients = recipientService.getFeatureNotificationRecipients(featureDto);

        // Create notifications for each recipient
        // Use unique eventId per recipient to avoid deduplication issues
        int recipientIndex = 0;
        for (String recipientUserId : recipients) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "updated");
                eventDetails.put("featureCode", event.code());
                eventDetails.put("title", event.title());
                if (event.status() != null) {
                    eventDetails.put("status", event.status().name());
                }

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/features/" + event.code();

                // Create unique eventId for each recipient to avoid deduplication
                String notificationEventId = event.eventId() + "-recipient-" + recipientIndex++;

                notificationService.createNotification(
                        recipientUserId,
                        notificationEventId,
                        NotificationEventType.FEATURE_UPDATED,
                        eventDetailsJson,
                        link);

                logger.debug("Created notification for user {} about feature update {}", recipientUserId, event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for feature {}", event.code(), e);
            }
        }

        logger.debug("Feature updated event processed successfully for: {}", event.code());
    }

    /**
     * Process feature deleted event - implement your business logic here
     */
    private void processFeatureDeletedEvent(FeatureDeletedEvent event) {
        logger.info(
                "EventListener business logic: Processing feature deleted: {} - {} (eventId: {})",
                event.code(),
                event.title(),
                event.eventId());

        // For deleted features, we can't get from DB, so use event data
        // Send notification to the user who deleted it (from event)
        if (event.deletedBy() != null) {
            try {
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("action", "deleted");
                eventDetails.put("featureCode", event.code());
                eventDetails.put("title", event.title());

                String eventDetailsJson = objectMapper.writeValueAsString(eventDetails);
                String link = "/features/" + event.code();

                notificationService.createNotification(
                        event.deletedBy(),
                        event.eventId(),
                        NotificationEventType.FEATURE_DELETED,
                        eventDetailsJson,
                        link);

                logger.debug(
                        "Created notification for user {} about feature deletion {}", event.deletedBy(), event.code());
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event details for feature {}", event.code(), e);
            }
        }

        logger.debug("Feature deleted event processed successfully for: {}", event.code());
    }
}
