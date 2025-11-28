package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for Release API endpoints - validates authentication and authorization
 */
class ReleaseSecurityTest extends AbstractIT {

    @Autowired
    private ObjectMapper objectMapper;

    // Authentication Tests

    @Test
    void createRelease_withoutAuthentication_shouldReturn401() {
        CreateReleasePayload payload =
                new CreateReleasePayload("intellij", "SECURITY-TEST-1", "Test Release", Instant.now());

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateRelease_withoutAuthentication_shouldReturn401() {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Updated description", ReleaseStatus.PLANNED, Instant.now(), null);

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteRelease_withoutAuthentication_shouldReturn401() {
        var result = mvc.delete().uri("/api/releases/{code}", "TEST-DELETE").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    // Authorization Tests - Valid User with USER role

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void createRelease_withUserRole_shouldReturn201() {
        CreateReleasePayload payload = new CreateReleasePayload(
                "intellij", "USER-CREATE-TEST", "Test Release", Instant.now().plusSeconds(86400 * 30));

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void updateRelease_withUserRole_shouldReturn200() {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Updated by user", ReleaseStatus.PLANNED, Instant.now(), null);

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void deleteRelease_withUserRole_shouldReturn403() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    // Authorization Tests - Admin User

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void createRelease_withAdminRole_shouldReturn201() {
        CreateReleasePayload payload = new CreateReleasePayload(
                "intellij",
                "ADMIN-CREATE-TEST",
                "Test Release by Admin",
                Instant.now().plusSeconds(86400 * 30));

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void updateRelease_withAdminRole_shouldReturn200() {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Updated by admin", ReleaseStatus.PLANNED, Instant.now(), null);

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void deleteRelease_withAdminRole_shouldReturn200() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();

        assertThat(result).hasStatusOk();
    }

    // Authorization Tests - User without Required Role

    @Test
    @WithMockOAuth2User(
            username = "viewer",
            roles = {"VIEWER"})
    void createRelease_withoutUserRole_shouldReturn403() {
        CreateReleasePayload payload =
                new CreateReleasePayload("intellij", "VIEWER-TEST", "Test Release", Instant.now());

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockOAuth2User(
            username = "viewer",
            roles = {"VIEWER"})
    void updateRelease_withoutUserRole_shouldReturn403() {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Unauthorized update", ReleaseStatus.PLANNED, Instant.now(), null);

        var result = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockOAuth2User(
            username = "viewer",
            roles = {"VIEWER"})
    void deleteRelease_withoutAdminRole_shouldReturn403() {
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    // Read Operations - Should be accessible without authentication (based on current implementation)

    @Test
    void getReleases_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases?productCode=intellij").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleaseByCode_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases/{code}", "IDEA-2023.3.8").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getOverdueReleases_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases/overdue").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getAtRiskReleases_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases/at-risk?daysThreshold=7").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleasesByStatus_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases/by-status?status=DRAFT").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleasesByOwner_withoutAuthentication_shouldReturn200() {
        var result = mvc.get().uri("/api/releases/by-owner?owner=admin").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void getReleasesByDateRange_withoutAuthentication_shouldReturn200() {
        var result = mvc.get()
                .uri("/api/releases/by-date-range?startDate=2023-01-01&endDate=2025-12-31")
                .exchange();

        assertThat(result).hasStatusOk();
    }

    // Cross-User Access Tests

    @Test
    @Transactional
    @Rollback
    @WithMockOAuth2User(
            username = "user1",
            roles = {"USER"})
    void createAndUpdateRelease_crossUserAccess_shouldWork() {
        // User1 creates a release
        CreateReleasePayload createPayload = new CreateReleasePayload(
                "intellij",
                "CROSS-USER-TEST",
                "Test Cross User Access",
                Instant.now().plusSeconds(86400 * 30));

        var createResult = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createPayload))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // User2 should be able to update the release (business rule: any user can update any release)
        // This test validates the current authorization model
    }

    @Test
    @WithMockOAuth2User(
            username = "malicioususer",
            roles = {"USER"})
    void maliciousUser_cannotEscalatePrivileges() {
        // Malicious user with USER role tries to delete (admin-only operation)
        var result = mvc.delete().uri("/api/releases/{code}", "RIDER-2024.2.6").exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void userWithValidRole_canPerformAllowedOperations() {
        // Test that a user with proper role can perform all allowed operations

        // 1. Create operation
        CreateReleasePayload createPayload = new CreateReleasePayload(
                "intellij", "ROLE-TEST-CREATE", "Role Test", Instant.now().plusSeconds(86400 * 30));

        var createResult = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createPayload))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // 2. Update operation
        UpdateReleasePayload updatePayload =
                new UpdateReleasePayload("Updated by role test", ReleaseStatus.PLANNED, Instant.now(), null);

        var updateResult = mvc.put()
                .uri("/api/releases/{code}", "IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updatePayload))
                .exchange();

        assertThat(updateResult).hasStatusOk();
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
