package com.sivalabs.ft.features.api.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sivalabs.ft.features.domain.FavoriteFeatureService;
import com.sivalabs.ft.features.domain.FeatureDependencyService;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
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
class FeatureDependencyEndpointsTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureService featureService;

    @MockBean
    private FavoriteFeatureService favoriteFeatureService;

    @MockBean
    private FeatureDependencyService featureDependencyService;

    private FeatureDto featureDto1;
    private FeatureDto featureDto2;

    @BeforeEach
    void setUp() {
        featureDto1 = new FeatureDto(
                1L,
                "FEAT-001",
                "Feature 1",
                "Description 1",
                FeatureStatus.IN_PROGRESS,
                "REL-001",
                false,
                "user1",
                "creator",
                Instant.now(),
                null,
                null);

        featureDto2 = new FeatureDto(
                2L,
                "FEAT-002",
                "Feature 2",
                "Description 2",
                FeatureStatus.NEW,
                "REL-001",
                false,
                "user2",
                "creator",
                Instant.now(),
                null,
                null);
    }

    @Test
    void getFeatureDependencies_ShouldReturnListOfFeatures() throws Exception {
        when(featureDependencyService.getFeatureDependencies("FEAT-001")).thenReturn(List.of(featureDto2));

        mockMvc.perform(get("/api/features/{featureCode}/dependencies", "FEAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("FEAT-002"))
                .andExpect(jsonPath("$[0].title").value("Feature 2"));
    }

    @Test
    void getFeatureDependencies_WhenFeatureNotFound_ShouldReturn404() throws Exception {
        when(featureDependencyService.getFeatureDependencies("INVALID"))
                .thenThrow(new ResourceNotFoundException("Feature not found"));

        mockMvc.perform(get("/api/features/{featureCode}/dependencies", "INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFeatureDependents_ShouldReturnListOfFeatures() throws Exception {
        when(featureDependencyService.getFeatureDependents("FEAT-002")).thenReturn(List.of(featureDto1));

        mockMvc.perform(get("/api/features/{featureCode}/dependents", "FEAT-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("FEAT-001"))
                .andExpect(jsonPath("$[0].title").value("Feature 1"));
    }

    @Test
    void getFeatureImpact_ShouldReturnImpactedFeatures() throws Exception {
        when(featureDependencyService.getFeatureImpact("FEAT-002", null, null, null))
                .thenReturn(List.of(featureDto1));

        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("FEAT-001"));
    }

    @Test
    void getFeatureImpact_WithFilters_ShouldApplyFilters() throws Exception {
        when(featureDependencyService.getFeatureImpact("FEAT-002", "PROD-001", "REL-001", "IN_PROGRESS"))
                .thenReturn(List.of(featureDto1));

        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-002")
                        .param("productCode", "PROD-001")
                        .param("releaseCode", "REL-001")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getFeatureImpact_WithInvalidStatus_ShouldReturn400() throws Exception {
        when(featureDependencyService.getFeatureImpact("FEAT-001", null, null, "INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid feature status: INVALID"));

        mockMvc.perform(get("/api/features/{featureCode}/impact", "FEAT-001").param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}
