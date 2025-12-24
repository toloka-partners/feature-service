package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

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
                "description": "IntelliJ IDEA 2025.1"
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
                "status": "RELEASED",
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
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.RELEASED);
                    assertThat(dto.releasedAt()).isNotNull();
                });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldDeleteRelease() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(result).hasStatusOk();

        // Verify deletion
        var getResult = mvc.get().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();
        assertThat(getResult).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetFeaturesForRelease() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetFeaturesForReleaseWithPlanningStatusFilter() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?planningStatus={status}", "IDEA-2023.3.8", "IN_PROGRESS")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetFeaturesForReleaseWithOwnerFilter() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?owner={owner}", "IDEA-2023.3.8", "john.doe")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetFeaturesForReleaseWithOverdueFilter() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?overdue={overdue}", "IDEA-2023.3.8", true)
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldGetFeaturesForReleaseWithBlockedFilter() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?blocked={blocked}", "IDEA-2023.3.8", true)
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldAssignFeatureToRelease() {
        var payload =
                """
            {
                "featureCode": "IDEA-1",
                "plannedCompletionDate": "2025-02-28",
                "featureOwner": "john.doe",
                "notes": "Initial assignment for release planning"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2024.2.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the assignment
        var feature = mvc.get().uri("/api/features/{code}", "IDEA-1").exchange();
        assertThat(feature).hasStatusOk().bodyJson().convertTo(FeatureDto.class).satisfies(dto -> {
            assertThat(dto.featureOwner()).isEqualTo("john.doe");
            assertThat(dto.notes()).isEqualTo("Initial assignment for release planning");
        });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnBadRequestWhenAssigningNonExistentFeature() {
        var payload =
                """
            {
                "featureCode": "NONEXISTENT",
                "plannedCompletionDate": "2025-02-28",
                "featureOwner": "john.doe"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateFeaturePlanning() {
        // First assign the existing feature IDEA-2 to a release
        var assignPayload =
                """
            {
                "featureCode": "IDEA-2",
                "plannedCompletionDate": "2025-02-28",
                "featureOwner": "john.doe"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2024.2.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Update planning
        var updatePayload =
                """
            {
                "plannedCompletionDate": "2025-03-15",
                "planningStatus": "IN_PROGRESS",
                "featureOwner": "jane.smith",
                "blockageReason": null
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2024.2.3", "IDEA-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the update
        var feature = mvc.get().uri("/api/features/{code}", "IDEA-2").exchange();
        assertThat(feature).hasStatusOk().bodyJson().convertTo(FeatureDto.class).satisfies(dto -> {
            assertThat(dto.featureOwner()).isEqualTo("jane.smith");
            assertThat(dto.planningStatus()).isEqualTo(FeaturePlanningStatus.IN_PROGRESS);
        });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnBadRequestForInvalidStatusTransition() {
        // Assign feature GO-3 to a release first
        var assignPayload = """
            {
                "featureCode": "GO-3"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "GO-2024.2.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Set to IN_PROGRESS
        var setInProgress =
                """
            {
                "planningStatus": "IN_PROGRESS"
            }
            """;
        mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "GO-2024.2.3", "GO-3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(setInProgress)
                .exchange();

        // Set to DONE
        var setDone2 = """
            {
                "planningStatus": "DONE"
            }
            """;
        mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "GO-2024.2.3", "GO-3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(setDone2)
                .exchange();

        // Try invalid transition from DONE to BLOCKED
        var invalidPayload =
                """
            {
                "planningStatus": "BLOCKED"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "GO-2024.2.3", "GO-3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldMoveFeatureBetweenReleases() {
        // Assign feature IDEA-1 to a release first
        var assignPayload = """
            {
                "featureCode": "IDEA-1"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2024.2.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Move to another release
        var movePayload =
                """
            {
                "rationale": "Moving to next release due to time constraints"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{targetReleaseCode}/features/{featureCode}/move", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movePayload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the move
        var feature = mvc.get().uri("/api/features/{code}", "IDEA-1").exchange();
        assertThat(feature).hasStatusOk().bodyJson().convertTo(FeatureDto.class).satisfies(dto -> {
            assertThat(dto.releaseCode()).isEqualTo("IDEA-2023.3.8");
            assertThat(dto.notes()).contains("Moved to release IDEA-2023.3.8");
            assertThat(dto.notes()).contains("Moving to next release due to time constraints");
        });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRemoveFeatureFromRelease() {
        // IDEA-2 is already assigned to release IDEA-2023.3.8 in the test data
        // First assign it explicitly for this test
        var assignPayload = """
            {
                "featureCode": "IDEA-2"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Remove from release
        var removePayload =
                """
            {
                "rationale": "Feature is no longer needed in this release"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "IDEA-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(removePayload)
                .exchange();
        assertThat(result).hasStatusOk();

        // Verify the removal
        var feature = mvc.get().uri("/api/features/{code}", "IDEA-2").exchange();
        assertThat(feature).hasStatusOk().bodyJson().convertTo(FeatureDto.class).satisfies(dto -> {
            assertThat(dto.releaseCode()).isNull();
            assertThat(dto.notes()).contains("Removed from release IDEA-2023.3.8");
            assertThat(dto.notes()).contains("Feature is no longer needed in this release");
        });
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturnBadRequestWhenRemovingFeatureNotInRelease() {
        var removePayload =
                """
            {
                "rationale": "Removing feature"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "GO-3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(removePayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn401WhenUnauthorizedUserTriesToAssignFeature() {
        var payload =
                """
            {
                "featureCode": "IDEA-1",
                "plannedCompletionDate": "2025-02-28",
                "featureOwner": "john.doe"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenUnauthorizedUserTriesToUpdatePlanning() {
        var payload = """
            {
                "planningStatus": "IN_PROGRESS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenUnauthorizedUserTriesToMoveFeature() {
        var payload = """
            {
                "rationale": "Moving feature"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{targetReleaseCode}/features/{featureCode}/move", "IDEA-2024.2.3", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenUnauthorizedUserTriesToRemoveFeature() {
        var payload = """
            {
                "rationale": "Removing feature"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
