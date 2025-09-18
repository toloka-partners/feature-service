package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.UniversalIdempotencyService;
import com.sivalabs.ft.features.domain.models.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventDeduplicationService {
    private final UniversalIdempotencyService universalIdempotencyService;

    public EventDeduplicationService(UniversalIdempotencyService universalIdempotencyService) {
        this.universalIdempotencyService = universalIdempotencyService;
    }

    @Transactional
    public boolean isEventProcessed(String eventId) {
        // Check for EVENT type in universal service
        return universalIdempotencyService.isProcessed(eventId, EventType.EVENT);
    }

    @Transactional
    public void markEventAsProcessed(String eventId, String eventType, String aggregateId) {
        // Store with EVENT type and include event type and aggregate ID as result data
        String resultData = String.format("eventType=%s,aggregateId=%s", eventType, aggregateId);
        universalIdempotencyService.markAsProcessed(eventId, EventType.EVENT, resultData);
    }
}
