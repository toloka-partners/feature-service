package com.sivalabs.ft.features.domain.events;

import java.io.Serializable;
import java.util.Objects;

public class ProcessedEventId implements Serializable {
    private String eventId;
    private String eventType;

    public ProcessedEventId() {}

    public ProcessedEventId(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedEventId that = (ProcessedEventId) o;
        return Objects.equals(eventId, that.eventId) && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType);
    }
}
