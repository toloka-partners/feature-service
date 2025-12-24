package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.MultiProductRoadmapResponseDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapByOwnerResponseDto;
import com.sivalabs.ft.features.domain.dtos.RoadmapResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RoadmapControllerTests extends AbstractIT {

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmap() {
        var result = mvc.get().uri("/api/roadmap").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    assertThat(dto.summary()).isNotNull();
                    assertThat(dto.appliedFilters()).isNotNull();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapByProductCode() {
        var result =
                mvc.get().uri("/api/roadmap?productCode={code}", "intellij").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    assertThat(dto.appliedFilters().productCode()).isEqualTo("intellij");
                    dto.roadmapItems().forEach(item -> {
                        assertThat(item.release().code()).startsWith("IDEA");
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapWithDateRange() {
        var result = mvc.get()
                .uri("/api/roadmap?startDate={start}&endDate={end}", "2024-02-01", "2024-03-31")
                .exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.appliedFilters().startDate()).isNotNull();
                    assertThat(dto.appliedFilters().endDate()).isNotNull();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapIncludeCompleted() {
        var result = mvc.get().uri("/api/roadmap?includeCompleted={inc}", true).exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    assertThat(dto.appliedFilters().includeCompleted()).isTrue();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapExcludeCompleted() {
        var result = mvc.get().uri("/api/roadmap?includeCompleted={inc}", false).exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.appliedFilters().includeCompleted()).isFalse();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapWithGroupBy() {
        var result = mvc.get().uri("/api/roadmap?groupBy={groupBy}", "PRODUCT").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.appliedFilters().groupBy()).isNotNull();
                    assertThat(dto.appliedFilters().groupBy().toString()).isEqualTo("PRODUCT");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapFilteredByOwner() {
        var result = mvc.get().uri("/api/roadmap?owner={owner}", "siva").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    assertThat(dto.appliedFilters().owner()).isEqualTo("siva");
                    dto.roadmapItems().forEach(item -> {
                        item.features().forEach(feature -> {
                            assertThat(feature.assignedTo()).isEqualTo("siva");
                        });
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetMultiProductRoadmap() {
        var result = mvc.get().uri("/api/roadmap/multi-product").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(MultiProductRoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.products()).isNotEmpty();
                    dto.products().forEach(product -> {
                        assertThat(product.productId()).isNotNull();
                        assertThat(product.productCode()).isNotEmpty();
                        assertThat(product.productName()).isNotEmpty();
                        assertThat(product.summary()).isNotNull();
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetMultiProductRoadmapWithFilters() {
        var result = mvc.get()
                .uri(
                        "/api/roadmap/multi-product?startDate={start}&endDate={end}&includeCompleted={inc}",
                        "2024-02-01",
                        "2024-03-31",
                        true)
                .exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(MultiProductRoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.products()).isNotEmpty();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapByOwner() {
        var result =
                mvc.get().uri("/api/roadmap/by-owner?owner={owner}", "siva").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapByOwnerResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.owner()).isEqualTo("siva");
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    dto.roadmapItems().forEach(item -> {
                        item.features().forEach(feature -> {
                            assertThat(feature.assignedTo()).isEqualTo("siva");
                        });
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetRoadmapByOwnerWithFilters() {
        var result = mvc.get()
                .uri(
                        "/api/roadmap/by-owner?owner={owner}&productCode={code}&includeCompleted={inc}",
                        "siva",
                        "intellij",
                        true)
                .exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapByOwnerResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.owner()).isEqualTo("siva");
                    assertThat(dto.appliedFilters().productCode()).isEqualTo("intellij");
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapToCsv() {
        var result = mvc.get().uri("/api/roadmap/export?format={format}", "CSV").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("Roadmap_")
                .contains(".csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapToPdf() {
        var result = mvc.get().uri("/api/roadmap/export?format={format}", "PDF").exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("application/pdf");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("Roadmap_")
                .contains(".pdf");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapByProductCode() {
        var result = mvc.get()
                .uri("/api/roadmap/export?format={format}&productCode={code}", "CSV", "intellij")
                .exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();

        String csvContent = new String(result.getResponse().getContentAsByteArray());
        assertThat(csvContent).contains("intellij", "IntelliJ IDEA");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapByMultipleProductCodes() {
        var result = mvc.get()
                .uri("/api/roadmap/export?format={format}&productCodes={codes}", "CSV", "intellij,goland")
                .exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();

        String csvContent = new String(result.getResponse().getContentAsByteArray());
        assertThat(csvContent).contains("intellij", "goland");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapWithDateRange() {
        var result = mvc.get()
                .uri(
                        "/api/roadmap/export?format={format}&startDate={start}&endDate={end}",
                        "CSV",
                        "2024-02-01",
                        "2024-03-31")
                .exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapWithIncludeCompleted() {
        var result = mvc.get()
                .uri("/api/roadmap/export?format={format}&includeCompleted={inc}", "CSV", true)
                .exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldExportRoadmapWithGroupBy() {
        var result = mvc.get()
                .uri("/api/roadmap/export?format={format}&groupBy={groupBy}", "CSV", "PRODUCT")
                .exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getContentType()).contains("text/csv");
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnEmptyRoadmapForNonExistentProduct() {
        var result =
                mvc.get().uri("/api/roadmap?productCode={code}", "non-existent").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isEmpty();
                    assertThat(dto.summary().totalReleases()).isEqualTo(0);
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldVerifyProgressMetricsInRoadmap() {
        var result =
                mvc.get().uri("/api/roadmap?productCode={code}", "intellij").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    dto.roadmapItems().forEach(item -> {
                        assertThat(item.progressMetrics()).isNotNull();
                        assertThat(item.progressMetrics().totalFeatures()).isGreaterThanOrEqualTo(0);
                        assertThat(item.progressMetrics().completionPercentage())
                                .isBetween(0.0, 100.0);
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldVerifyHealthIndicatorsInRoadmap() {
        var result = mvc.get().uri("/api/roadmap").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.roadmapItems()).isNotEmpty();
                    dto.roadmapItems().forEach(item -> {
                        assertThat(item.healthIndicators()).isNotNull();
                        assertThat(item.healthIndicators().timelineAdherence()).isNotNull();
                        assertThat(item.healthIndicators().riskLevel()).isNotNull();
                        assertThat(item.healthIndicators().blockedFeatures()).isGreaterThanOrEqualTo(0);
                    });
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldVerifyRoadmapSummary() {
        var result = mvc.get().uri("/api/roadmap").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(RoadmapResponseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.summary()).isNotNull();
                    assertThat(dto.summary().totalReleases()).isGreaterThan(0);
                    assertThat(dto.summary().totalFeatures()).isGreaterThanOrEqualTo(0);
                    assertThat(dto.summary().overallCompletionPercentage()).isBetween(0.0, 100.0);
                    assertThat(dto.summary().totalReleases())
                            .isEqualTo(dto.summary().completedReleases()
                                    + dto.summary().draftReleases());
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnBadRequestForInvalidExportFormat() {
        var result =
                mvc.get().uri("/api/roadmap/export?format={format}", "INVALID").exchange();
        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
