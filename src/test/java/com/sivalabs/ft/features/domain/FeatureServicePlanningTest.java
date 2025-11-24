package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.Commands.AssignFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeaturePlanningCommand;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureServicePlanningTest {

    @Mock
    private FavoriteFeatureService favoriteFeatureService;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FavoriteFeatureRepository favoriteFeatureRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FeatureMapper featureMapper;

    private FeatureService featureService;

    @BeforeEach
    void setUp() {
        featureService = new FeatureService(
                favoriteFeatureService,
                releaseRepository,
                featureRepository,
                productRepository,
                favoriteFeatureRepository,
                eventPublisher,
                featureMapper);
    }

    @Test
    void shouldAssignFeatureToRelease() {
        // Given
        var feature = new Feature();
        feature.setCode("FEAT-1");
        var release = new Release();
        release.setCode("REL-1");

        var cmd = new AssignFeatureCommand(
                "REL-1", "FEAT-1", Instant.now().plusSeconds(86400), "john.doe", "Initial assignment", "admin");

        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));
        when(releaseRepository.findByCode("REL-1")).thenReturn(Optional.of(release));

        // When
        featureService.assignFeatureToRelease(cmd);

        // Then
        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository).save(captor.capture());

        Feature savedFeature = captor.getValue();
        assertThat(savedFeature.getRelease()).isEqualTo(release);
        assertThat(savedFeature.getPlanningStatus()).isEqualTo(FeaturePlanningStatus.NOT_STARTED);
        assertThat(savedFeature.getFeatureOwner()).isEqualTo("john.doe");
        assertThat(savedFeature.getPlanningNotes()).isEqualTo("Initial assignment");
    }

    @Test
    void shouldThrowExceptionWhenAssigningNonExistentFeature() {
        // Given
        var cmd = new AssignFeatureCommand("REL-1", "FEAT-999", Instant.now(), "owner", "notes", "admin");
        when(featureRepository.findByCode("FEAT-999")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> featureService.assignFeatureToRelease(cmd))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feature not found: FEAT-999");
    }

    @Test
    void shouldThrowExceptionWhenAssigningToNonExistentRelease() {
        // Given
        var feature = new Feature();
        var cmd = new AssignFeatureCommand("REL-999", "FEAT-1", Instant.now(), "owner", "notes", "admin");
        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));
        when(releaseRepository.findByCode("REL-999")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> featureService.assignFeatureToRelease(cmd))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Release not found: REL-999");
    }

    @Test
    void shouldUpdateFeaturePlanning() {
        // Given
        var feature = new Feature();
        feature.setCode("FEAT-1");
        feature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);

        var cmd = new UpdateFeaturePlanningCommand(
                "FEAT-1",
                Instant.now().plusSeconds(172800),
                FeaturePlanningStatus.IN_PROGRESS,
                "jane.smith",
                null,
                "Updated planning",
                "admin");

        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));

        // When
        featureService.updateFeaturePlanning(cmd);

        // Then
        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository).save(captor.capture());

        Feature savedFeature = captor.getValue();
        assertThat(savedFeature.getPlanningStatus()).isEqualTo(FeaturePlanningStatus.IN_PROGRESS);
        assertThat(savedFeature.getFeatureOwner()).isEqualTo("jane.smith");
        assertThat(savedFeature.getPlanningNotes()).isEqualTo("Updated planning");
    }

    @Test
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        var feature = new Feature();
        feature.setCode("FEAT-1");
        feature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);

        var cmd = new UpdateFeaturePlanningCommand(
                "FEAT-1", null, FeaturePlanningStatus.DONE, null, null, null, "admin");

        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));

        // When/Then
        assertThatThrownBy(() -> featureService.updateFeaturePlanning(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid planning status transition");
    }

    @Test
    void shouldMoveFeatureBetweenReleases() {
        // Given
        var feature = new Feature();
        feature.setCode("FEAT-1");
        var oldRelease = new Release();
        oldRelease.setCode("REL-1");
        feature.setRelease(oldRelease);
        feature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);

        var targetRelease = new Release();
        targetRelease.setCode("REL-2");

        var cmd = new MoveFeatureCommand("FEAT-1", "REL-2", "Changed priorities", "admin");

        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));
        when(releaseRepository.findByCode("REL-2")).thenReturn(Optional.of(targetRelease));

        // When
        featureService.moveFeatureBetweenReleases(cmd);

        // Then
        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository).save(captor.capture());

        Feature savedFeature = captor.getValue();
        assertThat(savedFeature.getRelease()).isEqualTo(targetRelease);
        assertThat(savedFeature.getPlanningStatus()).isEqualTo(FeaturePlanningStatus.NOT_STARTED);
    }

    @Test
    void shouldRemoveFeatureFromRelease() {
        // Given
        var feature = new Feature();
        feature.setCode("FEAT-1");
        var release = new Release();
        release.setCode("REL-1");
        feature.setRelease(release);
        feature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        feature.setFeatureOwner("john.doe");
        feature.setPlanningNotes("Some notes");

        var cmd = new RemoveFeatureCommand("FEAT-1", "No longer needed in this release", "admin");

        when(featureRepository.findByCode("FEAT-1")).thenReturn(Optional.of(feature));

        // When
        featureService.removeFeatureFromRelease(cmd);

        // Then
        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository).save(captor.capture());

        Feature savedFeature = captor.getValue();
        assertThat(savedFeature.getRelease()).isNull();
        assertThat(savedFeature.getPlanningStatus()).isNull();
        assertThat(savedFeature.getFeatureOwner()).isNull();
        assertThat(savedFeature.getPlanningNotes()).isNull();
    }
}
