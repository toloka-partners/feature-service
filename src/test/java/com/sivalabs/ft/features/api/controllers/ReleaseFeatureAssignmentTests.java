package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ReleaseFeatureAssignmentTests extends AbstractIT {

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldAssignFeatureToRelease() {
        var payload =
                """
            {
                "featureCode": "IDEA-130",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "john.doe",
                "notes": "Assign to upcoming release"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        // Verify the assignment
        var features = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        assertThat(features)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[?(@.code == 'IDEA-130')]")
                .asArray()
                .hasSize(1);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturn404WhenAssigningNonExistentFeature() {
        var payload =
                """
            {
                "featureCode": "INVALID-999",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "john.doe",
                "notes": "Invalid feature"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldGetReleaseFeaturesWithoutFilters() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateFeaturePlanningDetails() {
        // First assign a feature
        var assignPayload =
                """
            {
                "featureCode": "IDEA-131",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "jane.smith",
                "notes": "Initial assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Now update planning details
        var updatePayload =
                """
            {
                "plannedCompletionDate": "2025-01-15T23:59:59Z",
                "planningStatus": "IN_PROGRESS",
                "featureOwner": "john.doe",
                "notes": "Updated planning"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-131")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReturn400ForInvalidStatusTransition() {
        // First assign a feature with NOT_STARTED status
        var assignPayload =
                """
            {
                "featureCode": "IDEA-132",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "jane.smith",
                "notes": "Initial assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Try to transition directly from NOT_STARTED to DONE (invalid)
        var updatePayload =
                """
            {
                "planningStatus": "DONE"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-132")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldMoveFeatureBetweenReleases() {
        // First assign a feature to a release
        var assignPayload =
                """
            {
                "featureCode": "IDEA-133",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "john.doe",
                "notes": "Initial assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Move to another release
        var movePayload =
                """
            {
                "rationale": "Changed priorities for next release"
            }
            """;

        var result = mvc.post()
                .uri(
                        "/api/releases/{targetReleaseCode}/features/{featureCode}/move",
                        "IDEA-2024.1",
                        "IDEA-133")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movePayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        // Verify the feature is now in the new release
        var features = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2024.1")
                .exchange();
        assertThat(features)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[?(@.code == 'IDEA-133')]")
                .asArray()
                .hasSize(1);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRemoveFeatureFromRelease() {
        // First assign a feature
        var assignPayload =
                """
            {
                "featureCode": "IDEA-134",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "jane.smith",
                "notes": "Initial assignment"
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
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "IDEA-134")
                .contentType(MediaType.APPLICATION_JSON)
                .content(removePayload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldGetReleaseFeaturesFilteredByStatus() {
        var result = mvc.get()
                .uri(
                        "/api/releases/{releaseCode}/features?status={status}",
                        "IDEA-2023.3.8",
                        FeaturePlanningStatus.IN_PROGRESS)
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetReleaseFeaturesFilteredByOwner() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?owner={owner}", "IDEA-2023.3.8", "john.doe")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetOverdueFeatures() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?overdue=true", "IDEA-2023.3.8")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetBlockedFeatures() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features?blocked=true", "IDEA-2023.3.8")
                .exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForAssignment() {
        var payload =
                """
            {
                "featureCode": "IDEA-130",
                "plannedCompletionDate": "2024-12-31T23:59:59Z",
                "featureOwner": "john.doe",
                "notes": "Test"
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
    void shouldReturn401WhenNotAuthenticatedForUpdate() {
        var payload =
                """
            {
                "planningStatus": "IN_PROGRESS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-130")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForMove() {
        var payload =
                """
            {
                "rationale": "Test"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{targetReleaseCode}/features/{featureCode}/move", "IDEA-2024.1", "IDEA-130")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForRemove() {
        var payload =
                """
            {
                "rationale": "Test"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "IDEA-130")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
