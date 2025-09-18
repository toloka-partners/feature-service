package com.sivalabs.ft.features.domain.events;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventId.class)
public class ProcessedEvent {
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Id
    @Column(name = "event_type")
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public ProcessedEvent(String eventId, String eventType, String resultData) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
        this.resultData = resultData;
    }

    public ProcessedEvent(String eventId, String eventType, String resultData, Instant expiresAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
        this.resultData = resultData;
        this.expiresAt = expiresAt;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }
}
