package com.sivalabs.ft.features.api.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sivalabs.ft.features.domain.FavoriteFeatureService;
import com.sivalabs.ft.features.domain.FeatureDependencyService;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeatureController.class)
@WithMockUser
class FeatureDependencyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureService featureService;

    @MockBean
    private FavoriteFeatureService favoriteFeatureService;

    @MockBean
    private FeatureDependencyService featureDependencyService;

    private FeatureDependencyDto dependencyDto1;
    private FeatureDependencyDto dependencyDto2;

    @BeforeEach
    void setUp() {
        dependencyDto1 = new FeatureDependencyDto(
                1L,
                "FEAT-001",
                "Feature 1",
                "FEAT-002",
                "Feature 2",
                DependencyType.HARD,
                "Critical dependency",
                Instant.now());
        dependencyDto2 = new FeatureDependencyDto(
                2L,
                "FEAT-001",
                "Feature 1",
                "FEAT-003",
                "Feature 3",
                DependencyType.SOFT,
                "Nice to have",
                Instant.now());
    }

    @Test
    void getFeatureDependencies_WhenFeatureExists_ShouldReturnDependencies() throws Exception {
        // Given
        when(featureDependencyService.getFeatureDependencies("FEAT-001"))
                .thenReturn(List.of(dependencyDto1, dependencyDto2));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/dependencies", "FEAT-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].featureCode").value("FEAT-001"))
                .andExpect(jsonPath("$[0].dependsOnFeatureCode").value("FEAT-002"))
                .andExpect(jsonPath("$[0].dependencyType").value("HARD"))
                .andExpect(jsonPath("$[1].dependsOnFeatureCode").value("FEAT-003"))
                .andExpect(jsonPath("$[1].dependencyType").value("SOFT"));

        verify(featureDependencyService).getFeatureDependencies("FEAT-001");
    }

    @Test
    void getFeatureDependencies_WhenFeatureNotFound_ShouldReturn404() throws Exception {
        // Given
        when(featureDependencyService.getFeatureDependencies("INVALID"))
                .thenThrow(new ResourceNotFoundException("Feature not found"));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/dependencies", "INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFeatureDependents_WhenFeatureHasDependents_ShouldReturnDependents() throws Exception {
        // Given
        FeatureDependencyDto dependentDto = new FeatureDependencyDto(
                3L, "FEAT-004", "Feature 4", "FEAT-001", "Feature 1", DependencyType.OPTIONAL, null, Instant.now());
        when(featureDependencyService.getFeatureDependents("FEAT-001")).thenReturn(List.of(dependentDto));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/dependents", "FEAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].featureCode").value("FEAT-004"))
                .andExpect(jsonPath("$[0].dependsOnFeatureCode").value("FEAT-001"))
                .andExpect(jsonPath("$[0].dependencyType").value("OPTIONAL"));
    }

    @Test
    void getFeatureImpact_WithNoFilters_ShouldReturnAllImpactedFeatures() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", null, null, null))
                .thenReturn(List.of(dependencyDto1, dependencyDto2));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getFeatureImpact_WithProductFilter_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", "PROD-001", null, null))
                .thenReturn(List.of(dependencyDto1));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001").param("productCode", "PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dependsOnFeatureCode").value("FEAT-002"));
    }

    @Test
    void getFeatureImpact_WithReleaseFilter_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", null, "REL-001", null))
                .thenReturn(List.of(dependencyDto2));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001").param("releaseCode", "REL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dependsOnFeatureCode").value("FEAT-003"));
    }

    @Test
    void getFeatureImpact_WithStatusFilter_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", null, null, "IN_PROGRESS"))
                .thenReturn(List.of(dependencyDto1));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getFeatureImpact_WithAllFilters_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", "PROD-001", "REL-001", "DONE"))
                .thenReturn(List.of(dependencyDto2));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001")
                        .param("productCode", "PROD-001")
                        .param("releaseCode", "REL-001")
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getFeatureImpact_WithInvalidStatus_ShouldReturn400() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-001", null, null, "INVALID"))
                .thenThrow(new BadRequestException("Invalid feature status: INVALID"));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001").param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFeatureImpact_WhenFeatureNotFound_ShouldReturn404() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("INVALID", null, null, null))
                .thenThrow(new ResourceNotFoundException("Feature not found"));

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "INVALID")).andExpect(status().isNotFound());
    }

    @Test
    void getFeatureImpact_WithNoImpact_ShouldReturnEmptyList() throws Exception {
        // Given
        when(featureDependencyService.getFeatureImpact("FEAT-ISOLATED", null, null, null))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-ISOLATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
