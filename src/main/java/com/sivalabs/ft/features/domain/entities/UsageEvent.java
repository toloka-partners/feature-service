package com.sivalabs.ft.features.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "usage_events")
public class UsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usage_events_id_gen")
    @SequenceGenerator(name = "usage_events_id_gen", sequenceName = "usage_event_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50) @NotNull @Column(name = "feature_code", nullable = false, length = 50)
    private String featureCode;

    @Size(max = 50) @NotNull @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Size(max = 255) @NotNull @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z_]+$", message = "Event type must be uppercase with underscores") @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "metadata", length = Integer.MAX_VALUE)
    private String metadata;

    @NotNull @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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
}
