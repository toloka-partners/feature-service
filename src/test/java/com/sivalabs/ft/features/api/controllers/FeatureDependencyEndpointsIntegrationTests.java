package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class FeatureDependencyEndpointsIntegrationTests extends AbstractIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean up any existing dependencies for test features
        jdbcTemplate.update(
                "DELETE FROM feature_dependencies WHERE feature_code IN ('IDEA-1', 'IDEA-2', 'GO-3', 'TEST-1', 'TEST-2')");
    }

    // === DEPENDENCIES ENDPOINT TESTS ===

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetDependenciesForFeature() {
        // Given - Create some dependencies
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "Hard dependency");
        createTestDependency("IDEA-1", "GO-3", "SOFT", "Soft dependency");

        // When - Get dependencies
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "IDEA-1")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnEmptyListWhenFeatureHasNoDependencies() {
        // When - Get dependencies for feature with no dependencies
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "IDEA-2")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturn404WhenFeatureDoesNotExist() {
        // When - Get dependencies for non-existent feature
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "NON-EXISTENT")
                .exchange();

        // Then
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependenciesByProductCode() {
        // Given - Create dependencies
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "Hard dependency");
        createTestDependency("IDEA-1", "GO-3", "SOFT", "Soft dependency");

        // When - Get dependencies filtered by product code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies?productCode=goland", "IDEA-1")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);

        // Verify the returned dependency
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.dependsOnFeature().code()).isEqualTo("GO-3");
                    assertThat(dto.dependencyType().name()).isEqualTo("SOFT");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependenciesByReleaseCode() {
        // Given - Create dependencies
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "Hard dependency");
        createTestDependency("IDEA-1", "GO-3", "SOFT", "Soft dependency");

        // When - Get dependencies filtered by release code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies?releaseCode=IDEA-2023.3.8", "IDEA-1")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependenciesByStatus() {
        // Given - Create dependencies
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "Hard dependency");
        createTestDependency("IDEA-1", "GO-3", "SOFT", "Soft dependency");

        // When - Get dependencies filtered by status
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependencies?status=IN_PROGRESS", "IDEA-1")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    // === DEPENDENTS ENDPOINT TESTS ===

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetDependentsForFeature() {
        // Given - Create some features that depend on IDEA-2
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("GO-3", "IDEA-2", "SOFT", "GO-3 depends on IDEA-2");

        // When - Get dependents
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependents", "IDEA-2")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);

        // Verify the structure of the first dependent
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.feature()).isNotNull();
                    assertThat(dto.dependsOnFeature()).isNotNull();
                    assertThat(dto.dependencyType()).isNotNull();
                    assertThat(dto.dependsOnFeature().code()).isEqualTo("IDEA-2");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnEmptyListWhenFeatureHasNoDependents() {
        // When - Get dependents for feature with no dependents
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependents", "IDEA-1")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependentsByProductCode() {
        // Given - Create features that depend on IDEA-2
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("GO-3", "IDEA-2", "SOFT", "GO-3 depends on IDEA-2");

        // When - Get dependents filtered by product code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependents?productCode=goland", "IDEA-2")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);

        // Verify the returned dependent
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.feature().code()).isEqualTo("GO-3");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependentsByReleaseCode() {
        // Given - Create features that depend on IDEA-2
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("GO-3", "IDEA-2", "SOFT", "GO-3 depends on IDEA-2");

        // When - Get dependents filtered by release code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependents?releaseCode=IDEA-2023.3.8", "IDEA-2")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterDependentsByStatus() {
        // Given - Create features that depend on IDEA-2
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("GO-3", "IDEA-2", "SOFT", "GO-3 depends on IDEA-2");

        // When - Get dependents filtered by status
        var result = mvc.get()
                .uri("/api/features/{featureCode}/dependents?status=IN_PROGRESS", "IDEA-2")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    // === IMPACT ANALYSIS ENDPOINT TESTS ===

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetImpactAnalysisForFeature() {
        // Given - Create a dependency chain: IDEA-1 -> IDEA-2 -> GO-3
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");

        // When - Get impact analysis (should return all affected features)
        var result = mvc.get().uri("/api/features/{featureCode}/impact", "GO-3").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2); // Should include both IDEA-2 and IDEA-1

        // Verify we get the correct feature codes in the impact
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.feature().code()).isIn("IDEA-2", "IDEA-1");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleCircularDependenciesInImpactAnalysis() {
        // Given - Create circular dependencies: IDEA-1 -> IDEA-2 -> GO-3 -> IDEA-1
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");
        createTestDependency("GO-3", "IDEA-1", "OPTIONAL", "GO-3 depends on IDEA-1");

        // When - Get impact analysis
        var result =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2); // Should include IDEA-2 and GO-3 (but not IDEA-1 itself)

        // Verify circular reference is handled correctly (no infinite loop)
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.feature().code()).isIn("IDEA-2", "GO-3");
                    assertThat(dto.feature().code()).isNotEqualTo("IDEA-1");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnEmptyImpactWhenFeatureHasNoDependents() {
        // When - Get impact analysis for feature with no dependents
        var result =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterImpactAnalysisByProductCode() {
        // Given - Create a dependency chain with mixed products
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");

        // When - Get impact analysis filtered by product code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/impact?productCode=intellij", "GO-3")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);

        // Should return IDEA-1 and IDEA-2 impact
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.feature().code()).isIn("IDEA-1", "IDEA-2");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterImpactAnalysisByReleaseCode() {
        // Given - Create a dependency chain
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");

        // When - Get impact analysis filtered by release code
        var result = mvc.get()
                .uri("/api/features/{featureCode}/impact?releaseCode=IDEA-2023.3.8", "GO-3")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldFilterImpactAnalysisByStatus() {
        // Given - Create a dependency chain
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");

        // When - Get impact analysis filtered by status
        var result = mvc.get()
                .uri("/api/features/{featureCode}/impact?status=NEW", "GO-3")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleCircularFeatureDeletionThroughAPI() {
        // Given - Create circular dependency: IDEA-1 -> IDEA-2 -> GO-3 -> IDEA-1
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");
        createTestDependency("GO-3", "IDEA-1", "OPTIONAL", "GO-3 depends on IDEA-1 - creates cycle");

        // When - Get impact analysis before deletion
        var impactBefore =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then - Verify circular dependency exists
        assertThat(impactBefore)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2); // Should include IDEA-2 and GO-3

        // When - Delete GO-3 feature through API
        var deleteResult = mvc.delete().uri("/api/features/{code}", "GO-3").exchange();

        // Then - Verify deletion was successful
        assertThat(deleteResult).hasStatusOk();

        // When - Get impact analysis after deletion
        var impactAfter =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then - Verify circular dependency is broken
        assertThat(impactAfter)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0); // Should be empty after breaking the cycle

        // Verify GO-3 no longer exists
        var getDeletedFeature = mvc.get().uri("/api/features/{code}", "GO-3").exchange();
        assertThat(getDeletedFeature).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleCircularDependencyDeletionThroughAPI() {
        // Given - Create circular dependency: IDEA-1 -> IDEA-2 -> GO-3 -> IDEA-1
        createTestDependency("IDEA-1", "IDEA-2", "HARD", "IDEA-1 depends on IDEA-2");
        createTestDependency("IDEA-2", "GO-3", "SOFT", "IDEA-2 depends on GO-3");
        createTestDependency("GO-3", "IDEA-1", "OPTIONAL", "GO-3 depends on IDEA-1 - creates cycle");

        // When - Get impact analysis before dependency deletion
        var impactBefore =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then - Verify circular dependency exists
        assertThat(impactBefore)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2); // Should include IDEA-2 and GO-3

        // When - Delete the circular dependency GO-3 -> IDEA-1 through API
        var deleteDependencyResult = mvc.delete()
                .uri("/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}", "GO-3", "IDEA-1")
                .exchange();

        // Then - Verify dependency deletion was successful
        assertThat(deleteDependencyResult).hasStatusOk();

        // When - Get impact analysis after dependency deletion
        var impactAfter =
                mvc.get().uri("/api/features/{featureCode}/impact", "IDEA-1").exchange();

        // Then - Verify circular dependency is broken but features still exist
        assertThat(impactAfter)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0); // Should be empty (GO-3 no longer depends on IDEA-1, breaking the cycle)

        // Verify all features still exist
        var getIdea1 = mvc.get().uri("/api/features/{code}", "IDEA-1").exchange();
        assertThat(getIdea1).hasStatusOk();

        var getIdea2 = mvc.get().uri("/api/features/{code}", "IDEA-2").exchange();
        assertThat(getIdea2).hasStatusOk();

        var getGo3 = mvc.get().uri("/api/features/{code}", "GO-3").exchange();
        assertThat(getGo3).hasStatusOk();

        // Verify the circular dependency was actually removed
        var verifyDependencyGone = mvc.delete()
                .uri("/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}", "GO-3", "IDEA-1")
                .exchange();
        assertThat(verifyDependencyGone).hasStatus(HttpStatus.NOT_FOUND);

        // Verify remaining dependencies are still intact
        // IDEA-1 -> IDEA-2 should still exist
        var idea1Dependencies = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "IDEA-1")
                .exchange();
        assertThat(idea1Dependencies)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);

        assertThat(idea1Dependencies)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.dependsOnFeature().code()).isEqualTo("IDEA-2");
                    assertThat(dto.dependencyType().name()).isEqualTo("HARD");
                });

        // IDEA-2 -> GO-3 should still exist
        var idea2Dependencies = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "IDEA-2")
                .exchange();
        assertThat(idea2Dependencies)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);

        assertThat(idea2Dependencies)
                .bodyJson()
                .extractingPath("$[0]")
                .convertTo(FeatureDependencyDto.class)
                .satisfies(dto -> {
                    assertThat(dto.dependsOnFeature().code()).isEqualTo("GO-3");
                    assertThat(dto.dependencyType().name()).isEqualTo("SOFT");
                });

        // GO-3 should have no dependencies now (circular dependency removed)
        var go3Dependencies = mvc.get()
                .uri("/api/features/{featureCode}/dependencies", "GO-3")
                .exchange();
        assertThat(go3Dependencies)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(0);
    }

    // === HELPER METHODS ===

    private void createTestDependency(
            String featureCode, String dependsOnFeatureCode, String dependencyType, String notes) {
        var payload = String.format(
                """
            {
                "dependsOnFeatureCode": "%s",
                "dependencyType": "%s",
                "notes": "%s"
            }
            """,
                dependsOnFeatureCode, dependencyType, notes);

        mvc.post()
                .uri("/api/features/{featureCode}/dependencies", featureCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
    }
}
