package com.sivalabs.ft.features.domain.events;

import java.time.Instant;

/**
 * Base interface for all domain events
 */
public interface DomainEvent {
    /**
     * Unique identifier for the event
     */
    String getEventId();

    /**
     * Type of the event (class name)
     */
    String getEventType();

    /**
     * ID of the aggregate that generated this event
     */
    String getCode();

    /**
     * Type of the aggregate (e.g., "Feature", "Product", "Release")
     */
    String getAggregateType();

    /**
     * Timestamp when the event occurred
     */
    Instant getTimestamp();

    /**
     * Version of the aggregate when this event was generated
     */
    Long getVersion();
}
