package com.sivalabs.ft.features.domain.events;

import java.time.Instant;
import java.util.UUID;

public record EventIdentifier(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        Instant timestamp,
        String correlationId) {

    public static EventIdentifier create(String eventType, String aggregateId, String aggregateType) {
        return new EventIdentifier(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                aggregateType,
                Instant.now(),
                UUID.randomUUID().toString());
    }
}
