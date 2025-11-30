package com.sivalabs.ft.features.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.Commands.AssignFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeaturePlanningCommand;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureServicePlanningTest {

    @Mock
    private FavoriteFeatureService favoriteFeatureService;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FavoriteFeatureRepository favoriteFeatureRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FeatureMapper featureMapper;

    @InjectMocks
    private FeatureService featureService;

    private Feature mockFeature;
    private Release mockRelease;

    @BeforeEach
    void setUp() {
        mockFeature = new Feature();
        mockFeature.setCode("FEATURE-1");
        mockFeature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);

        mockRelease = new Release();
        mockRelease.setCode("REL-1.0");
    }

    @Test
    void shouldAssignFeatureToReleaseSuccessfully() {
        var cmd = new AssignFeatureCommand(
                "REL-1.0", "FEATURE-1", Instant.now().plusSeconds(30 * 24 * 60 * 60), "owner", "notes", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));
        when(releaseRepository.findByCode("REL-1.0")).thenReturn(Optional.of(mockRelease));

        featureService.assignFeatureToRelease(cmd);

        assertEquals(mockRelease, mockFeature.getRelease());
        assertEquals(FeaturePlanningStatus.NOT_STARTED, mockFeature.getPlanningStatus());
        assertEquals("owner", mockFeature.getFeatureOwner());
        assertEquals("notes", mockFeature.getPlanningNotes());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldThrowExceptionWhenFeatureAlreadyAssigned() {
        mockFeature.setRelease(mockRelease); // Feature already assigned to same release
        var cmd = new AssignFeatureCommand(
                "REL-1.0", "FEATURE-1", Instant.now().plusSeconds(30 * 24 * 60 * 60), "owner", "notes", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));
        when(releaseRepository.findByCode("REL-1.0")).thenReturn(Optional.of(mockRelease));

        assertThrows(BadRequestException.class, () -> featureService.assignFeatureToRelease(cmd));
    }

    @Test
    void shouldUpdateFeaturePlanningSuccessfully() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1",
                Instant.now().plusSeconds(30 * 24 * 60 * 60),
                FeaturePlanningStatus.IN_PROGRESS,
                "newowner",
                null,
                "updated notes",
                "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        assertEquals("newowner", mockFeature.getFeatureOwner());
        assertEquals("updated notes", mockFeature.getPlanningNotes());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldThrowExceptionWhenFeatureNotFound() {
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1",
                Instant.now().plusSeconds(30 * 24 * 60 * 60),
                FeaturePlanningStatus.IN_PROGRESS,
                "owner",
                "notes",
                null,
                "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.updateFeaturePlanning(cmd));
    }

    @Test
    void shouldValidateStatusTransitionFromNotStartedToInProgress() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.IN_PROGRESS, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromDone() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.DONE);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.IN_PROGRESS, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        assertThrows(BadRequestException.class, () -> featureService.updateFeaturePlanning(cmd));
    }

    @Test
    void shouldAllowTransitionFromBlockedToInProgress() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.BLOCKED);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.IN_PROGRESS, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldAllowTransitionFromInProgressToDone() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.DONE, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.DONE, mockFeature.getPlanningStatus());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldAllowTransitionFromInProgressToBlocked() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.BLOCKED, null, "Waiting for dependencies", null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.BLOCKED, mockFeature.getPlanningStatus());
        assertEquals("Waiting for dependencies", mockFeature.getBlockageReason());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromNotStartedToDone() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.DONE, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        assertThrows(BadRequestException.class, () -> featureService.updateFeaturePlanning(cmd));
    }

    @Test
    void shouldAllowSameStatusTransition() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1",
                Instant.now().plusSeconds(15 * 24 * 60 * 60),
                FeaturePlanningStatus.IN_PROGRESS, // Same status
                "owner",
                null,
                "updated notes",
                "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        assertEquals("updated notes", mockFeature.getPlanningNotes());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldMoveFeatureBetweenReleases() {
        mockFeature.setRelease(mockRelease);
        var targetRelease = new Release();
        targetRelease.setCode("REL-2.0");
        var cmd = new MoveFeatureCommand("FEATURE-1", "REL-2.0", "Moving to new release", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));
        when(releaseRepository.findByCode("REL-2.0")).thenReturn(Optional.of(targetRelease));

        featureService.moveFeatureBetweenReleases(cmd);

        assertEquals(targetRelease, mockFeature.getRelease());
        assertEquals(FeaturePlanningStatus.NOT_STARTED, mockFeature.getPlanningStatus());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldRemoveFeatureFromRelease() {
        mockFeature.setRelease(mockRelease);
        mockFeature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        var cmd = new RemoveFeatureCommand("FEATURE-1", "Removing from release", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.removeFeatureFromRelease(cmd);

        assertNull(mockFeature.getRelease());
        assertNull(mockFeature.getPlanningStatus());
        assertNull(mockFeature.getFeatureOwner());
        assertNull(mockFeature.getPlanningNotes());
        assertNull(mockFeature.getBlockageReason());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldThrowExceptionWhenReleaseNotFoundForAssignment() {
        var cmd = new AssignFeatureCommand(
                "REL-1.0", "FEATURE-1", Instant.now().plusSeconds(30 * 24 * 60 * 60), "owner", "notes", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));
        when(releaseRepository.findByCode("REL-1.0")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.assignFeatureToRelease(cmd));
    }

    @Test
    void shouldThrowExceptionWhenFeatureNotFoundForAssignment() {
        var cmd = new AssignFeatureCommand(
                "REL-1.0", "FEATURE-1", Instant.now().plusSeconds(30 * 24 * 60 * 60), "owner", "notes", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.assignFeatureToRelease(cmd));
    }

    @Test
    void shouldThrowExceptionWhenReleaseNotFoundForMove() {
        mockFeature.setRelease(mockRelease);
        var cmd = new MoveFeatureCommand("FEATURE-1", "REL-2.0", "Moving to new release", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));
        when(releaseRepository.findByCode("REL-2.0")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.moveFeatureBetweenReleases(cmd));
    }

    @Test
    void shouldThrowExceptionWhenFeatureNotFoundForMove() {
        var cmd = new MoveFeatureCommand("FEATURE-1", "REL-2.0", "Moving to new release", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.moveFeatureBetweenReleases(cmd));
    }

    @Test
    void shouldThrowExceptionWhenFeatureNotFoundForRemoval() {
        var cmd = new RemoveFeatureCommand("FEATURE-1", "Removing from release", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.removeFeatureFromRelease(cmd));
    }

    @Test
    void shouldValidateStatusTransitionFromNotStartedToBlocked() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.BLOCKED, null, "Dependencies not ready", null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.BLOCKED, mockFeature.getPlanningStatus());
        assertEquals("Dependencies not ready", mockFeature.getBlockageReason());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldClearBlockageReasonWhenMovingFromBlockedToOtherStatus() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.BLOCKED);
        mockFeature.setBlockageReason("Previous blockage");
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.IN_PROGRESS, null, null, null, "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        assertNull(mockFeature.getBlockageReason());
        verify(featureRepository).save(mockFeature);
    }

    @Test
    void shouldPreserveExistingValuesWhenPartialUpdate() {
        mockFeature.setPlanningStatus(FeaturePlanningStatus.IN_PROGRESS);
        mockFeature.setFeatureOwner("existingowner");
        mockFeature.setPlanningNotes("existing notes");
        var cmd = new UpdateFeaturePlanningCommand(
                "FEATURE-1", null, FeaturePlanningStatus.IN_PROGRESS, null, null, "updated notes only", "testuser");

        when(featureRepository.findByCode("FEATURE-1")).thenReturn(Optional.of(mockFeature));

        featureService.updateFeaturePlanning(cmd);

        assertEquals(FeaturePlanningStatus.IN_PROGRESS, mockFeature.getPlanningStatus());
        assertEquals("existingowner", mockFeature.getFeatureOwner()); // Should be preserved
        assertEquals("updated notes only", mockFeature.getPlanningNotes()); // Should be updated
        verify(featureRepository).save(mockFeature);
    }
}
