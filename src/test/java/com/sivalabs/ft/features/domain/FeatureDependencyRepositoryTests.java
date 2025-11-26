package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.models.DependencyType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FeatureDependencyRepositoryTests extends AbstractIT {

    @Autowired
    private FeatureDependencyRepository dependencyRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Feature feature1;
    private Feature feature2;
    private Feature feature3;
    private Product product;

    @BeforeEach
    void setUp() {
        product = createAndSaveProduct();
        feature1 = createAndSaveFeature("FEAT-001", "Feature 1", product);
        feature2 = createAndSaveFeature("FEAT-002", "Feature 2", product);
        feature3 = createAndSaveFeature("FEAT-003", "Feature 3", product);
    }

    @Test
    void shouldSaveFeatureDependency() {
        FeatureDependency dependency = createDependency(feature1, feature2, DependencyType.HARD);

        FeatureDependency saved = dependencyRepository.save(dependency);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFeature().getCode()).isEqualTo("FEAT-001");
        assertThat(saved.getDependsOnFeature().getCode()).isEqualTo("FEAT-002");
        assertThat(saved.getDependencyType()).isEqualTo(DependencyType.HARD);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindDependenciesByFeatureCode() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));
        dependencyRepository.save(createDependency(feature1, feature3, DependencyType.SOFT));

        List<FeatureDependency> dependencies = dependencyRepository.findByFeature_Code("FEAT-001");

        assertThat(dependencies).hasSize(2);
        assertThat(dependencies)
                .extracting(d -> d.getDependsOnFeature().getCode())
                .containsExactlyInAnyOrder("FEAT-002", "FEAT-003");
    }

    @Test
    void shouldFindDependentsByDependsOnFeatureCode() {
        dependencyRepository.save(createDependency(feature1, feature3, DependencyType.HARD));
        dependencyRepository.save(createDependency(feature2, feature3, DependencyType.OPTIONAL));

        List<FeatureDependency> dependents = dependencyRepository.findByDependsOnFeature_Code("FEAT-003");

        assertThat(dependents).hasSize(2);
        assertThat(dependents)
                .extracting(d -> d.getFeature().getCode())
                .containsExactlyInAnyOrder("FEAT-001", "FEAT-002");
    }

    @Test
    void shouldFindSpecificDependency() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.SOFT));

        Optional<FeatureDependency> found =
                dependencyRepository.findByFeature_CodeAndDependsOnFeature_Code("FEAT-001", "FEAT-002");

        assertThat(found).isPresent();
        assertThat(found.get().getDependencyType()).isEqualTo(DependencyType.SOFT);
    }

    @Test
    void shouldCheckExistenceOfDependency() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));

        boolean exists = dependencyRepository.existsByFeature_CodeAndDependsOnFeature_Code("FEAT-001", "FEAT-002");
        boolean notExists = dependencyRepository.existsByFeature_CodeAndDependsOnFeature_Code("FEAT-001", "FEAT-003");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldDeleteSpecificDependency() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));
        dependencyRepository.save(createDependency(feature1, feature3, DependencyType.SOFT));

        dependencyRepository.deleteByFeature_CodeAndDependsOnFeature_Code("FEAT-001", "FEAT-002");

        List<FeatureDependency> remaining = dependencyRepository.findByFeature_Code("FEAT-001");
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getDependsOnFeature().getCode()).isEqualTo("FEAT-003");
    }

    @Test
    void shouldDeleteAllDependenciesForFeature() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));
        dependencyRepository.save(createDependency(feature1, feature3, DependencyType.SOFT));

        dependencyRepository.deleteByFeature_Code("FEAT-001");

        List<FeatureDependency> dependencies = dependencyRepository.findByFeature_Code("FEAT-001");
        assertThat(dependencies).isEmpty();
    }

    @Test
    void shouldPreventDuplicateDependencies() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));

        FeatureDependency duplicate = createDependency(feature1, feature2, DependencyType.SOFT);

        assertThatThrownBy(() -> {
                    dependencyRepository.saveAndFlush(duplicate);
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPreventSelfDependency() {
        FeatureDependency selfDependency = createDependency(feature1, feature1, DependencyType.HARD);

        assertThatThrownBy(() -> {
                    dependencyRepository.saveAndFlush(selfDependency);
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPreventDependencyOnNonExistentFeature() {
        Feature unsavedFeature = new Feature();
        unsavedFeature.setCode("UNSAVED");

        FeatureDependency invalidDependency = createDependency(feature1, unsavedFeature, DependencyType.HARD);

        assertThatThrownBy(() -> {
                    dependencyRepository.saveAndFlush(invalidDependency);
                })
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("transient");
    }

    @Test
    void shouldCascadeDeleteWhenFeatureIsDeleted() {
        dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));
        dependencyRepository.save(createDependency(feature2, feature3, DependencyType.SOFT));

        // Verify dependencies exist
        assertThat(dependencyRepository.findAll()).hasSize(2);

        // Clear the entity manager to avoid transient object issues
        entityManager.clear();

        // Delete feature2 which should cascade delete both dependencies
        featureRepository.deleteById(feature2.getId());

        // Verify all dependencies are deleted
        assertThat(dependencyRepository.findAll()).isEmpty();
    }

    @Test
    void shouldUpdateDependency() {
        FeatureDependency dependency =
                dependencyRepository.save(createDependency(feature1, feature2, DependencyType.HARD));

        dependency.setDependencyType(DependencyType.OPTIONAL);
        dependency.setNotes("Updated to optional dependency");
        dependencyRepository.save(dependency);

        FeatureDependency updated =
                dependencyRepository.findById(dependency.getId()).orElseThrow();
        assertThat(updated.getDependencyType()).isEqualTo(DependencyType.OPTIONAL);
        assertThat(updated.getNotes()).isEqualTo("Updated to optional dependency");
    }

    private Product createAndSaveProduct() {
        Product product = new Product();
        product.setCode("PROD-001");
        product.setPrefix("PRD");
        product.setName("Test Product");
        product.setDescription("Test Product Description");
        product.setImageUrl("https://example.com/product.jpg");
        product.setCreatedBy("test-user");
        product.setCreatedAt(Instant.now());
        return productRepository.save(product);
    }

    private Feature createAndSaveFeature(String code, String title, Product product) {
        Feature feature = new Feature();
        feature.setCode(code);
        feature.setTitle(title);
        feature.setStatus(FeatureStatus.NEW);
        feature.setProduct(product);
        feature.setCreatedBy("test-user");
        feature.setCreatedAt(Instant.now());
        return featureRepository.save(feature);
    }

    private FeatureDependency createDependency(Feature feature, Feature dependsOn, DependencyType type) {
        FeatureDependency dependency = new FeatureDependency();
        dependency.setFeature(feature);
        dependency.setDependsOnFeature(dependsOn);
        dependency.setDependencyType(type);
        return dependency;
    }
}
