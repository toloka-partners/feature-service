package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.TestcontainersConfiguration;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ReleaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Use existing test data or create a new product
        testProduct = productRepository
                .findByCode("intellij")
                .orElseThrow(() -> new RuntimeException("Test product not found"));
    }

    @Test
    void shouldSaveAndRetrieveReleaseWithPlanningFields() {
        // Given
        Instant now = Instant.now();
        Instant plannedStart = now.plus(1, ChronoUnit.DAYS);
        Instant plannedRelease = now.plus(7, ChronoUnit.DAYS);
        Instant actualRelease = now.plus(8, ChronoUnit.DAYS);

        Release release = new Release();
        release.setProduct(testProduct);
        release.setCode("TEST-v1.0.0");
        release.setDescription("Test release with planning fields");
        release.setStatus(ReleaseStatus.PLANNED);
        release.setPlannedStartDate(plannedStart);
        release.setPlannedReleaseDate(plannedRelease);
        release.setActualReleaseDate(actualRelease);
        release.setOwner("test.owner");
        release.setNotes("Test notes for release planning");
        release.setCreatedBy("test.user");
        release.setCreatedAt(now);

        // When
        Release savedRelease = releaseRepository.save(release);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Release> retrievedRelease = releaseRepository.findById(savedRelease.getId());
        assertThat(retrievedRelease).isPresent();

        Release actual = retrievedRelease.get();
        assertThat(actual.getCode()).isEqualTo("TEST-v1.0.0");
        assertThat(actual.getDescription()).isEqualTo("Test release with planning fields");
        assertThat(actual.getStatus()).isEqualTo(ReleaseStatus.PLANNED);
        assertThat(actual.getPlannedStartDate()).isEqualTo(plannedStart);
        assertThat(actual.getPlannedReleaseDate()).isEqualTo(plannedRelease);
        assertThat(actual.getActualReleaseDate()).isEqualTo(actualRelease);
        assertThat(actual.getOwner()).isEqualTo("test.owner");
        assertThat(actual.getNotes()).isEqualTo("Test notes for release planning");
        assertThat(actual.getCreatedBy()).isEqualTo("test.user");
    }

    @Test
    void shouldSaveReleaseWithNullPlanningFields() {
        // Given
        Release release = new Release();
        release.setProduct(testProduct);
        release.setCode("TEST-v1.1.0");
        release.setDescription("Test release with null planning fields");
        release.setStatus(ReleaseStatus.DRAFT);
        release.setPlannedStartDate(null);
        release.setPlannedReleaseDate(null);
        release.setActualReleaseDate(null);
        release.setOwner(null);
        release.setNotes(null);
        release.setCreatedBy("test.user");
        release.setCreatedAt(Instant.now());

        // When
        Release savedRelease = releaseRepository.save(release);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Release> retrievedRelease = releaseRepository.findById(savedRelease.getId());
        assertThat(retrievedRelease).isPresent();

        Release actual = retrievedRelease.get();
        assertThat(actual.getPlannedStartDate()).isNull();
        assertThat(actual.getPlannedReleaseDate()).isNull();
        assertThat(actual.getActualReleaseDate()).isNull();
        assertThat(actual.getOwner()).isNull();
        assertThat(actual.getNotes()).isNull();
    }

    @Test
    void shouldUpdatePlanningFields() {
        // Given
        Release release = new Release();
        release.setProduct(testProduct);
        release.setCode("TEST-v1.2.0");
        release.setDescription("Test release for update");
        release.setStatus(ReleaseStatus.DRAFT);
        release.setCreatedBy("test.user");
        release.setCreatedAt(Instant.now());

        Release savedRelease = releaseRepository.save(release);
        entityManager.flush();

        // When - Update planning fields
        Instant plannedStart = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant plannedRelease = Instant.now().plus(10, ChronoUnit.DAYS);

        savedRelease.setStatus(ReleaseStatus.PLANNED);
        savedRelease.setPlannedStartDate(plannedStart);
        savedRelease.setPlannedReleaseDate(plannedRelease);
        savedRelease.setOwner("updated.owner");
        savedRelease.setNotes("Updated notes");
        savedRelease.setUpdatedBy("test.updater");
        savedRelease.setUpdatedAt(Instant.now());

        releaseRepository.save(savedRelease);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Release> updatedRelease = releaseRepository.findById(savedRelease.getId());
        assertThat(updatedRelease).isPresent();

        Release actual = updatedRelease.get();
        assertThat(actual.getStatus()).isEqualTo(ReleaseStatus.PLANNED);
        assertThat(actual.getPlannedStartDate()).isEqualTo(plannedStart);
        assertThat(actual.getPlannedReleaseDate()).isEqualTo(plannedRelease);
        assertThat(actual.getOwner()).isEqualTo("updated.owner");
        assertThat(actual.getNotes()).isEqualTo("Updated notes");
        assertThat(actual.getUpdatedBy()).isEqualTo("test.updater");
        assertThat(actual.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindReleasesByProductCode() {
        // Given
        Release release1 = createTestRelease("TEST-v2.0.0", ReleaseStatus.PLANNED);
        Release release2 = createTestRelease("TEST-v2.1.0", ReleaseStatus.IN_PROGRESS);

        releaseRepository.save(release1);
        releaseRepository.save(release2);
        entityManager.flush();

        // When
        List<Release> releases = releaseRepository.findByProductCode(testProduct.getCode());

        // Then
        assertThat(releases).hasSizeGreaterThanOrEqualTo(2);
        assertThat(releases).extracting(Release::getCode).contains("TEST-v2.0.0", "TEST-v2.1.0");
    }

    @Test
    void shouldFindReleaseByCode() {
        // Given
        Release release = createTestRelease("TEST-v3.0.0", ReleaseStatus.COMPLETED);
        releaseRepository.save(release);
        entityManager.flush();

        // When
        Optional<Release> found = releaseRepository.findByCode("TEST-v3.0.0");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("TEST-v3.0.0");
        assertThat(found.get().getStatus()).isEqualTo(ReleaseStatus.COMPLETED);
    }

    @Test
    void shouldCheckReleaseExistsByCode() {
        // Given
        Release release = createTestRelease("TEST-v4.0.0", ReleaseStatus.RELEASED);
        releaseRepository.save(release);
        entityManager.flush();

        // When & Then
        assertThat(releaseRepository.existsByCode("TEST-v4.0.0")).isTrue();
        assertThat(releaseRepository.existsByCode("NON-EXISTENT")).isFalse();
    }

    @Test
    void shouldDeleteReleaseByCode() {
        // Given
        Release release = createTestRelease("TEST-v5.0.0", ReleaseStatus.CANCELLED);
        releaseRepository.save(release);
        entityManager.flush();

        assertThat(releaseRepository.existsByCode("TEST-v5.0.0")).isTrue();

        // When
        releaseRepository.deleteByCode("TEST-v5.0.0");
        entityManager.flush();

        // Then
        assertThat(releaseRepository.existsByCode("TEST-v5.0.0")).isFalse();
    }

    private Release createTestRelease(String code, ReleaseStatus status) {
        Release release = new Release();
        release.setProduct(testProduct);
        release.setCode(code);
        release.setDescription("Test release: " + code);
        release.setStatus(status);
        release.setPlannedStartDate(Instant.now().plus(1, ChronoUnit.DAYS));
        release.setPlannedReleaseDate(Instant.now().plus(7, ChronoUnit.DAYS));
        release.setOwner("test.owner");
        release.setNotes("Test notes for " + code);
        release.setCreatedBy("test.user");
        release.setCreatedAt(Instant.now());
        return release;
    }
}
