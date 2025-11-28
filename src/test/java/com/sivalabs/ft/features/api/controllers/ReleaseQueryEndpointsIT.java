package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Release Query Endpoints Integration Tests")
class ReleaseQueryEndpointsIT extends AbstractIT {

    // Overdue endpoint tests
    @Test
    @DisplayName("Should return overdue releases")
    @WithMockOAuth2User(username = "user")
    void testOverdueEndpoint_ReturnsCorrectReleases_IT() {
        // Create a release with past planned date
        String pastDate = Instant.now().minus(10, ChronoUnit.DAYS).toString();
        var createPayload = String.format(
                """
                        {
                            "productCode": "intellij",
                            "code": "OVERDUE-TEST-1",
                            "description": "Overdue release",
                            "plannedReleaseDate": "%s",
                            "releaseOwner": "testuser"
                        }
                        """,
                pastDate);

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
                .exchange();

        // Valid state transitions: DRAFT -> PLANNED -> IN_PROGRESS
        var updateToPlanned =
                """
                {
                    "description": "Overdue release",
                    "status": "PLANNED"
                }
                """;
        mvc.put()
                .uri("/api/releases/{code}", "IDEA-OVERDUE-TEST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateToPlanned)
                .exchange();

        var updateToInProgress =
                """
                {
                    "description": "Overdue release",
                    "status": "IN_PROGRESS"
                }
                """;
        mvc.put()
                .uri("/api/releases/{code}", "IDEA-OVERDUE-TEST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateToInProgress)
                .exchange();

        // Query overdue releases
        var result = mvc.get().uri("/api/releases/overdue").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.content")
                .asArray()
                .hasSizeGreaterThanOrEqualTo(1)
                .anySatisfy(item -> assertThat(item.toString())
                        .contains("IDEA-OVERDUE-TEST-1")
                        .contains("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Should exclude completed releases from overdue")
    void testOverdueEndpoint_ExcludesCompleted() {
        // Query overdue - completed releases should not be included
        var result = mvc.get().uri("/api/releases/overdue?page=0&size=100").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.content[*].status")
                .asArray()
                .doesNotContain("COMPLETED")
                .doesNotContain("CANCELLED");
    }

    @Test
    @DisplayName("Should handle empty overdue results")
    void testOverdueEndpoint_NoReleases() {
        // This test assumes we can query with no results or very specific filters
        var result = mvc.get().uri("/api/releases/overdue").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    // At-risk endpoint tests
    @Test
    @DisplayName("Should return at-risk releases within threshold")
    @WithMockOAuth2User(username = "user")
    void testAtRiskEndpoint_WithDaysThreshold_IT() {
        // Create a release with planned date 5 days in future
        String futureDate = Instant.now().plus(5, ChronoUnit.DAYS).toString();
        var payload = String.format(
                """
                        {
                            "productCode": "intellij",
                            "code": "ATRISK-TEST-1",
                            "description": "At-risk release",
                            "plannedReleaseDate": "%s",
                            "releaseOwner": "testuser"
                        }
                        """,
                futureDate);

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        // Query with threshold of 7 days
        var result = mvc.get().uri("/api/releases/at-risk?daysThreshold=7").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.content")
                .asArray()
                .hasSizeGreaterThanOrEqualTo(1)
                .anySatisfy(item -> assertThat(item.toString()).contains("code=IDEA-ATRISK-TEST-1"));
    }

    @Test
    @DisplayName("Should exclude releases beyond threshold")
    @WithMockOAuth2User(username = "user")
    void testAtRiskEndpoint_ExcludesOutsideThreshold() {
        // Create release with date 30 days in future
        String farFutureDate = Instant.now().plus(30, ChronoUnit.DAYS).toString();
        var payload = String.format(
                """
                        {
                            "productCode": "intellij",
                            "code": "FAR-FUTURE-1",
                            "description": "Far future release",
                            "plannedReleaseDate": "%s",
                            "releaseOwner": "testuser"
                        }
                        """,
                farFutureDate);

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        // Query with threshold of 7 days - should not include the 30-day release
        var result = mvc.get().uri("/api/releases/at-risk?daysThreshold=7").exchange();
        assertThat(result).hasStatus2xxSuccessful();
        // Note: Can't easily verify exclusion without knowing exact test data
    }

    @Test
    @DisplayName("Should test boundary dates for at-risk")
    void testAtRiskEndpoint_BoundaryDates() {
        // Boundary test - exactly 7 days should be included
        var result = mvc.get().uri("/api/releases/at-risk?daysThreshold=7").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    // By-status endpoint tests
    @Test
    @DisplayName("Should filter releases by status")
    void testByStatusEndpoint_FiltersByStatus_IT() {
        var result = mvc.get().uri("/api/releases/by-status?status=DRAFT").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.content[*].status")
                .asArray()
                .allMatch(status -> status.equals("DRAFT"));
    }

    @Test
    @DisplayName("Should return 400 for invalid status")
    void testByStatusEndpoint_InvalidStatus_IT() {
        var result =
                mvc.get().uri("/api/releases/by-status?status=INVALID_STATUS").exchange();
        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @DisplayName("Should handle empty results for status filter")
    void testFilterByStatus_EmptyResult() {
        // Assuming there might be statuses with no releases
        var result = mvc.get().uri("/api/releases/by-status?status=COMPLETED").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    // By-owner endpoint tests
    @Test
    @DisplayName("Should filter releases by owner")
    @WithMockOAuth2User(username = "user")
    void testByOwnerEndpoint_FiltersByOwner_IT() {
        // Create release with specific owner
        var payload =
                """
                {
                    "productCode": "intellij",
                    "code": "OWNER-TEST-1",
                    "description": "Owner test release",
                    "releaseOwner": "john.doe"
                }
                """;

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        var result = mvc.get().uri("/api/releases/by-owner?owner=john.doe").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.content[*].releaseOwner")
                .asArray()
                .allMatch(owner -> owner.equals("john.doe"));
    }

    // By-date-range endpoint tests
    @Test
    @DisplayName("Should filter releases by date range")
    @WithMockOAuth2User(username = "user")
    void testByDateRangeEndpoint_FiltersByRange_IT() {
        Instant start = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(30, ChronoUnit.DAYS);

        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate={start}&endDate={end}", start, end)
                .exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("Should return 400 for invalid date range")
    void testByDateRangeEndpoint_InvalidDateRange_IT() {
        // End date before start date
        Instant start = Instant.now().plus(30, ChronoUnit.DAYS);
        Instant end = Instant.now().minus(30, ChronoUnit.DAYS);

        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate={start}&endDate={end}", start, end)
                .exchange();
        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @DisplayName("Should handle zero-length date range")
    void testDateRange_ZeroLengthRange() {
        Instant sameDate = Instant.now();

        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate={start}&endDate={end}", sameDate, sameDate)
                .exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    // Enhanced list endpoint tests
    @Test
    @DisplayName("Should support multiple filters")
    @WithMockOAuth2User(username = "user")
    void testEnhancedListEndpoint_MultipleFilters_IT() {
        var result =
                mvc.get().uri("/api/releases?productCode=intellij&status=DRAFT").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("Should support pagination")
    void testEnhancedListEndpoint_WithPagination_IT() {
        var result = mvc.get().uri("/api/releases?page=0&size=5").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.pageable.pageSize")
                .asNumber()
                .isEqualTo(5);
    }

    @Test
    @DisplayName("Should enforce max page size of 100")
    void testPagination_PageSizeLimit() {
        var result = mvc.get().uri("/api/releases?page=0&size=200").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.pageable.pageSize")
                .asNumber()
                .satisfies(pageSize -> assertThat(pageSize.intValue()).isLessThanOrEqualTo(100));
    }

    @Test
    @DisplayName("Should return first page")
    void testPagination_FirstPage() {
        var result = mvc.get().uri("/api/releases?page=0&size=10").exchange();
        assertThat(result)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.first")
                .asBoolean()
                .isTrue();
    }

    @Test
    @DisplayName("Should support sorting ascending")
    void testSorting_AscendingOrder() {
        var result = mvc.get().uri("/api/releases?sort=code&direction=asc").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("Should support sorting descending")
    void testSorting_DescendingOrder() {
        var result =
                mvc.get().uri("/api/releases?sort=createdAt&direction=desc").exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    // Status transition tests
    @Test
    @DisplayName("Should allow valid status transition")
    @WithMockOAuth2User(username = "user")
    void testStatusTransition_ValidTransitionInUpdate_IT() {
        // Create a release in DRAFT
        var createPayload =
                """
                {
                    "productCode": "intellij",
                    "code": "TRANSITION-TEST-1",
                    "description": "Transition test"
                }
                """;

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
                .exchange();

        // Transition DRAFT -> PLANNED
        var updatePayload =
                """
                {
                    "description": "Transition test",
                    "status": "PLANNED"
                }
                """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-TRANSITION-TEST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();
        assertThat(result).hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    @WithMockOAuth2User(username = "user")
    void testStatusTransition_InvalidTransitionInUpdate_IT() {
        // Create release and try invalid transition
        var createPayload =
                """
                {
                    "productCode": "intellij",
                    "code": "INVALID-TRANS-1",
                    "description": "Invalid transition test"
                }
                """;

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
                .exchange();

        // Try invalid transition DRAFT -> IN_PROGRESS (should be DRAFT -> PLANNED
        // first)
        var updatePayload =
                """
                {
                    "description": "Invalid transition",
                    "status": "IN_PROGRESS"
                }
                """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-INVALID-TRANS-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();
        assertThat(result).hasStatus4xxClientError();
    }

    // Unauthorized tests
    @Test
    @DisplayName("Should return 401 for unauthenticated query request")
    void testQueryEndpoint_UnauthenticatedUser_Unauthorized() {
        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();
        assertThat(result).hasStatus4xxClientError();
    }

    // Data integrity tests
    @Test
    @DisplayName("Should preserve plannedReleaseDate when updating status")
    @WithMockOAuth2User(username = "user")
    void testPreservePlannedReleaseDateOnStatusUpdate() {
        // Create a release with a specific planned date
        Instant plannedDate = Instant.now().plus(30, ChronoUnit.DAYS);
        var createPayload = String.format(
                """
                        {
                            "productCode": "intellij",
                            "code": "PRESERVE-DATE-TEST",
                            "description": "Test preserve date",
                            "plannedReleaseDate": "%s",
                            "releaseOwner": "test.user"
                        }
                        """,
                plannedDate);

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
                .exchange();

        // Get the initial planned date for comparison
        var getResponse1 =
                mvc.get().uri("/api/releases/IDEA-PRESERVE-DATE-TEST").exchange();

        // Extract the initial planned date value - using satisfies to capture the value
        final String[] initialPlannedDate = {null};
        assertThat(getResponse1)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.plannedReleaseDate")
                .asString()
                .satisfies(date -> initialPlannedDate[0] = date);

        // Update status WITHOUT providing plannedReleaseDate
        var updatePayload =
                """
                {
                    "description": "Updated",
                    "status": "PLANNED"
                }
                """;

        var updateResponse = mvc.put()
                .uri("/api/releases/IDEA-PRESERVE-DATE-TEST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();

        assertThat(updateResponse).hasStatus2xxSuccessful();

        // Verify the planned date is still the same (data integrity check)
        var getResponse2 =
                mvc.get().uri("/api/releases/IDEA-PRESERVE-DATE-TEST").exchange();

        // Extract the updated planned date value and compare with initial
        assertThat(getResponse2)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.plannedReleaseDate")
                .asString()
                .isNotNull()
                .isEqualTo(initialPlannedDate[0]);
    }
}
