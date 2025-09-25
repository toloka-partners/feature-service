package com.sivalabs.ft.features.api.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.api.models.CreateDependencyPayload;
import com.sivalabs.ft.features.api.models.UpdateDependencyPayload;
import com.sivalabs.ft.features.domain.FeatureDependencyService;
import com.sivalabs.ft.features.domain.mappers.DependencyMapper;
import com.sivalabs.ft.features.domain.models.DependencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeatureDependencyController.class)
@DisplayName("Feature Dependency Security Tests")
class FeatureDependencySecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureDependencyService featureDependencyService;

    @MockBean
    private DependencyMapper dependencyMapper;

    @Test
    @DisplayName("Should allow unauthenticated users to view dependencies")
    void shouldAllowUnauthenticatedUsersToViewDependencies() throws Exception {
        String featureCode = "FEAT-001";

        mockMvc.perform(get("/api/features/{featureCode}/dependencies", featureCode))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject unauthenticated POST requests")
    void shouldRejectUnauthenticatedPostRequests() throws Exception {
        String featureCode = "FEAT-001";
        CreateDependencyPayload payload = new CreateDependencyPayload("FEAT-002", DependencyType.HARD, "Test notes");

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject unauthenticated PUT requests")
    void shouldRejectUnauthenticatedPutRequests() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        UpdateDependencyPayload payload = new UpdateDependencyPayload(DependencyType.SOFT, "Updated notes");

        mockMvc.perform(put(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject unauthenticated DELETE requests")
    void shouldRejectUnauthenticatedDeleteRequests() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";

        mockMvc.perform(delete(
                        "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                        featureCode,
                        dependsOnFeatureCode))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow authenticated users to create dependencies")
    void shouldAllowAuthenticatedUsersToCreateDependencies() throws Exception {
        String featureCode = "FEAT-001";
        CreateDependencyPayload payload = new CreateDependencyPayload("FEAT-002", DependencyType.HARD, "Test notes");

        Jwt jwt = createJwt("user1");

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should allow authenticated users to update dependencies")
    void shouldAllowAuthenticatedUsersToUpdateDependencies() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        UpdateDependencyPayload payload = new UpdateDependencyPayload(DependencyType.SOFT, "Updated notes");

        Jwt jwt = createJwt("user1");

        mockMvc.perform(put(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow authenticated users to delete dependencies")
    void shouldAllowAuthenticatedUsersToDeleteDependencies() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";

        Jwt jwt = createJwt("user1");

        mockMvc.perform(delete(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should allow different authenticated users to modify dependencies")
    void shouldAllowDifferentAuthenticatedUsersToModifyDependencies() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";

        // User 1 creates dependency
        CreateDependencyPayload createPayload =
                new CreateDependencyPayload(dependsOnFeatureCode, DependencyType.HARD, "Created by user1");
        Jwt jwt1 = createJwt("user1");

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated());

        // User 2 updates dependency
        UpdateDependencyPayload updatePayload = new UpdateDependencyPayload(DependencyType.SOFT, "Updated by user2");
        Jwt jwt2 = createJwt("user2");

        mockMvc.perform(put(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk());

        // User 3 deletes dependency
        Jwt jwt3 = createJwt("user3");

        mockMvc.perform(delete(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt3)))
                .andExpect(status().isNoContent());
    }

    private Jwt createJwt(String username) {
        return Jwt.withTokenValue("token-" + username)
                .header("alg", "HS256")
                .claim("preferred_username", username)
                .claim("email", username + "@example.com")
                .build();
    }
}
