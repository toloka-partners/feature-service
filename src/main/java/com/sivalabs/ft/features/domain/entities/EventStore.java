package com.sivalabs.ft.features.domain.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "event_store")
public class EventStore {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_store_id_gen")
    @SequenceGenerator(name = "event_store_id_gen", sequenceName = "event_store_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "version", nullable = false)
    private Long version;

    public EventStore() {}

    public EventStore(
            String eventId,
            String eventType,
            String code,
            String aggregateType,
            String eventData,
            String metadata,
            Instant createdAt,
            Long version) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.code = code;
        this.aggregateType = aggregateType;
        this.eventData = eventData;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
