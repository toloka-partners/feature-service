package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ReleaseFeaturePlanningIT extends AbstractIT {

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldAssignFeatureToRelease() {
        var payload =
                """
            {
                "featureCode": "IDEA-3",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Initial assignment"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Fetch the feature and assert
        var getResult = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        var responseBody = new String(getResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).contains("IDEA-3");
    }

    @Test
    void shouldRejectAssignFeatureToReleaseForUnauthorizedUser() {
        var payload =
                """
            {
                "featureCode": "IDEA-2",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "jane.doe",
                "notes": "Unauthorized attempt"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRejectDuplicateFeatureAssignment() {
        // First assignment should succeed
        var payload =
                """
            {
                "featureCode": "IDEA-3",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "First assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        // Second assignment should fail
        var duplicatePayload =
                """
            {
                "featureCode": "IDEA-3",
                "plannedCompletionDate": "2025-01-15T23:59:59.999Z",
                "featureOwner": "jane.doe",
                "notes": "Duplicate assignment"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicatePayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldGetFeaturesAssignedToRelease() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();
        assertThat(result.getResponse().getContentType()).isEqualTo("application/json");

        // Verify response contains specific feature data from test-data.sql
        var responseBody = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).contains("IDEA-1");
        assertThat(responseBody).contains("IDEA-2");
        assertThat(responseBody).contains("Redesign Structure Tool Window");
        assertThat(responseBody).contains("SDJ Repository Method AutoCompletion");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldUpdateFeaturePlanning() {
        // First assign a feature
        var assignPayload =
                """
            {
                "featureCode": "IDEA-4",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Initial assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Then update the planning
        var updatePayload =
                """
            {
                "plannedCompletionDate": "2024-11-30T23:59:59.999Z",
                "status": "IN_PROGRESS",
                "featureOwner": "jane.doe",
                "notes": "Updated planning details"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Fetch the feature and assert
        var getResult = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        var responseBody = new String(getResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).contains("IN_PROGRESS");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRejectInvalidStatusTransition() {
        // First assign a feature
        var assignPayload =
                """
            {
                "featureCode": "IDEA-5",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Initial assignment"
            }
            """;

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Try invalid transition from NOT_STARTED to DONE
        var invalidUpdatePayload = """
            {
                "status": "DONE"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdatePayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldAllowUpdateFeaturePlanningForAnyAuthenticatedUser() {
        var updatePayload = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Fetch the feature and assert
        var getResult = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        var responseBody = new String(getResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).contains("IN_PROGRESS");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldMoveFeatureBetweenReleases() {
        // First assign a feature to a release
        var assignPayload =
                """
            {
                "featureCode": "IDEA-6",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
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
                "targetReleaseCode": "RIDER-2024.2.6",
                "rationale": "Feature scope changed"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{targetReleaseCode}/features/{featureCode}/move", "RIDER-2024.2.6", "IDEA-6")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movePayload)
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Fetch the feature from the target release and assert
        var getResult = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "RIDER-2024.2.6")
                .exchange();
        var responseBody = new String(getResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).contains("RIDER-2024.2.6");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldRemoveFeatureFromRelease() {
        // First assign a feature
        // Use a unique feature code for isolation
        var featureCode = "IDEA-7";
        var assignPayload =
                """
            {
            "featureCode": "%s",
            "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
            "featureOwner": "john.doe",
            "notes": "Initial assignment"
            }
            """
                        .formatted(featureCode);

        mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignPayload)
                .exchange();

        // Remove from release
        var removePayload =
                """
            {
            "rationale": "Feature cancelled"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", featureCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(removePayload)
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Fetch the features and assert the feature is not present
        var getResult = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .exchange();
        var responseBody = new String(getResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).doesNotContain(featureCode);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleFeatureNotFoundWhenUpdatingPlanning() {
        var updatePayload = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "NON_EXISTENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateRequiredFieldsInAssignFeaturePayload() {
        var invalidPayload =
                """
            {
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Missing featureCode"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleReleaseNotFoundWhenAssigningFeature() {
        var payload =
                """
            {
                "featureCode": "IDEA-1",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Assignment to non-existent release"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "NON_EXISTENT_RELEASE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleFeatureNotFoundWhenAssigningToRelease() {
        var payload =
                """
            {
                "featureCode": "NON_EXISTENT_FEATURE",
                "plannedCompletionDate": "2024-12-31T23:59:59.999Z",
                "featureOwner": "john.doe",
                "notes": "Assignment of non-existent feature"
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
    void shouldHandleReleaseNotFoundWhenMovingFeature() {
        var movePayload =
                """
            {
                "targetReleaseCode": "NON_EXISTENT_RELEASE",
                "rationale": "Moving to non-existent release"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{targetReleaseCode}/features/{featureCode}/move", "NON_EXISTENT_RELEASE", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movePayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleFeatureNotFoundWhenMoving() {
        var movePayload =
                """
            {
                "targetReleaseCode": "RIDER-2024.2.6",
                "rationale": "Moving non-existent feature"
            }
            """;

        var result = mvc.post()
                .uri(
                        "/api/releases/{targetReleaseCode}/features/{featureCode}/move",
                        "RIDER-2024.2.6",
                        "NON_EXISTENT_FEATURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movePayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleFeatureNotFoundWhenRemoving() {
        var removePayload =
                """
            {
                "rationale": "Removing non-existent feature"
            }
            """;

        var result = mvc.delete()
                .uri("/api/releases/{releaseCode}/features/{featureCode}", "IDEA-2023.3.8", "NON_EXISTENT_FEATURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(removePayload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleReleaseNotFoundWhenGettingFeatures() {
        var result = mvc.get()
                .uri("/api/releases/{releaseCode}/features", "NON_EXISTENT_RELEASE")
                .exchange();

        assertThat(result).hasStatus2xxSuccessful(); // Should return empty list, not 404
        assertThat(result.getResponse().getContentType()).isEqualTo("application/json");
        // Verify response is an empty array for non-existent release
        var responseBody = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(responseBody).isEqualTo("[]");
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateDateFormatInAssignFeaturePayload() {
        var invalidDatePayload =
                """
            {
                "featureCode": "IDEA-8",
                "plannedCompletionDate": "invalid-date",
                "featureOwner": "john.doe",
                "notes": "Invalid date format"
            }
            """;

        var result = mvc.post()
                .uri("/api/releases/{releaseCode}/features", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidDatePayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateDateFormatInUpdateFeaturePlanningPayload() {
        var invalidDatePayload =
                """
            {
                "plannedCompletionDate": "invalid-date",
                "status": "IN_PROGRESS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidDatePayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateStatusEnumInUpdateFeaturePlanningPayload() {
        var invalidStatusPayload =
                """
            {
                "status": "INVALID_STATUS"
            }
            """;

        var result = mvc.patch()
                .uri("/api/releases/{releaseCode}/features/{featureCode}/planning", "IDEA-2023.3.8", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidStatusPayload)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldValidateEmptyPayloadStructure() {
        // Test assign feature with empty payload - this is different from missing required fields
        var result = mvc.post()
                .uri("/api/releases/IDEA-2023.3.8/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }
}
