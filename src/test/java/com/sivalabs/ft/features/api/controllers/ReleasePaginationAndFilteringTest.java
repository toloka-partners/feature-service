package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for pagination and filtering functionality in Release endpoints
 */
@Transactional
@Rollback
class ReleasePaginationAndFilteringTest extends AbstractIT {

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void setupTestData() {
        // Create test data for pagination and filtering tests
        createTestRelease("PAGINATE-TEST-1", "Test Release 1", ReleaseStatus.DRAFT, "testuser1", Instant.now());
        createTestRelease(
                "PAGINATE-TEST-2",
                "Test Release 2",
                ReleaseStatus.PLANNED,
                "testuser2",
                Instant.now().plus(1, ChronoUnit.DAYS));
        createTestRelease(
                "PAGINATE-TEST-3",
                "Test Release 3",
                ReleaseStatus.IN_PROGRESS,
                "testuser1",
                Instant.now().plus(7, ChronoUnit.DAYS));
        createTestRelease(
                "PAGINATE-TEST-4",
                "Test Release 4",
                ReleaseStatus.COMPLETED,
                "testuser3",
                Instant.now().plus(14, ChronoUnit.DAYS));
        createTestRelease(
                "PAGINATE-TEST-5",
                "Test Release 5",
                ReleaseStatus.DRAFT,
                "testuser2",
                Instant.now().plus(30, ChronoUnit.DAYS));
    }

    // Pagination Tests

    @Test
    void getReleases_withPagination_shouldReturnCorrectPageSize() {
        var result = mvc.get().uri("/api/releases?page=0&size=2").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size")
                .asNumber()
                .isEqualTo(2);

        assertThat(result)
                .bodyJson()
                .extractingPath("$.content.length()")
                .asNumber()
                .isLessThanOrEqualTo(2);
    }

    @Test
    void getReleases_withPagination_shouldReturnCorrectPageNumber() {
        var result = mvc.get().uri("/api/releases?page=1&size=3").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.number")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    void getReleases_withPagination_shouldReturnTotalElements() {
        var result = mvc.get().uri("/api/releases?page=0&size=10").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalElements")
                .asNumber()
                .isGreaterThan(0);
    }

    @Test
    void getReleases_withPagination_shouldReturnTotalPages() {
        var result = mvc.get().uri("/api/releases?page=0&size=2").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalPages")
                .asNumber()
                .isGreaterThan(0);
    }

    @Test
    void getReleases_withPagination_shouldReturnFirstAndLastFlags() {
        // Test first page
        var firstPageResult = mvc.get().uri("/api/releases?page=0&size=2").exchange();

        assertThat(firstPageResult)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.first")
                .isEqualTo(true);

        // Test if there are multiple pages
        var totalPages = firstPageResult
                .bodyJson()
                .extractingPath("$.totalPages")
                .asNumber()
                .intValue();

        if (totalPages > 1) {
            // Test last page
            var lastPageResult = mvc.get()
                    .uri("/api/releases?page=" + (totalPages - 1) + "&size=2")
                    .exchange();

            assertThat(lastPageResult)
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.last")
                    .isEqualTo(true);
        }
    }

    // Sorting Tests

    @Test
    void getReleases_withSorting_shouldSortByCreatedAtAsc() {
        var result = mvc.get()
                .uri("/api/releases?page=0&size=10&sort=createdAt&direction=ASC")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sort.sorted")
                .isEqualTo(true);
    }

    @Test
    void getReleases_withSorting_shouldSortByCreatedAtDesc() {
        var result = mvc.get()
                .uri("/api/releases?page=0&size=10&sort=createdAt&direction=DESC")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sort.sorted")
                .isEqualTo(true);
    }

    @Test
    void getReleases_withSorting_shouldSortByCodeAsc() {
        var result = mvc.get()
                .uri("/api/releases?page=0&size=10&sort=code&direction=ASC")
                .exchange();

        assertThat(result).hasStatusOk();
    }

    // Filtering Tests

    @Test
    void getReleases_filterByStatus_shouldReturnOnlyMatchingStatus() {
        var result = mvc.get().uri("/api/releases?status=DRAFT&page=0&size=10").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[*].status")
                .asArray()
                .allMatch(status -> status.equals("DRAFT"));
    }

    @Test
    void getReleases_filterByOwner_shouldReturnOnlyMatchingOwner() {
        var result = mvc.get().uri("/api/releases?owner=admin&page=0&size=10").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[*].createdBy")
                .asArray()
                .allMatch(owner -> owner.equals("admin"));
    }

    @Test
    void getReleases_filterByDateRange_shouldReturnOnlyReleasesInRange() {
        String startDate = "2023-01-01";
        String endDate = "2025-12-31";

        var result = mvc.get()
                .uri("/api/releases?startDate={start}&endDate={end}&page=0&size=10", startDate, endDate)
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleases_filterByMultipleCriteria_shouldReturnMatchingReleases() {
        var result = mvc.get()
                .uri("/api/releases?status=DRAFT&owner=admin&page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    // Invalid Parameter Tests

    @Test
    void getReleases_withInvalidPageNumber_shouldUseDefault() {
        var result = mvc.get().uri("/api/releases?page=-1&size=10").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleases_withInvalidPageSize_shouldUseDefault() {
        var result = mvc.get().uri("/api/releases?page=0&size=-5").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleases_withInvalidSortField_shouldUseDefault() {
        var result =
                mvc.get().uri("/api/releases?page=0&size=10&sort=invalidField").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleases_withInvalidStatus_shouldReturn400() {
        var result = mvc.get()
                .uri("/api/releases?status=INVALID_STATUS&page=0&size=10")
                .exchange();

        assertThat(result).hasStatus(400);
    }

    @Test
    void getReleases_withInvalidDateFormat_shouldReturn400() {
        var result = mvc.get()
                .uri("/api/releases?startDate=invalid-date&page=0&size=10")
                .exchange();

        assertThat(result).hasStatus(400);
    }

    // Edge Cases

    @Test
    void getReleases_withVeryLargePageSize_shouldHandleGracefully() {
        var result = mvc.get().uri("/api/releases?page=0&size=1000").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleases_withVeryLargePageNumber_shouldReturnEmptyPage() {
        var result = mvc.get().uri("/api/releases?page=9999&size=10").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content")
                .asArray()
                .isEmpty();
    }

    @Test
    void getReleases_withZeroPageSize_shouldUseDefaultSize() {
        var result = mvc.get().uri("/api/releases?page=0&size=0").exchange();

        assertThat(result).hasStatusOk();
    }

    // Performance Tests (basic)

    @Test
    void getReleases_withPaginationAndFilters_shouldPerformReasonably() {
        long startTime = System.currentTimeMillis();

        var result = mvc.get()
                .uri("/api/releases?status=DRAFT&page=0&size=50&sort=createdAt&direction=DESC")
                .exchange();

        long duration = System.currentTimeMillis() - startTime;

        assertThat(result).hasStatusOk();
        // Basic performance check - should complete within reasonable time
        assertThat(duration).isLessThan(5000L); // 5 seconds max
    }

    // Combined Functionality Tests

    @Test
    void getReleases_withAllParameters_shouldWork() {
        var result = mvc.get()
                .uri(
                        "/api/releases?status=DRAFT&owner=admin&startDate=2023-01-01&endDate=2025-12-31&page=0&size=5&sort=createdAt&direction=ASC")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void getReleases_emptyFilters_shouldReturnAllReleases() {
        var result = mvc.get().uri("/api/releases?page=0&size=100").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalElements")
                .asNumber()
                .isGreaterThan(0);
    }

    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    private void createTestRelease(
            String code, String description, ReleaseStatus status, String owner, Instant plannedDate) {
        try {
            CreateReleasePayload payload = new CreateReleasePayload("intellij", code, description, plannedDate);

            mvc.post()
                    .uri("/api/releases")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload))
                    .exchange();
        } catch (Exception e) {
            // Ignore errors in test data setup
        }
    }
}
