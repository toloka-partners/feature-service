package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureDependencyMapper;
import com.sivalabs.ft.features.domain.models.DependencyType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureDependencyServiceTest {

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private FeatureDependencyRepository featureDependencyRepository;

    @Mock
    private FeatureDependencyMapper featureDependencyMapper;

    @InjectMocks
    private FeatureDependencyService service;

    private Feature featureA;
    private Feature featureB;
    private Feature featureC;
    private Feature featureD;
    private FeatureDependency dependencyAB;
    private FeatureDependency dependencyBC;
    private FeatureDependency dependencyCD;
    private FeatureDependencyDto dtoAB;
    private FeatureDependencyDto dtoBC;
    private FeatureDependencyDto dtoCD;

    @BeforeEach
    void setUp() {
        // Setup test features
        featureA = createFeature(1L, "FEAT-001", "Feature A", FeatureStatus.NEW);
        featureB = createFeature(2L, "FEAT-002", "Feature B", FeatureStatus.IN_PROGRESS);
        featureC = createFeature(3L, "FEAT-003", "Feature C", FeatureStatus.RELEASED);
        featureD = createFeature(4L, "FEAT-004", "Feature D", FeatureStatus.NEW);

        // Setup dependencies: A depends on B, B depends on C, C depends on D
        dependencyAB = createDependency(1L, featureA, featureB, DependencyType.HARD);
        dependencyBC = createDependency(2L, featureB, featureC, DependencyType.SOFT);
        dependencyCD = createDependency(3L, featureC, featureD, DependencyType.OPTIONAL);

        // Setup DTOs
        dtoAB = createDependencyDto(1L, "FEAT-001", "Feature A", "FEAT-002", "Feature B", DependencyType.HARD);
        dtoBC = createDependencyDto(2L, "FEAT-002", "Feature B", "FEAT-003", "Feature C", DependencyType.SOFT);
        dtoCD = createDependencyDto(3L, "FEAT-003", "Feature C", "FEAT-004", "Feature D", DependencyType.OPTIONAL);
    }

    @Test
    void getFeatureDependencies_WhenFeatureExists_ShouldReturnDependencies() {
        // Given
        when(featureRepository.findByCode("FEAT-001")).thenReturn(Optional.of(featureA));
        when(featureDependencyRepository.findByFeature_Code("FEAT-001")).thenReturn(List.of(dependencyAB));
        when(featureDependencyMapper.toDto(dependencyAB)).thenReturn(dtoAB);

        // When
        List<FeatureDependencyDto> dependencies = service.getFeatureDependencies("FEAT-001");

        // Then
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.get(0)).isEqualTo(dtoAB);
        verify(featureRepository).findByCode("FEAT-001");
        verify(featureDependencyRepository).findByFeature_Code("FEAT-001");
    }

    @Test
    void getFeatureDependencies_WhenFeatureNotFound_ShouldThrowException() {
        // Given
        when(featureRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getFeatureDependencies("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feature not found with code: INVALID");
    }

    @Test
    void getFeatureDependents_WhenFeatureHasDependents_ShouldReturnDependents() {
        // Given
        when(featureRepository.findByCode("FEAT-003")).thenReturn(Optional.of(featureC));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-003"))
                .thenReturn(List.of(dependencyBC));
        when(featureDependencyMapper.toDto(dependencyBC)).thenReturn(dtoBC);

        // When
        List<FeatureDependencyDto> dependents = service.getFeatureDependents("FEAT-003");

        // Then
        assertThat(dependents).hasSize(1);
        assertThat(dependents.get(0)).isEqualTo(dtoBC);
    }

    @Test
    void getFeatureImpact_ShouldReturnAllDependentsRecursively() {
        // Given - Feature D is modified, should impact C, B, and A
        when(featureRepository.findByCode("FEAT-004")).thenReturn(Optional.of(featureD));

        // Setup recursive dependency lookups
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-004"))
                .thenReturn(List.of(dependencyCD));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-003"))
                .thenReturn(List.of(dependencyBC));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-002"))
                .thenReturn(List.of(dependencyAB));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-001"))
                .thenReturn(Collections.emptyList());

        // Setup feature lookups
        when(featureRepository.findByCodeIn(anyList())).thenReturn(List.of(featureA, featureB, featureC));
        when(featureRepository.findAllById(anyList())).thenReturn(List.of(featureA, featureB, featureC));

        // Setup dependency lookups for impact results
        when(featureDependencyRepository.findByFeature_Code("FEAT-001")).thenReturn(List.of(dependencyAB));
        when(featureDependencyRepository.findByFeature_Code("FEAT-002")).thenReturn(List.of(dependencyBC));
        when(featureDependencyRepository.findByFeature_Code("FEAT-003")).thenReturn(List.of(dependencyCD));

        when(featureDependencyMapper.toDto(any())).thenReturn(dtoAB, dtoBC, dtoCD);

        // When
        List<FeatureDependencyDto> impact = service.getFeatureImpact("FEAT-004", null, null, null);

        // Then
        assertThat(impact).hasSize(3);
    }

    @Test
    void getFeatureImpact_WithProductFilter_ShouldFilterByProduct() {
        // Given
        Product product1 = new Product();
        product1.setCode("PROD-001");
        featureA.setProduct(product1);
        featureB.setProduct(product1);

        Product product2 = new Product();
        product2.setCode("PROD-002");
        featureC.setProduct(product2);

        when(featureRepository.findByCode("FEAT-004")).thenReturn(Optional.of(featureD));
        when(featureDependencyRepository.findByDependsOnFeature_Code(anyString()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(dependencyCD))
                .thenReturn(List.of(dependencyBC))
                .thenReturn(List.of(dependencyAB));

        when(featureRepository.findByCodeIn(anyList())).thenReturn(List.of(featureA, featureB, featureC));
        when(featureRepository.findAllById(anyList())).thenReturn(List.of(featureA, featureB, featureC));

        when(featureDependencyRepository.findByFeature_Code("FEAT-001")).thenReturn(List.of(dependencyAB));
        when(featureDependencyRepository.findByFeature_Code("FEAT-002")).thenReturn(List.of(dependencyBC));

        when(featureDependencyMapper.toDto(any())).thenReturn(dtoAB, dtoBC);

        // When
        List<FeatureDependencyDto> impact = service.getFeatureImpact("FEAT-004", "PROD-001", null, null);

        // Then
        assertThat(impact).hasSize(2); // Only features from PROD-001
    }

    @Test
    void getFeatureImpact_WithStatusFilter_ShouldFilterByStatus() {
        // Given
        when(featureRepository.findByCode("FEAT-004")).thenReturn(Optional.of(featureD));
        when(featureDependencyRepository.findByDependsOnFeature_Code(anyString()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(dependencyCD))
                .thenReturn(List.of(dependencyBC))
                .thenReturn(List.of(dependencyAB));

        when(featureRepository.findByCodeIn(anyList())).thenReturn(List.of(featureA, featureB, featureC));
        when(featureRepository.findAllById(anyList())).thenReturn(List.of(featureA, featureB, featureC));

        when(featureDependencyRepository.findByFeature_Code("FEAT-001")).thenReturn(List.of(dependencyAB));

        when(featureDependencyMapper.toDto(dependencyAB)).thenReturn(dtoAB);

        // When
        List<FeatureDependencyDto> impact = service.getFeatureImpact("FEAT-004", null, null, "NEW");

        // Then
        assertThat(impact).hasSize(1); // Only NEW features
    }

    @Test
    void getFeatureImpact_WithInvalidStatus_ShouldThrowException() {
        // Given
        when(featureRepository.findByCode("FEAT-001")).thenReturn(Optional.of(featureA));

        // When/Then
        assertThatThrownBy(() -> service.getFeatureImpact("FEAT-001", null, null, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid feature status: INVALID_STATUS");
    }

    @Test
    void getFeatureImpact_WithCyclicDependencies_ShouldNotInfiniteLoop() {
        // Given - Create a cycle: A -> B -> C -> A
        FeatureDependency dependencyCA = createDependency(4L, featureC, featureA, DependencyType.HARD);

        when(featureRepository.findByCode("FEAT-001")).thenReturn(Optional.of(featureA));

        // Setup cyclic dependencies
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-001"))
                .thenReturn(List.of(dependencyCA));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-003"))
                .thenReturn(List.of(dependencyBC));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-002"))
                .thenReturn(List.of(dependencyAB));

        when(featureRepository.findByCodeIn(anyList())).thenReturn(List.of(featureB, featureC));
        when(featureRepository.findAllById(anyList())).thenReturn(List.of(featureB, featureC));

        when(featureDependencyRepository.findByFeature_Code(anyString())).thenReturn(Collections.emptyList());

        // When
        List<FeatureDependencyDto> impact = service.getFeatureImpact("FEAT-001", null, null, null);

        // Then - Should complete without infinite loop
        assertThat(impact).isNotNull();
    }

    @Test
    void getFeatureImpact_WithNoDependents_ShouldReturnEmptyList() {
        // Given
        when(featureRepository.findByCode("FEAT-001")).thenReturn(Optional.of(featureA));
        when(featureDependencyRepository.findByDependsOnFeature_Code("FEAT-001"))
                .thenReturn(Collections.emptyList());

        // When
        List<FeatureDependencyDto> impact = service.getFeatureImpact("FEAT-001", null, null, null);

        // Then
        assertThat(impact).isEmpty();
    }

    private Feature createFeature(Long id, String code, String title, FeatureStatus status) {
        Feature feature = new Feature();
        feature.setId(id);
        feature.setCode(code);
        feature.setTitle(title);
        feature.setStatus(status);
        feature.setCreatedBy("test");
        feature.setCreatedAt(Instant.now());
        return feature;
    }

    private FeatureDependency createDependency(Long id, Feature feature, Feature dependsOn, DependencyType type) {
        FeatureDependency dependency = new FeatureDependency();
        dependency.setId(id);
        dependency.setFeature(feature);
        dependency.setDependsOnFeature(dependsOn);
        dependency.setDependencyType(type);
        dependency.setCreatedAt(Instant.now());
        return dependency;
    }

    private FeatureDependencyDto createDependencyDto(
            Long id,
            String featureCode,
            String featureTitle,
            String dependsOnCode,
            String dependsOnTitle,
            DependencyType type) {
        return new FeatureDependencyDto(
                id, featureCode, featureTitle, dependsOnCode, dependsOnTitle, type, null, Instant.now());
    }
}
