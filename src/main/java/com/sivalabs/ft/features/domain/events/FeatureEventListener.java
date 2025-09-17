package com.sivalabs.ft.features.domain.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FeatureEventListener {
    private static final Logger log = LoggerFactory.getLogger(FeatureEventListener.class);
    private final EventDeduplicationService deduplicationService;

    public FeatureEventListener(EventDeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @KafkaListener(topics = "${app.events.new-features}")
    public void handleFeatureCreatedEvent(FeatureCreatedEvent event) {
        String eventId = event.eventIdentifier().eventId();

        if (deduplicationService.isEventProcessed(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        log.info("Processing feature created event for feature: {}", event.code());

        deduplicationService.markEventAsProcessed(
                eventId,
                event.eventIdentifier().eventType(),
                event.eventIdentifier().aggregateId());
    }

    @KafkaListener(topics = "${app.events.updated-features}")
    public void handleFeatureUpdatedEvent(FeatureUpdatedEvent event) {
        String eventId = event.eventIdentifier().eventId();

        if (deduplicationService.isEventProcessed(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        log.info("Processing feature updated event for feature: {}", event.code());

        deduplicationService.markEventAsProcessed(
                eventId,
                event.eventIdentifier().eventType(),
                event.eventIdentifier().aggregateId());
    }

    @KafkaListener(topics = "${app.events.deleted-features}")
    public void handleFeatureDeletedEvent(FeatureDeletedEvent event) {
        String eventId = event.eventIdentifier().eventId();

        if (deduplicationService.isEventProcessed(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        log.info("Processing feature deleted event for feature: {}", event.code());

        deduplicationService.markEventAsProcessed(
                eventId,
                event.eventIdentifier().eventType(),
                event.eventIdentifier().aggregateId());
    }
}
