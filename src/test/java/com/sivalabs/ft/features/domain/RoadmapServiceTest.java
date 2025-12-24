package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.TestcontainersConfiguration;
import com.sivalabs.ft.features.domain.dtos.*;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import com.sivalabs.ft.features.domain.models.GroupBy;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import com.sivalabs.ft.features.domain.models.RiskLevel;
import com.sivalabs.ft.features.domain.models.TimelineAdherence;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Sql(scripts = {"/test-data.sql"})
class RoadmapServiceTest {

    @Autowired
    private RoadmapService roadmapService;

    @Test
    void testGetRoadmap() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, null, null, null, null);

        assertThat(roadmap).as("Roadmap response should not be null").isNotNull();
        assertThat(roadmap.roadmapItems())
                .as("Roadmap items should not be empty")
                .isNotEmpty();
        assertThat(roadmap.summary()).as("Summary should not be null").isNotNull();
        assertThat(roadmap.appliedFilters())
                .as("Applied filters should not be null")
                .isNotNull();
    }

    @Test
    void testGetRoadmapByProductCode() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap("intellij", null, null, null, null, null, null);

        assertThat(roadmap.roadmapItems())
                .as("Roadmap items should not be empty")
                .isNotEmpty();
        assertThat(roadmap.roadmapItems())
                .allMatch(item -> item.release().code().startsWith("IDEA"))
                .as("All releases should belong to IntelliJ product");
        assertThat(roadmap.appliedFilters().productCode())
                .as("Product code filter should be set")
                .isEqualTo("intellij");
    }

    @Test
    void testGetRoadmapWithDateRange() {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);

        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, startDate, endDate, null, null, null, null);

        assertThat(roadmap.appliedFilters().startDate())
                .as("Start date filter should be set")
                .isEqualTo(startDate);
        assertThat(roadmap.appliedFilters().endDate())
                .as("End date filter should be set")
                .isEqualTo(endDate);
    }

    @Test
    void testGetRoadmapIncludeCompleted() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, true, null, null, null);

        assertThat(roadmap.roadmapItems())
                .as("Roadmap should include completed releases")
                .isNotEmpty();
        assertThat(roadmap.appliedFilters().includeCompleted())
                .as("Include completed filter should be true")
                .isTrue();
    }

    @Test
    void testGetRoadmapExcludeCompleted() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, false, null, null, null);

        assertThat(roadmap.roadmapItems())
                .allMatch(item -> item.release().status() != ReleaseStatus.RELEASED)
                .as("All releases should not be in RELEASED status when excluding completed");
        assertThat(roadmap.appliedFilters().includeCompleted())
                .as("Include completed filter should be false")
                .isFalse();
    }

    @Test
    void testGetRoadmapFilterByOwner() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, null, null, "siva", null);

        assertThat(roadmap.roadmapItems())
                .as("Roadmap items should not be empty")
                .isNotEmpty();

        roadmap.roadmapItems().forEach(item -> {
            assertThat(item.features())
                    .allMatch(f -> "siva".equals(f.assignedTo()))
                    .as("All features should be assigned to 'siva'");
        });
        assertThat(roadmap.appliedFilters().owner())
                .as("Owner filter should be set")
                .isEqualTo("siva");
    }

    @Test
    void testGetRoadmapWithGroupBy() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, null, GroupBy.PRODUCT, null, null);

        assertThat(roadmap.appliedFilters().groupBy())
                .as("GroupBy filter should be set")
                .isEqualTo(GroupBy.PRODUCT);
    }

    @Test
    void testProgressMetricsCalculation() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap("intellij", null, null, null, null, null, null);

        assertThat(roadmap.roadmapItems()).isNotEmpty();

        RoadmapItemDto firstItem = roadmap.roadmapItems().getFirst();
        ProgressMetricsDto metrics = firstItem.progressMetrics();

        assertThat(metrics.totalFeatures())
                .as("Total features should be positive")
                .isGreaterThanOrEqualTo(0);
        assertThat(metrics.completionPercentage())
                .as("Completion percentage should be between 0 and 100")
                .isBetween(0.0, 100.0);
        assertThat(metrics.totalFeatures())
                .as("Total features should equal sum of all statuses")
                .isEqualTo(metrics.completedFeatures()
                        + metrics.inProgressFeatures()
                        + metrics.newFeatures()
                        + metrics.onHoldFeatures());
    }

    @Test
    void testHealthIndicatorsCalculation() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, null, null, null, null);

        assertThat(roadmap.roadmapItems()).isNotEmpty();

        roadmap.roadmapItems().forEach(item -> {
            HealthIndicatorsDto health = item.healthIndicators();
            assertThat(health.timelineAdherence())
                    .as("Timeline adherence should be set")
                    .isIn(TimelineAdherence.ON_SCHEDULE, TimelineAdherence.DELAYED, TimelineAdherence.CRITICAL);
            assertThat(health.riskLevel())
                    .as("Risk level should be set")
                    .isIn(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH);
            assertThat(health.blockedFeatures())
                    .as("Blocked features should be non-negative")
                    .isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void testRoadmapSummaryCalculation() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap(null, null, null, null, null, null, null);

        RoadmapSummaryDto summary = roadmap.summary();

        assertThat(summary.totalReleases())
                .as("Total releases should be positive")
                .isGreaterThan(0);
        assertThat(summary.totalFeatures())
                .as("Total features should be positive")
                .isGreaterThanOrEqualTo(0);
        assertThat(summary.overallCompletionPercentage())
                .as("Overall completion percentage should be between 0 and 100")
                .isBetween(0.0, 100.0);
        assertThat(summary.totalReleases())
                .as("Total releases should equal sum of completed and draft")
                .isEqualTo(summary.completedReleases() + summary.draftReleases());
    }

    @Test
    void testGetMultiProductRoadmap() {
        MultiProductRoadmapResponseDto roadmap = roadmapService.getMultiProductRoadmap(null, null, null, null, null);

        assertThat(roadmap).as("Multi-product roadmap should not be null").isNotNull();
        assertThat(roadmap.products()).as("Products should not be empty").isNotEmpty();

        roadmap.products().forEach(product -> {
            assertThat(product.productId()).as("Product ID should not be null").isNotNull();
            assertThat(product.productCode())
                    .as("Product code should not be null")
                    .isNotEmpty();
            assertThat(product.productName())
                    .as("Product name should not be null")
                    .isNotEmpty();
            assertThat(product.summary())
                    .as("Product summary should not be null")
                    .isNotNull();
        });
    }

    @Test
    void testGetRoadmapByOwner() {
        RoadmapByOwnerResponseDto roadmap =
                roadmapService.getRoadmapByOwner("siva", null, null, null, null, null, null);

        assertThat(roadmap).as("Roadmap by owner should not be null").isNotNull();
        assertThat(roadmap.owner()).as("Owner should be set").isEqualTo("siva");
        assertThat(roadmap.roadmapItems())
                .as("Roadmap items should not be empty")
                .isNotEmpty();

        roadmap.roadmapItems().forEach(item -> {
            assertThat(item.features())
                    .allMatch(f -> "siva".equals(f.assignedTo()))
                    .as("All features should be assigned to 'siva'");
        });
    }

    @Test
    void testFeatureStatusCounting() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap("intellij", null, null, null, null, null, null);

        assertThat(roadmap.roadmapItems()).isNotEmpty();

        RoadmapItemDto firstItem = roadmap.roadmapItems().getFirst();
        ProgressMetricsDto metrics = firstItem.progressMetrics();

        long actualNew = firstItem.features().stream()
                .filter(f -> f.status() == FeatureStatus.NEW)
                .count();
        long actualInProgress = firstItem.features().stream()
                .filter(f -> f.status() == FeatureStatus.IN_PROGRESS)
                .count();
        long actualReleased = firstItem.features().stream()
                .filter(f -> f.status() == FeatureStatus.RELEASED)
                .count();
        long actualOnHold = firstItem.features().stream()
                .filter(f -> f.status() == FeatureStatus.ON_HOLD)
                .count();

        assertThat(metrics.newFeatures()).as("New features count should match").isEqualTo((int) actualNew);
        assertThat(metrics.inProgressFeatures())
                .as("In progress features count should match")
                .isEqualTo((int) actualInProgress);
        assertThat(metrics.completedFeatures())
                .as("Completed features count should match")
                .isEqualTo((int) actualReleased);
        assertThat(metrics.onHoldFeatures())
                .as("On hold features count should match")
                .isEqualTo((int) actualOnHold);
    }

    @Test
    void testEmptyRoadmapForNonExistentProduct() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap("non-existent", null, null, null, null, null, null);

        assertThat(roadmap.roadmapItems())
                .as("Roadmap items should be empty for non-existent product")
                .isEmpty();
        assertThat(roadmap.summary().totalReleases())
                .as("Total releases should be 0 for non-existent product")
                .isEqualTo(0);
    }

    @Test
    void testFavoriteFeatureStatus() {
        RoadmapResponseDto roadmap = roadmapService.getRoadmap("intellij", null, null, null, null, null, "user");

        assertThat(roadmap.roadmapItems()).isNotEmpty();

        roadmap.roadmapItems().forEach(item -> {
            item.features().forEach(feature -> {
                assertThat(feature.isFavorite())
                        .as("Feature favorite status should be a boolean")
                        .isIn(true, false);
            });
        });
    }
}
