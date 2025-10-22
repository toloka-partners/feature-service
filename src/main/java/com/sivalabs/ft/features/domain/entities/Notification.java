package com.sivalabs.ft.features.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_id_gen")
    @SequenceGenerator(name = "notifications_id_gen", sequenceName = "notification_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255) @NotNull @Column(name = "event_id", nullable = false)
    private String eventId;

    @Size(max = 50) @NotNull @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Size(max = 255) @NotNull @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", length = Integer.MAX_VALUE)
    private String message;

    @Size(max = 500) @Column(name = "link", length = 500)
    private String link;

    @NotNull @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}