package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.ProductUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class UsageControllerTests extends AbstractIT {

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEvent() {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "VIEWED",
                    "metadata": "{\\"source\\": \\"test\\"}"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getMvcResult().getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        // Verify the created event
        assertThat(result).bodyJson().convertTo(UsageEventDto.class).satisfies(dto -> {
            assertThat(dto.featureCode()).isEqualTo("IDEA-1");
            assertThat(dto.productCode()).isEqualTo("intellij");
            assertThat(dto.eventType().name()).isEqualTo("VIEWED");
            assertThat(dto.userId()).isEqualTo("testuser");
            assertThat(dto.metadata()).contains("source");
        });
    }

    @Test
    void shouldReturn401WhenCreatingUsageEventWithoutAuthentication() {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingFeatureCode() {
        var payload =
                """
                {
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingProductCode() {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "eventType": "VIEWED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenCreatingUsageEventWithMissingEventType() {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "intellij"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenCreatingUsageEventForNonExistentFeature() {
        var payload =
                """
                {
                    "featureCode": "INVALID-FEATURE",
                    "productCode": "intellij",
                    "eventType": "VIEWED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenCreatingUsageEventForNonExistentProduct() {
        var payload =
                """
                {
                    "featureCode": "IDEA-1",
                    "productCode": "invalid-product",
                    "eventType": "VIEWED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStats() {
        var result = mvc.get().uri("/api/usage/feature/{featureCode}", "IDEA-1").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(FeatureUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.featureCode()).isEqualTo("IDEA-1");
                    assertThat(dto.totalEvents()).isEqualTo(4L);
                    assertThat(dto.uniqueUsers()).isEqualTo(2L);
                    assertThat(dto.eventsByType()).isNotEmpty();
                    assertThat(dto.firstEventAt()).isNotNull();
                    assertThat(dto.lastEventAt()).isNotNull();
                });
    }

    @Test
    void shouldReturn401WhenGettingFeatureUsageStatsWithoutAuthentication() {
        var result = mvc.get().uri("/api/usage/feature/{featureCode}", "IDEA-1").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingFeatureUsageStatsForNonExistentFeature() {
        var result = mvc.get()
                .uri("/api/usage/feature/{featureCode}", "INVALID-FEATURE")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithEventTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/feature/{featureCode}?eventType=VIEWED", "IDEA-1")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(FeatureUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.featureCode()).isEqualTo("IDEA-1");
                    assertThat(dto.totalEvents()).isEqualTo(4L);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageStatsWithDateRangeFilter() {
        var result = mvc.get()
                .uri(
                        "/api/usage/feature/{featureCode}?startDate=2024-03-01T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(FeatureUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.featureCode()).isEqualTo("IDEA-1");
                    assertThat(dto.totalEvents()).isGreaterThan(0L);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingFeatureUsageStatsWithInvalidDateRange() {
        var result = mvc.get()
                .uri(
                        "/api/usage/feature/{featureCode}?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "IDEA-1")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStats() {
        var result =
                mvc.get().uri("/api/usage/product/{productCode}", "intellij").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.productCode()).isEqualTo("intellij");
                    assertThat(dto.totalEvents()).isEqualTo(6L);
                    assertThat(dto.uniqueUsers()).isEqualTo(2L);
                    assertThat(dto.uniqueFeatures()).isEqualTo(2L);
                    assertThat(dto.eventsByType()).isNotEmpty();
                    assertThat(dto.firstEventAt()).isNotNull();
                    assertThat(dto.lastEventAt()).isNotNull();
                });
    }

    @Test
    void shouldReturn401WhenGettingProductUsageStatsWithoutAuthentication() {
        var result =
                mvc.get().uri("/api/usage/product/{productCode}", "intellij").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn404WhenGettingProductUsageStatsForNonExistentProduct() {
        var result = mvc.get()
                .uri("/api/usage/product/{productCode}", "invalid-product")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithEventTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/product/{productCode}?eventType=VIEWED", "intellij")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.productCode()).isEqualTo("intellij");
                    assertThat(dto.totalEvents()).isGreaterThan(0L);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageStatsWithDateRangeFilter() {
        var result = mvc.get()
                .uri(
                        "/api/usage/product/{productCode}?startDate=2024-03-01T00:00:00Z&endDate=2024-03-02T23:59:59Z",
                        "intellij")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductUsageStatsDto.class)
                .satisfies(dto -> {
                    assertThat(dto.productCode()).isEqualTo("intellij");
                    assertThat(dto.totalEvents()).isGreaterThan(0L);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400WhenGettingProductUsageStatsWithInvalidDateRange() {
        var result = mvc.get()
                .uri(
                        "/api/usage/product/{productCode}?startDate=2024-03-10T00:00:00Z&endDate=2024-03-01T23:59:59Z",
                        "intellij")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEvents() {
        var result = mvc.get()
                .uri("/api/usage/feature/{featureCode}/events", "IDEA-1")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(4);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureUsageEventsWithEventTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/feature/{featureCode}/events?eventType=VIEWED", "IDEA-1")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEvents() {
        var result = mvc.get()
                .uri("/api/usage/product/{productCode}/events", "intellij")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(6);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductUsageEventsWithEventTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/product/{productCode}/events?eventType=VIEWED", "goland")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(3);
    }
}
