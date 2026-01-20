package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private ReleaseMapper releaseMapper;

    private ReleaseService releaseService;
    private Product testProduct;
    private Release testRelease;

    @BeforeEach
    void setUp() {
        releaseService = new ReleaseService(releaseRepository, productRepository, featureRepository, releaseMapper);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setCode("TEST");
        testProduct.setPrefix("TST");
        testProduct.setName("Test Product");

        testRelease = new Release();
        testRelease.setId(1L);
        testRelease.setProduct(testProduct);
        testRelease.setCode("TST-v1.0");
        testRelease.setDescription("Test Release");
        testRelease.setStatus(ReleaseStatus.DRAFT);
    }

    @Test
    void shouldCreateReleaseWithPlanningFields() {
        // Given
        Instant plannedStart = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant plannedRelease = Instant.now().plus(7, ChronoUnit.DAYS);
        String owner = "john.doe";
        String notes = "Initial release notes";

        CreateReleaseCommand command = new CreateReleaseCommand(
                "TEST", "v1.0", "Test Release", plannedStart, plannedRelease, owner, notes, "creator");

        when(productRepository.findByCode("TEST")).thenReturn(Optional.of(testProduct));
        when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

        // When
        String result = releaseService.createRelease(command);

        // Then
        assertThat(result).isEqualTo("TST-v1.0");
        verify(releaseRepository).save(any(Release.class));
    }

    @Test
    void shouldValidatePlanningDatesOnCreate() {
        // Given - planned start date is after planned release date
        Instant plannedStart = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant plannedRelease = Instant.now().plus(1, ChronoUnit.DAYS);

        CreateReleaseCommand command = new CreateReleaseCommand(
                "TEST", "v1.0", "Test Release", plannedStart, plannedRelease, "owner", "notes", "creator");

        when(productRepository.findByCode("TEST")).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> releaseService.createRelease(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Planned start date cannot be after planned release date");
    }

    @Test
    void shouldUpdateReleaseWithNewPlanningFields() {
        // Given
        Instant plannedStart = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant plannedRelease = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant actualRelease = Instant.now().plus(8, ChronoUnit.DAYS);

        // Set status to PLANNED first so we can transition to IN_PROGRESS
        testRelease.setStatus(ReleaseStatus.PLANNED);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0",
                "Updated description",
                ReleaseStatus.IN_PROGRESS,
                null,
                plannedStart,
                plannedRelease,
                actualRelease,
                "new.owner",
                "Updated notes",
                "updater");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(testRelease));
        when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

        // When
        releaseService.updateRelease(command);

        // Then
        verify(releaseRepository).save(any(Release.class));
    }

    @Test
    void shouldValidateStatusTransitionOnUpdate() {
        // Given - invalid transition from DRAFT to COMPLETED
        testRelease.setStatus(ReleaseStatus.DRAFT);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0",
                "Updated description",
                ReleaseStatus.COMPLETED,
                null,
                null,
                null,
                null,
                null,
                null,
                "updater");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(testRelease));

        // When & Then
        assertThatThrownBy(() -> releaseService.updateRelease(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition from DRAFT to COMPLETED");
    }

    @Test
    void shouldValidatePlanningDatesOnUpdate() {
        // Given - actual release date before planned start date
        Instant plannedStart = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant actualRelease = Instant.now().plus(1, ChronoUnit.DAYS);

        testRelease.setPlannedStartDate(plannedStart);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0", "Updated description", null, null, null, null, actualRelease, null, null, "updater");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(testRelease));

        // When & Then
        assertThatThrownBy(() -> releaseService.updateRelease(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Actual release date cannot be before planned start date");
    }

    @Test
    void shouldAllowValidStatusTransition() {
        // Given - valid transition from DRAFT to PLANNED
        testRelease.setStatus(ReleaseStatus.DRAFT);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0",
                "Updated description",
                ReleaseStatus.PLANNED,
                null,
                null,
                null,
                null,
                null,
                null,
                "updater");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(testRelease));
        when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

        // When
        releaseService.updateRelease(command);

        // Then
        verify(releaseRepository).save(any(Release.class));
    }

    @Test
    void shouldAllowNullStatusInUpdate() {
        // Given - null status should not trigger validation
        testRelease.setStatus(ReleaseStatus.DRAFT);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0", "Updated description", null, null, null, null, null, "owner", "notes", "updater");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(testRelease));
        when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

        // When
        releaseService.updateRelease(command);

        // Then
        verify(releaseRepository).save(any(Release.class));
    }
}
