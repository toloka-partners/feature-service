package com.sivalabs.ft.features.api.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.api.GlobalExceptionHandler;
import com.sivalabs.ft.features.api.models.CreateDependencyPayload;
import com.sivalabs.ft.features.api.models.UpdateDependencyPayload;
import com.sivalabs.ft.features.domain.FeatureDependencyService;
import com.sivalabs.ft.features.domain.dtos.DependencyDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.mappers.DependencyMapper;
import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeatureDependencyController.class)
@Import(GlobalExceptionHandler.class)
class FeatureDependencyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureDependencyService featureDependencyService;

    @MockBean
    private DependencyMapper dependencyMapper;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("preferred_username", "testuser")
                .claim("email", "testuser@example.com")
                .build();
    }

    @Test
    void shouldGetFeatureDependencies() throws Exception {
        String featureCode = "FEAT-001";

        FeatureDependency dependency = createDependency("FEAT-001", "FEAT-002", DependencyType.HARD);
        DependencyDto dependencyDto =
                new DependencyDto(1L, "FEAT-001", "FEAT-002", DependencyType.HARD, "Notes", Instant.now());

        when(featureDependencyService.findDependenciesByFeatureCode(featureCode))
                .thenReturn(List.of(dependency));
        when(dependencyMapper.toDto(dependency)).thenReturn(dependencyDto);

        mockMvc.perform(get("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featureCode").value("FEAT-001"))
                .andExpect(jsonPath("$[0].dependsOnFeatureCode").value("FEAT-002"))
                .andExpect(jsonPath("$[0].dependencyType").value("HARD"));

        verify(featureDependencyService).findDependenciesByFeatureCode(featureCode);
    }

    @Test
    void shouldCreateDependencyWhenAuthenticated() throws Exception {
        String featureCode = "FEAT-001";
        CreateDependencyPayload payload =
                new CreateDependencyPayload("FEAT-002", DependencyType.HARD, "This feature depends on FEAT-002");

        FeatureDependency createdDependency = createDependency("FEAT-001", "FEAT-002", DependencyType.HARD);
        DependencyDto dependencyDto = new DependencyDto(
                1L, "FEAT-001", "FEAT-002", DependencyType.HARD, "This feature depends on FEAT-002", Instant.now());

        when(featureDependencyService.createDependency(
                        eq(featureCode),
                        eq("FEAT-002"),
                        eq(DependencyType.HARD),
                        eq("This feature depends on FEAT-002"),
                        eq("testuser")))
                .thenReturn(createdDependency);
        when(dependencyMapper.toDto(createdDependency)).thenReturn(dependencyDto);

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureCode").value("FEAT-001"))
                .andExpect(jsonPath("$.dependsOnFeatureCode").value("FEAT-002"))
                .andExpect(jsonPath("$.dependencyType").value("HARD"));

        verify(featureDependencyService)
                .createDependency(
                        eq(featureCode),
                        eq("FEAT-002"),
                        eq(DependencyType.HARD),
                        eq("This feature depends on FEAT-002"),
                        eq("testuser"));
    }

    @Test
    void shouldFailToCreateDependencyWhenNotAuthenticated() throws Exception {
        String featureCode = "FEAT-001";
        CreateDependencyPayload payload = new CreateDependencyPayload("FEAT-002", DependencyType.HARD, null);

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(featureDependencyService);
    }

    @Test
    void shouldUpdateDependencyWhenAuthenticated() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        UpdateDependencyPayload payload = new UpdateDependencyPayload(DependencyType.SOFT, "Updated notes");

        FeatureDependency updatedDependency = createDependency("FEAT-001", "FEAT-002", DependencyType.SOFT);
        DependencyDto dependencyDto =
                new DependencyDto(1L, "FEAT-001", "FEAT-002", DependencyType.SOFT, "Updated notes", Instant.now());

        when(featureDependencyService.updateDependency(
                        eq(featureCode),
                        eq(dependsOnFeatureCode),
                        eq(DependencyType.SOFT),
                        eq("Updated notes"),
                        eq("testuser")))
                .thenReturn(updatedDependency);
        when(dependencyMapper.toDto(updatedDependency)).thenReturn(dependencyDto);

        mockMvc.perform(put(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependencyType").value("SOFT"))
                .andExpect(jsonPath("$.notes").value("Updated notes"));

        verify(featureDependencyService)
                .updateDependency(
                        eq(featureCode),
                        eq(dependsOnFeatureCode),
                        eq(DependencyType.SOFT),
                        eq("Updated notes"),
                        eq("testuser"));
    }

    @Test
    void shouldDeleteDependencyWhenAuthenticated() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";

        mockMvc.perform(delete(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

        verify(featureDependencyService).deleteDependency(featureCode, dependsOnFeatureCode, "testuser");
    }

    @Test
    void shouldFailToDeleteDependencyWhenNotAuthenticated() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";

        mockMvc.perform(delete(
                        "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                        featureCode,
                        dependsOnFeatureCode))
                .andExpect(status().isForbidden());

        verifyNoInteractions(featureDependencyService);
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        String featureCode = "FEAT-001";
        CreateDependencyPayload payload = new CreateDependencyPayload(null, null, null); // Invalid payload

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(featureDependencyService);
    }

    @Test
    void shouldReturnBadRequestForInvalidDependencyType() throws Exception {
        String featureCode = "FEAT-001";
        // Create JSON with invalid dependency type
        String invalidPayload =
                """
                {
                    "dependsOnFeatureCode": "FEAT-002",
                    "dependencyType": "INVALID_TYPE",
                    "notes": "Some notes"
                }
                """;

        mockMvc.perform(post("/api/features/{featureCode}/dependencies", featureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        verifyNoInteractions(featureDependencyService);
    }

    @Test
    void shouldReturnBadRequestForInvalidDependencyTypeOnUpdate() throws Exception {
        String featureCode = "FEAT-001";
        String dependsOnFeatureCode = "FEAT-002";
        // Create JSON with invalid dependency type
        String invalidPayload =
                """
                {
                    "dependencyType": "UNKNOWN_TYPE",
                    "notes": "Updated notes"
                }
                """;

        mockMvc.perform(put(
                                "/api/features/{featureCode}/dependencies/{dependsOnFeatureCode}",
                                featureCode,
                                dependsOnFeatureCode)
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        verifyNoInteractions(featureDependencyService);
    }

    private FeatureDependency createDependency(String featureCode, String dependsOnCode, DependencyType type) {
        Feature feature1 = new Feature();
        feature1.setCode(featureCode);

        Feature feature2 = new Feature();
        feature2.setCode(dependsOnCode);

        FeatureDependency dependency = new FeatureDependency();
        dependency.setId(1L);
        dependency.setFeature(feature1);
        dependency.setDependsOnFeature(feature2);
        dependency.setDependencyType(type);
        dependency.setNotes("Notes");
        dependency.setCreatedAt(Instant.now());

        return dependency;
    }
}
