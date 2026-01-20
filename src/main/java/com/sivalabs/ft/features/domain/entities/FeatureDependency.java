package com.sivalabs.ft.features.domain.entities;

import com.sivalabs.ft.features.domain.models.DependencyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
        name = "feature_dependencies",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"feature_code", "depends_on_feature_code"})})
public class FeatureDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feature_dependencies_id_gen")
    @SequenceGenerator(name = "feature_dependencies_id_gen", sequenceName = "feature_dependency_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_code", referencedColumnName = "code", nullable = false)
    private Feature feature;

    @NotNull @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_feature_code", referencedColumnName = "code", nullable = false)
    private Feature dependsOnFeature;

    @NotNull @Column(name = "dependency_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DependencyType dependencyType;

    @NotNull @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public Feature getDependsOnFeature() {
        return dependsOnFeature;
    }

    public void setDependsOnFeature(Feature dependsOnFeature) {
        this.dependsOnFeature = dependsOnFeature;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
