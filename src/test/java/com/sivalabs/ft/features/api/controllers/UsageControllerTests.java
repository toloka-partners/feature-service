package com.sivalabs.ft.features.api.controllers;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class UsageControllerTests extends AbstractIT {

    private static final Logger log = LoggerFactory.getLogger(UsageControllerTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEvent() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "VIEWED",
                    "metadata": "{\\"source\\": \\"test\\"}"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.productCode").value("intellij"))
                .andExpect(jsonPath("$.eventType").value("VIEWED"))
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.metadata").exists());
    }

    @Test
    void shouldReturn401WhenCreatingUsageEventWithoutAuthentication() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingFeatureCode() throws Exception {
        var payload =
                """
                {
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingProductCode() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingEventType() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenCreatingUsageEventForNonExistentFeature() throws Exception {
        var payload =
                """
                {
                    "featureCode": "INVALID-FEATURE",
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenCreatingUsageEventForNonExistentProduct() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "invalid-product",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStats() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}", "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.totalEvents").value(4))
                .andExpect(jsonPath("$.uniqueUsers").value(2))
                .andExpect(jsonPath("$.eventsByType").isNotEmpty())
                .andExpect(jsonPath("$.firstEventAt").isNotEmpty())
                .andExpect(jsonPath("$.lastEventAt").isNotEmpty());
    }

    @Test
    void shouldReturn401WhenGettingFeatureUsageStatsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}", "IDEA-1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingFeatureUsageStatsForNonExistentFeature() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}", "INVALID-FEATURE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithEventTypeFilter() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}?eventType=VIEWED", "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.totalEvents").value(2));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithDateRangeFilter() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}?startDate=2024-03-01T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.totalEvents").value(greaterThan(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingFeatureUsageStatsWithInvalidDateRange() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStats() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}", "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.productCode").value("intellij"))
                .andExpect(jsonPath("$.totalEvents").value(6))
                .andExpect(jsonPath("$.uniqueUsers").value(2))
                .andExpect(jsonPath("$.uniqueFeatures").value(2))
                .andExpect(jsonPath("$.eventsByType").isNotEmpty())
                .andExpect(jsonPath("$.firstEventAt").isNotEmpty())
                .andExpect(jsonPath("$.lastEventAt").isNotEmpty());
    }

    @Test
    void shouldReturn401WhenGettingProductUsageStatsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}", "intellij")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingProductUsageStatsForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}", "invalid-product"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithEventTypeFilter() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}?eventType=VIEWED", "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.productCode").value("intellij"))
                .andExpect(jsonPath("$.totalEvents").value(greaterThan(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithDateRangeFilter() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}?startDate=2024-03-01T00:00:00Z&endDate=2024-03-02T23:59:59Z",
                        "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.productCode").value("intellij"))
                .andExpect(jsonPath("$.totalEvents").value(greaterThan(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingProductUsageStatsWithInvalidDateRange() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "intellij"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEvents() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}/events", "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEventsWithEventTypeFilter() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}/events?eventType=VIEWED", "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEvents() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}/events", "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEventsWithEventTypeFilter() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}/events?eventType=VIEWED", "goland"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithInvalidEventTypeFormat() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "invalid-event"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithLowercaseEventType() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "viewed"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEventWithCustomEventType() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "CUSTOM_INSTALLED",
                    "metadata": "{\\"version\\": \\"2025.1\\"}"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("CUSTOM_INSTALLED"));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEventWithNullMetadata() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata").doesNotExist());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithCombinedFilters() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}?eventType=VIEWED&startDate=2024-03-01T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.totalEvents").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithCombinedFilters() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}?eventType=VIEWED&startDate=2024-03-01T00:00:00Z&endDate=2024-03-03T23:59:59Z",
                        "goland"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.productCode").value("goland"))
                .andExpect(jsonPath("$.totalEvents").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithEmptyEventType() throws Exception {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": ""
                }
                """;

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEventsWithDateRangeFilter() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}/events?startDate=2024-03-01T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEventsWithDateRangeFilter() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}/events?startDate=2024-03-03T00:00:00Z&endDate=2024-03-03T23:59:59Z",
                        "goland"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithNoEvents() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}?startDate=2020-01-01T00:00:00Z&endDate=2020-01-02T00:00:00Z",
                        "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.featureCode").value("IDEA-1"))
                .andExpect(jsonPath("$.totalEvents").value(0))
                .andExpect(jsonPath("$.uniqueUsers").value(0))
                .andExpect(jsonPath("$.eventsByType").isEmpty())
                .andExpect(jsonPath("$.firstEventAt").doesNotExist())
                .andExpect(jsonPath("$.lastEventAt").doesNotExist());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithNoEvents() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}?startDate=2020-01-01T00:00:00Z&endDate=2020-01-02T00:00:00Z",
                        "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.productCode").value("intellij"))
                .andExpect(jsonPath("$.totalEvents").value(0))
                .andExpect(jsonPath("$.uniqueUsers").value(0))
                .andExpect(jsonPath("$.uniqueFeatures").value(0))
                .andExpect(jsonPath("$.eventsByType").isEmpty())
                .andExpect(jsonPath("$.firstEventAt").doesNotExist())
                .andExpect(jsonPath("$.lastEventAt").doesNotExist());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEventsReturnsEmptyList() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}/events?startDate=2020-01-01T00:00:00Z&endDate=2020-01-02T00:00:00Z",
                        "IDEA-1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEventsReturnsEmptyList() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}/events?startDate=2020-01-01T00:00:00Z&endDate=2020-01-02T00:00:00Z",
                        "intellij"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn401WhenGettingFeatureUsageEventsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}/events", "IDEA-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenGettingProductUsageEventsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}/events", "intellij"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingFeatureUsageEventsForNonExistentFeature() throws Exception {
        mockMvc.perform(get("/api/usage/feature/{featureCode}/events", "INVALID-FEATURE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingProductUsageEventsForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/usage/product/{productCode}/events", "invalid-product"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingFeatureUsageEventsWithInvalidDateRange() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/feature/{featureCode}/events?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingProductUsageEventsWithInvalidDateRange() throws Exception {
        mockMvc.perform(get(
                        "/api/usage/product/{productCode}/events?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "intellij"))
                .andExpect(status().is4xxClientError());
    }
}
