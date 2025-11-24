package com.sivalabs.ft.features.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract tests for Release API endpoints - validates API structure, response formats,
 * and compliance with OpenAPI specifications
 */
class ReleaseApiContractTest extends AbstractIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getReleases_withoutParams_shouldReturnPagedResponse() throws Exception {
        mockMvc.perform(get("/api/releases"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.first").exists())
                .andExpect(jsonPath("$.last").exists());
    }

    @Test
    void getReleases_withProductCode_shouldReturnArrayResponse() throws Exception {
        mockMvc.perform(get("/api/releases").param("productCode", "intellij"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getReleases_withFiltersAndPagination_shouldReturnPagedResponse() throws Exception {
        mockMvc.perform(get("/api/releases")
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getReleaseByCode_existingRelease_shouldReturnReleaseDto() throws Exception {
        mockMvc.perform(get("/api/releases/{code}", "IDEA-2023.3.8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpected(jsonPath("$.code").value("IDEA-2023.3.8"))
                .andExpect(jsonPath("$.description").exists())
                .andExpected(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.plannedReleaseDate").exists())
                .andExpect(jsonPath("$.releasedAt").exists())
                .andExpect(jsonPath("$.createdBy").exists())
                .andExpected(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedBy").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getReleaseByCode_nonExistentRelease_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/releases/{code}", "NON-EXISTENT")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void createRelease_validPayload_shouldReturn201WithLocation() throws Exception {
        CreateReleasePayload payload = new CreateReleasePayload(
                "intellij", "TEST-2025.1", "Test Release", Instant.now().plusSeconds(86400 * 30));

        mockMvc.perform(post("/api/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void createRelease_invalidPayload_shouldReturn400() throws Exception {
        // Missing required productCode
        String invalidPayload =
                """
            {
                "code": "TEST-2025.1",
                "description": "Test Release"
            }
            """;

        mockMvc.perform(post("/api/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRelease_withoutAuthentication_shouldReturn401() throws Exception {
        CreateReleasePayload payload =
                new CreateReleasePayload("intellij", "TEST-2025.1", "Test Release", Instant.now());

        mockMvc.perform(post("/api/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void updateRelease_validPayload_shouldReturn200() throws Exception {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Updated description", ReleaseStatus.PLANNED, Instant.now(), null);

        mockMvc.perform(put("/api/releases/{code}", "IDEA-2023.3.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void updateRelease_withoutAuthentication_shouldReturn401() throws Exception {
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Updated description", ReleaseStatus.PLANNED, Instant.now(), null);

        mockMvc.perform(put("/api/releases/{code}", "IDEA-2023.3.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void deleteRelease_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/releases/{code}", "RIDER-2024.2.6")).andExpect(status().isOk());
    }

    @Test
    @WithMockOAuth2User(
            username = "user",
            roles = {"USER"})
    void deleteRelease_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/releases/{code}", "RIDER-2024.2.6")).andExpect(status().isForbidden());
    }

    @Test
    void deleteRelease_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/releases/{code}", "RIDER-2024.2.6")).andExpected(status().isUnauthorized());
    }

    @Test
    void getOverdueReleases_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/overdue"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].id").exists())
                .andExpect(jsonPath("$[*].code").exists())
                .andExpect(jsonPath("$[*].status").exists());
    }

    @Test
    void getAtRiskReleases_withDefaultThreshold_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/at-risk"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAtRiskReleases_withCustomThreshold_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/at-risk").param("daysThreshold", "14"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getReleasesByStatus_validStatus_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/by-status").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpected(jsonPath("$[*].status").value("DRAFT"));
    }

    @Test
    void getReleasesByStatus_invalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/releases/by-status").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReleasesByOwner_validOwner_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/by-owner").param("owner", "admin"))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getReleasesByDateRange_validDates_shouldReturnArrayOfReleases() throws Exception {
        mockMvc.perform(get("/api/releases/by-date-range")
                        .param("startDate", "2023-01-01")
                        .param("endDate", "2025-12-31"))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getReleasesByDateRange_invalidDateFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/releases/by-date-range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReleasesByDateRange_missingStartDate_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/releases/by-date-range").param("endDate", "2025-12-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReleasesByDateRange_missingEndDate_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/releases/by-date-range").param("startDate", "2023-01-01"))
                .andExpected(status().isBadRequest());
    }

    @Test
    @WithMockOAuth2User(
            username = "testuser",
            roles = {"USER"})
    void updateRelease_invalidStatusTransition_shouldReturn400() throws Exception {
        // Attempt to transition from RELEASED to DRAFT (invalid)
        UpdateReleasePayload payload =
                new UpdateReleasePayload("Invalid transition", ReleaseStatus.DRAFT, Instant.now(), null);

        mockMvc.perform(put("/api/releases/{code}", "IDEA-2023.3.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpected(status().isBadRequest());
    }

    @Test
    void getAllEndpoints_shouldReturnCorrectContentType() throws Exception {
        // Test that all endpoints return JSON content type
        String[] endpoints = {
            "/api/releases/overdue",
            "/api/releases/at-risk",
            "/api/releases/by-status?status=DRAFT",
            "/api/releases/by-owner?owner=admin",
            "/api/releases/by-date-range?startDate=2023-01-01&endDate=2025-12-31"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpected(status().isOk())
                    .andExpected(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
