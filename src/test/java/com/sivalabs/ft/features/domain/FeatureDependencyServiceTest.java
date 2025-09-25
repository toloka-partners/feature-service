package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureDependencyServiceTest {

    @Mock
    private FeatureDependencyRepository featureDependencyRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private EventPublisher eventPublisher;

    private FeatureDependencyService featureDependencyService;

    @BeforeEach
    void setUp() {
        featureDependencyService =
                new FeatureDependencyService(featureDependencyRepository, featureRepository, eventPublisher);
    }

    @Test
    void shouldCreateDependencySuccessfully() {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        DependencyType dependencyType = DependencyType.HARD;
        String notes = "This feature requires FEAT-002";
        String createdBy = "testuser";

        Feature feature1 = createFeature(featureCode);
        Feature feature2 = createFeature(dependsOnFeatureCode);

        when(featureRepository.findByCode(featureCode)).thenReturn(Optional.of(feature1));
        when(featureRepository.findByCode(dependsOnFeatureCode)).thenReturn(Optional.of(feature2));
        when(featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode))
                .thenReturn(Optional.empty());
        when(featureDependencyRepository.findByFeature_Code(dependsOnFeatureCode))
                .thenReturn(List.of());

        FeatureDependency savedDependency = new FeatureDependency();
        savedDependency.setFeature(feature1);
        savedDependency.setDependsOnFeature(feature2);
        savedDependency.setDependencyType(dependencyType);
        savedDependency.setNotes(notes);
        savedDependency.setCreatedAt(Instant.now());

        when(featureDependencyRepository.save(any(FeatureDependency.class))).thenReturn(savedDependency);

        FeatureDependency result = featureDependencyService.createDependency(
                featureCode, dependsOnFeatureCode, dependencyType, notes, createdBy);

        assertThat(result).isNotNull();
        assertThat(result.getFeature()).isEqualTo(feature1);
        assertThat(result.getDependsOnFeature()).isEqualTo(feature2);
        assertThat(result.getDependencyType()).isEqualTo(dependencyType);
        assertThat(result.getNotes()).isEqualTo(notes);

        verify(eventPublisher).publishDependencyCreatedEvent(savedDependency, createdBy);
    }

    @Test
    void shouldThrowExceptionWhenFeatureDependsOnItself() {
        String featureCode = "FEAT-001";
        String createdBy = "testuser";

        assertThatThrownBy(() -> featureDependencyService.createDependency(
                        featureCode, featureCode, DependencyType.HARD, null, createdBy))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A feature cannot depend on itself");

        verify(featureRepository, never()).findByCode(anyString());
    }

    @Test
    void shouldThrowExceptionWhenFeatureNotFound() {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        String createdBy = "testuser";

        when(featureRepository.findByCode(featureCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featureDependencyService.createDependency(
                        featureCode, dependsOnFeatureCode, DependencyType.HARD, null, createdBy))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Feature not found: " + featureCode);
    }

    @Test
    void shouldThrowExceptionWhenDependencyAlreadyExists() {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        String createdBy = "testuser";

        Feature feature1 = createFeature(featureCode);
        Feature feature2 = createFeature(dependsOnFeatureCode);

        when(featureRepository.findByCode(featureCode)).thenReturn(Optional.of(feature1));
        when(featureRepository.findByCode(dependsOnFeatureCode)).thenReturn(Optional.of(feature2));
        when(featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode))
                .thenReturn(Optional.of(new FeatureDependency()));

        assertThatThrownBy(() -> featureDependencyService.createDependency(
                        featureCode, dependsOnFeatureCode, DependencyType.HARD, null, createdBy))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Dependency already exists between these features");
    }

    @Test
    void shouldUpdateDependencySuccessfully() {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        DependencyType newType = DependencyType.SOFT;
        String newNotes = "Updated notes";
        String updatedBy = "testuser";

        FeatureDependency existingDependency = new FeatureDependency();
        existingDependency.setDependencyType(DependencyType.HARD);
        existingDependency.setNotes("Old notes");

        when(featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode))
                .thenReturn(Optional.of(existingDependency));
        when(featureDependencyRepository.save(existingDependency)).thenReturn(existingDependency);

        FeatureDependency result = featureDependencyService.updateDependency(
                featureCode, dependsOnFeatureCode, newType, newNotes, updatedBy);

        assertThat(result).isNotNull();
        assertThat(result.getDependencyType()).isEqualTo(newType);
        assertThat(result.getNotes()).isEqualTo(newNotes);

        verify(eventPublisher).publishDependencyUpdatedEvent(existingDependency, updatedBy);
    }

    @Test
    void shouldDeleteDependencySuccessfully() {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        String deletedBy = "testuser";

        FeatureDependency existingDependency = new FeatureDependency();
        existingDependency.setDependencyType(DependencyType.HARD);
        existingDependency.setNotes("Some notes");

        when(featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode))
                .thenReturn(Optional.of(existingDependency));

        featureDependencyService.deleteDependency(featureCode, dependsOnFeatureCode, deletedBy);

        verify(featureDependencyRepository).delete(existingDependency);
        verify(eventPublisher)
                .publishDependencyDeletedEvent(
                        featureCode, dependsOnFeatureCode, DependencyType.HARD, "Some notes", deletedBy);
    }

    @Test
    void shouldThrowExceptionWhenCyclicDependencyDetected() {
        String featureCode1 = "FEAT-001";
        String featureCode2 = "FEAT-002";
        String featureCode3 = "FEAT-003";
        String createdBy = "testuser";

        Feature feature1 = createFeature(featureCode1);
        Feature feature2 = createFeature(featureCode2);
        Feature feature3 = createFeature(featureCode3);

        FeatureDependency dep1 = new FeatureDependency();
        dep1.setFeature(feature2);
        dep1.setDependsOnFeature(feature3);

        FeatureDependency dep2 = new FeatureDependency();
        dep2.setFeature(feature3);
        dep2.setDependsOnFeature(feature1);

        when(featureRepository.findByCode(featureCode1)).thenReturn(Optional.of(feature1));
        when(featureRepository.findByCode(featureCode2)).thenReturn(Optional.of(feature2));
        when(featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(featureCode1, featureCode2))
                .thenReturn(Optional.empty());
        when(featureDependencyRepository.findByFeature_Code(featureCode2)).thenReturn(List.of(dep1));
        when(featureDependencyRepository.findByFeature_Code(featureCode3)).thenReturn(List.of(dep2));

        assertThatThrownBy(() -> featureDependencyService.createDependency(
                        featureCode1, featureCode2, DependencyType.HARD, null, createdBy))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Creating this dependency would result in a cyclic dependency");
    }

    private Feature createFeature(String code) {
        Feature feature = new Feature();
        feature.setCode(code);
        feature.setTitle("Feature " + code);
        return feature;
    }
}
