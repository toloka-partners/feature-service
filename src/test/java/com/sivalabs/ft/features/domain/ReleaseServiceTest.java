package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    @BeforeEach
    void setUp() {
        releaseService = new ReleaseService(releaseRepository, productRepository, featureRepository, releaseMapper);
    }

    @Test
    void createRelease_shouldCreateReleaseWithDraftStatus() {
        // Given
        Product product = new Product();
        product.setCode("TEST");
        product.setPrefix("TST");

        CreateReleaseCommand command =
                new CreateReleaseCommand("TEST", "v1.0", "Test release", Instant.now(), "testuser");

        when(productRepository.findByCode("TEST")).thenReturn(Optional.of(product));

        // When
        String result = releaseService.createRelease(command);

        // Then
        assertThat(result).isEqualTo("TST-v1.0");
        verify(releaseRepository).save(any(Release.class));
    }

    @Test
    void updateRelease_validStatusTransition_shouldSucceed() {
        // Given
        Release existingRelease = new Release();
        existingRelease.setCode("TST-v1.0");
        existingRelease.setStatus(ReleaseStatus.DRAFT);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0", "Updated description", ReleaseStatus.PLANNED, Instant.now(), null, "testuser");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(existingRelease));

        // When
        releaseService.updateRelease(command);

        // Then
        verify(releaseRepository).save(any(Release.class));
    }

    @Test
    void updateRelease_invalidStatusTransition_shouldThrowException() {
        // Given
        Release existingRelease = new Release();
        existingRelease.setCode("TST-v1.0");
        existingRelease.setStatus(ReleaseStatus.RELEASED);

        UpdateReleaseCommand command = new UpdateReleaseCommand(
                "TST-v1.0", "Updated description", ReleaseStatus.DRAFT, Instant.now(), null, "testuser");

        when(releaseRepository.findByCode("TST-v1.0")).thenReturn(Optional.of(existingRelease));

        // When & Then
        assertThatThrownBy(() -> releaseService.updateRelease(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition from RELEASED to DRAFT");
    }

    @Test
    void findOverdueReleases_shouldReturnOverdueReleases() {
        // Given
        Release overdueRelease = new Release();
        overdueRelease.setCode("TST-v1.0");
        overdueRelease.setStatus(ReleaseStatus.IN_PROGRESS);
        overdueRelease.setPlannedReleaseDate(Instant.now().minusSeconds(86400)); // 1 day ago

        ReleaseDto releaseDto = new ReleaseDto(
                1L,
                "TST-v1.0",
                "Test",
                ReleaseStatus.IN_PROGRESS,
                Instant.now().minusSeconds(86400),
                null,
                "testuser",
                Instant.now(),
                null,
                null);

        when(releaseRepository.findOverdueReleases(any(Instant.class))).thenReturn(List.of(overdueRelease));
        when(releaseMapper.toDto(overdueRelease)).thenReturn(releaseDto);

        // When
        List<ReleaseDto> result = releaseService.findOverdueReleases();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("TST-v1.0");
    }

    @Test
    void findAtRiskReleases_shouldReturnAtRiskReleases() {
        // Given
        Release atRiskRelease = new Release();
        atRiskRelease.setCode("TST-v1.1");
        atRiskRelease.setStatus(ReleaseStatus.IN_PROGRESS);
        atRiskRelease.setPlannedReleaseDate(Instant.now().plusSeconds(3 * 86400)); // 3 days from now

        ReleaseDto releaseDto = new ReleaseDto(
                2L,
                "TST-v1.1",
                "Test",
                ReleaseStatus.IN_PROGRESS,
                Instant.now().plusSeconds(3 * 86400),
                null,
                "testuser",
                Instant.now(),
                null,
                null);

        when(releaseRepository.findAtRiskReleases(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(atRiskRelease));
        when(releaseMapper.toDto(atRiskRelease)).thenReturn(releaseDto);

        // When
        List<ReleaseDto> result = releaseService.findAtRiskReleases(7); // 7 days threshold

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("TST-v1.1");
    }

    @Test
    void findReleasesByStatus_shouldReturnReleasesWithGivenStatus() {
        // Given
        Release plannedRelease = new Release();
        plannedRelease.setCode("TST-v1.2");
        plannedRelease.setStatus(ReleaseStatus.PLANNED);

        ReleaseDto releaseDto = new ReleaseDto(
                3L,
                "TST-v1.2",
                "Test",
                ReleaseStatus.PLANNED,
                Instant.now(),
                null,
                "testuser",
                Instant.now(),
                null,
                null);

        when(releaseRepository.findByStatus(ReleaseStatus.PLANNED)).thenReturn(List.of(plannedRelease));
        when(releaseMapper.toDto(plannedRelease)).thenReturn(releaseDto);

        // When
        List<ReleaseDto> result = releaseService.findReleasesByStatus(ReleaseStatus.PLANNED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ReleaseStatus.PLANNED);
    }

    @Test
    void findReleasesWithFilters_shouldReturnPagedResults() {
        // Given
        Release filteredRelease = new Release();
        filteredRelease.setCode("TST-v1.3");
        filteredRelease.setStatus(ReleaseStatus.IN_PROGRESS);
        filteredRelease.setCreatedBy("testuser");

        ReleaseDto releaseDto = new ReleaseDto(
                4L,
                "TST-v1.3",
                "Test",
                ReleaseStatus.IN_PROGRESS,
                Instant.now(),
                null,
                "testuser",
                Instant.now(),
                null,
                null);

        Page<Release> releasePage = new PageImpl<>(List.of(filteredRelease), PageRequest.of(0, 20), 1);
        Page<ReleaseDto> expectedPage = releasePage.map(release -> releaseDto);

        when(releaseRepository.findAllWithFilters(any(), anyString(), any(), any(), any()))
                .thenReturn(releasePage);
        when(releaseMapper.toDto(filteredRelease)).thenReturn(releaseDto);

        // When
        Page<ReleaseDto> result = releaseService.findReleasesWithFilters(
                ReleaseStatus.IN_PROGRESS, "testuser", null, null, PageRequest.of(0, 20));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).createdBy()).isEqualTo("testuser");
        assertThat(result.getContent().get(0).status()).isEqualTo(ReleaseStatus.IN_PROGRESS);
    }
}
