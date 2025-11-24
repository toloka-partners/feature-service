package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

class ReleaseControllerTests extends AbstractIT {

    @Test
    void shouldGetReleasesByProductCode() {
        var result =
                mvc.get().uri("/api/releases?productCode={code}", "intellij").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetReleaseByCode() {
        String code = "IDEA-2023.3.8";
        var result = mvc.get().uri("/api/releases/{code}", code).exchange();
        assertThat(result).hasStatusOk().bodyJson().convertTo(ReleaseDto.class).satisfies(dto -> {
            assertThat(dto.code()).isEqualTo(code);
        });
    }

    @Test
    void shouldReturn404WhenReleaseNotFound() {
        var result = mvc.get().uri("/api/releases/{code}", "INVALID_CODE").exchange();
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldCreateNewRelease() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "IDEA-2025.1",
                "description": "IntelliJ IDEA 2025.1",
                "plannedReleaseDate": "2025-06-01T00:00:00Z"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateRelease() {
        var payload =
                """
            {
                "description": "Updated description",
                "status": "COMPLETED",
                "plannedReleaseDate": "2024-01-01T00:00:00Z",
                "releasedAt": "2023-12-01T10:00:00Z"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the update
        var updatedRelease =
                mvc.get().uri("/api/releases/{code}", "IDEA-2023.3.8").exchange();
        assertThat(updatedRelease)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.description()).isEqualTo("Updated description");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.COMPLETED);
                    assertThat(dto.releasedAt()).isNotNull();
                });
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void shouldDeleteRelease() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(result).hasStatusOk();

        // Verify deletion
        var getResult = mvc.get().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(getResult).hasStatus(HttpStatus.NOT_FOUND);
    }

    // New tests for advanced query endpoints

    @Test
    void shouldGetOverdueReleases() {
        var result = mvc.get().uri("/api/releases/overdue").exchange();
        assertThat(result).hasStatusOk().bodyJson().isArray();
    }

    @Test
    void shouldGetAtRiskReleases() {
        var result = mvc.get().uri("/api/releases/at-risk?daysThreshold=30").exchange();
        assertThat(result).hasStatusOk().bodyJson().isArray();
    }

    @Test
    void shouldGetReleasesByStatus() {
        var result = mvc.get().uri("/api/releases/by-status?status=DRAFT").exchange();
        assertThat(result).hasStatusOk().bodyJson().isArray();
    }

    @Test
    void shouldGetReleasesByOwner() {
        var result = mvc.get().uri("/api/releases/by-owner?owner=admin").exchange();
        assertThat(result).hasStatusOk().bodyJson().isArray();
    }

    @Test
    void shouldGetReleasesByDateRange() {
        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate=2023-01-01&endDate=2025-12-31")
                .exchange();
        assertThat(result).hasStatusOk().bodyJson().isArray();
    }

    @Test
    void shouldReturnBadRequestForInvalidDateRange() {
        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate=invalid&endDate=2024-12-31")
                .exchange();
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldGetReleasesWithPagination() {
        var result = mvc.get()
                .uri("/api/releases?page=0&size=5&sort=createdAt&direction=ASC")
                .exchange();
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetReleasesWithFilters() {
        var result = mvc.get()
                .uri("/api/releases?status=DRAFT&owner=admin&page=0&size=10")
                .exchange();
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    @Transactional
    @Rollback
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateReleaseWithPlannedDate() {
        String futureDate = Instant.now().plus(30, ChronoUnit.DAYS).toString();
        var payload = String.format(
                """
            {
                "productCode": "intellij",
                "code": "TEST-2025.1",
                "description": "Test release with planned date",
                "plannedReleaseDate": "%s"
            }
            """,
                futureDate);

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateStatusTransitionOnUpdate() {
        // First, create a release
        var createPayload =
                """
            {
                "productCode": "intellij",
                "code": "TRANSITION-TEST",
                "description": "Test status transition",
                "plannedReleaseDate": "2025-06-01T00:00:00Z"
            }
            """;

        mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
                .exchange();

        // Try to transition from DRAFT to COMPLETED (invalid)
        var invalidUpdatePayload =
                """
            {
                "description": "Test invalid transition",
                "status": "COMPLETED"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-TRANSITION-TEST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdatePayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRequireAuthenticationForCreate() {
        var payload =
                """
            {
                "productCode": "intellij",
                "code": "UNAUTH-TEST",
                "description": "Test unauthorized access"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRequireAuthenticationForUpdate() {
        var payload =
                """
            {
                "description": "Unauthorized update",
                "status": "PLANNED"
            }
            """;

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(
            username = "user",
            roles = {"USER"})
    void shouldRequireAdminRoleForDelete() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
