package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@WithMockOAuth2User
class RoadmapControllerTests extends AbstractIT {

    @Test
    void shouldGetBasicRoadmapWithoutFilters() {
        var result = mvc.get().uri("/api/roadmap").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();
                    assertThat(roadmap.summary()).isNotNull();
                    assertThat(roadmap.summary().totalReleases()).isGreaterThan(0);
                    assertThat(roadmap.appliedFilters()).isNotNull();
                    assertThat(roadmap.appliedFilters().productCodes()).isNull();
                    assertThat(roadmap.appliedFilters().statuses()).isNull();
                    assertThat(roadmap.appliedFilters().dateFrom()).isNull();
                    assertThat(roadmap.appliedFilters().dateTo()).isNull();
                    assertThat(roadmap.appliedFilters().groupBy()).isNull();
                    assertThat(roadmap.appliedFilters().owner()).isNull();
                });
    }

    @Test
    void shouldFilterRoadmapByProductCodes() {
        var result = mvc.get()
                .uri("/api/roadmap?productCodes={productCodes}", "intellij,goland")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();
                    assertThat(roadmap.appliedFilters().productCodes()).containsExactly("intellij", "goland");

                    // Verify all releases are from specified products
                    roadmap.roadmapItems().forEach(item -> {
                        String releaseCode = item.release().code();
                        assertThat(releaseCode)
                                .satisfiesAnyOf(code -> assertThat(code).startsWith("IDEA-"), code -> assertThat(code)
                                        .startsWith("GO-"));
                    });
                });
    }

    @Test
    void shouldFilterRoadmapByStatuses() {
        var result = mvc.get()
                .uri("/api/roadmap?statuses={statuses}", "RELEASED,COMPLETED")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();
                    assertThat(roadmap.appliedFilters().statuses()).containsExactly("RELEASED", "COMPLETED");

                    // Verify all releases have specified statuses
                    roadmap.roadmapItems().forEach(item -> {
                        String status = item.release().status().toString();
                        assertThat(status).isIn("RELEASED", "COMPLETED");
                    });
                });
    }

    @Test
    void shouldFilterRoadmapByDateRange() {
        var result = mvc.get()
                .uri("/api/roadmap?dateFrom={dateFrom}&dateTo={dateTo}", "2023-01-01", "2024-12-31")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.appliedFilters().dateFrom().toString()).isEqualTo("2023-01-01");
                    assertThat(roadmap.appliedFilters().dateTo().toString()).isEqualTo("2024-12-31");
                });
    }

    @Test
    void shouldGroupRoadmapByProductCode() {
        var result =
                mvc.get().uri("/api/roadmap?groupBy={groupBy}", "productCode").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();
                    assertThat(roadmap.appliedFilters().groupBy()).isEqualTo("productCode");
                });
    }

    @Test
    void shouldGroupRoadmapByStatus() {
        var result = mvc.get().uri("/api/roadmap?groupBy={groupBy}", "status").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();
                    assertThat(roadmap.appliedFilters().groupBy()).isEqualTo("status");
                });
    }

    @Test
    void shouldGroupRoadmapByOwner() {
        var result = mvc.get().uri("/api/roadmap?groupBy={groupBy}", "owner").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.appliedFilters().groupBy()).isEqualTo("owner");
                });
    }

    @Test
    void shouldReturnEmptyResultForNonExistentOwner() {
        var result = mvc.get()
                .uri("/api/roadmap?owner={owner}", "nonexistent@example.com")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isEmpty();
                    assertThat(roadmap.summary().totalReleases()).isEqualTo(0);
                    assertThat(roadmap.appliedFilters().owner()).isEqualTo("nonexistent@example.com");
                });
    }

    @Test
    void shouldIncludeProgressMetricsInRoadmapItems() {
        var result = mvc.get().uri("/api/roadmap").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();

                    roadmap.roadmapItems().forEach(item -> {
                        assertThat(item.progressMetrics()).isNotNull();
                        assertThat(item.progressMetrics().totalFeatures()).isNotNull();
                        assertThat(item.progressMetrics().completedFeatures()).isNotNull();
                        assertThat(item.progressMetrics().inProgressFeatures()).isNotNull();
                        assertThat(item.progressMetrics().newFeatures()).isNotNull();
                        assertThat(item.progressMetrics().onHoldFeatures()).isNotNull();
                        assertThat(item.progressMetrics().completionPercentage())
                                .isNotNull();
                    });
                });
    }

    @Test
    void shouldIncludeHealthIndicatorsInRoadmapItems() {
        var result = mvc.get().uri("/api/roadmap").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();

                    roadmap.roadmapItems().forEach(item -> {
                        assertThat(item.healthIndicators()).isNotNull();
                        assertThat(item.healthIndicators().riskLevel()).isIn("ZERO", "LOW", "MEDIUM", "HIGH");
                        // timelineAdherence can be null if plannedReleaseDate is missing
                    });
                });
    }

    @Test
    void shouldIncludeFeaturesInRoadmapItems() {
        var result = mvc.get().uri("/api/roadmap").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.roadmapItems()).isNotEmpty();

                    roadmap.roadmapItems().forEach(item -> {
                        assertThat(item.features()).isNotNull();
                        // Features list can be empty but should not be null
                    });
                });
    }

    @Test
    void shouldValidateInvalidDateFormat() {
        var result = mvc.get()
                .uri("/api/roadmap?dateFrom={dateFrom}", "invalid-date")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldValidateInvalidDateRange() {
        var result = mvc.get()
                .uri("/api/roadmap?dateFrom={dateFrom}&dateTo={dateTo}", "2024-12-31", "2024-01-01")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldValidateInvalidStatus() {
        var result = mvc.get()
                .uri("/api/roadmap?statuses={statuses}", "INVALID_STATUS")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldValidateInvalidGroupBy() {
        var result = mvc.get()
                .uri("/api/roadmap?groupBy={groupBy}", "invalidGroupBy")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldIgnoreExtraQueryParameters() {
        var result = mvc.get()
                .uri("/api/roadmap?productCodes={productCodes}&extraParam={extraParam}", "intellij", "ignored")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.appliedFilters().productCodes()).containsExactly("intellij");
                });
    }

    @Test
    void shouldHandleCaseInsensitiveStatuses() {
        var result = mvc.get()
                .uri("/api/roadmap?statuses={statuses}", "released,completed")
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldHandleCaseInsensitiveGroupBy() {
        var result =
                mvc.get().uri("/api/roadmap?groupBy={groupBy}", "ProductCode").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldExportRoadmapAsCSV() {
        var result = mvc.get().uri("/api/roadmap/export?format={format}", "CSV").exchange();

        assertThat(result).hasStatusOk().hasHeader("Content-Type", "text/csv");
    }

    @Test
    void shouldExportRoadmapAsPDF() {
        var result = mvc.get().uri("/api/roadmap/export?format={format}", "PDF").exchange();

        assertThat(result).hasStatusOk().hasHeader("Content-Type", "application/pdf");
    }

    @Test
    void shouldExportRoadmapWithFilters() {
        var result = mvc.get()
                .uri(
                        "/api/roadmap/export?format={format}&productCodes={productCodes}&statuses={statuses}",
                        "CSV",
                        "intellij",
                        "RELEASED")
                .exchange();

        assertThat(result).hasStatusOk().hasHeader("Content-Type", "text/csv");
    }

    @Test
    void shouldValidateMissingFormatInExport() {
        var result = mvc.get().uri("/api/roadmap/export").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldValidateInvalidFormatInExport() {
        var result =
                mvc.get().uri("/api/roadmap/export?format={format}", "INVALID").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHandleCaseInsensitiveExportFormat() {
        var result = mvc.get().uri("/api/roadmap/export?format={format}", "csv").exchange();

        assertThat(result).hasStatusOk().hasHeader("Content-Type", "text/csv");
    }

    @Test
    void shouldApplyAllFiltersSimultaneously() {
        var result = mvc.get()
                .uri(
                        "/api/roadmap?productCodes={productCodes}&statuses={statuses}&dateFrom={dateFrom}&dateTo={dateTo}&groupBy={groupBy}",
                        "intellij",
                        "RELEASED",
                        "2023-01-01",
                        "2024-12-31",
                        "status")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.appliedFilters().productCodes()).containsExactly("intellij");
                    assertThat(roadmap.appliedFilters().statuses()).containsExactly("RELEASED");
                    assertThat(roadmap.appliedFilters().dateFrom().toString()).isEqualTo("2023-01-01");
                    assertThat(roadmap.appliedFilters().dateTo().toString()).isEqualTo("2024-12-31");
                    assertThat(roadmap.appliedFilters().groupBy()).isEqualTo("status");
                });
    }

    @Test
    void shouldCalculateCorrectSummary() {
        var result = mvc.get().uri("/api/roadmap").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(roadmap -> {
                    assertThat(roadmap.summary()).isNotNull();
                    assertThat(roadmap.summary().totalReleases())
                            .isEqualTo(roadmap.roadmapItems().size());
                    assertThat(roadmap.summary().completedReleases()).isNotNull();
                    assertThat(roadmap.summary().draftReleases()).isNotNull();
                    assertThat(roadmap.summary().totalFeatures()).isNotNull();
                    assertThat(roadmap.summary().overallCompletionPercentage()).isNotNull();

                    // Verify summary calculations
                    int expectedTotalFeatures = roadmap.roadmapItems().stream()
                            .mapToInt(item -> item.progressMetrics().totalFeatures())
                            .sum();
                    assertThat(roadmap.summary().totalFeatures()).isEqualTo(expectedTotalFeatures);
                });
    }
}
