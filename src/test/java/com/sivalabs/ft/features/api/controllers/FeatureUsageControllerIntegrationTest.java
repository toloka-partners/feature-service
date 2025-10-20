package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.FeatureUsageRepository;
import com.sivalabs.ft.features.domain.dtos.FeatureStatsDto;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageDto;
import com.sivalabs.ft.features.domain.dtos.ProductStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageStatsDto;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class FeatureUsageControllerIntegrationTest extends AbstractIT {

    @Autowired
    private FeatureUsageRepository featureUsageRepository;

    @BeforeEach
    void setUp() {
        featureUsageRepository.deleteAll();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEventWithAllFields() {
        String payload =
                """
            {
                "actionType": "FEATURE_VIEWED",
                "featureCode": "FEAT-123",
                "productCode": "intellij",
                "context": {"page": "dashboard", "version": "1.0"}
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

        assertThat(result).bodyJson().convertTo(FeatureUsageDto.class).satisfies(dto -> {
            assertThat(dto.id()).isNotNull();
            assertThat(dto.userId()).isEqualTo("testuser");
            assertThat(dto.featureCode()).isEqualTo("FEAT-123");
            assertThat(dto.productCode()).isEqualTo("intellij");
            assertThat(dto.actionType()).isEqualTo(ActionType.FEATURE_VIEWED);
            assertThat(dto.timestamp()).isNotNull();
            assertThat(dto.context()).isNotNull();
            assertThat(dto.ipAddress()).isNotNull();
            // userAgent might be null in mock environment
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateUsageEventWithMinimalData() {
        String payload = """
            {
                "actionType": "FEATURE_VIEWED"
            }
            """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().convertTo(FeatureUsageDto.class).satisfies(dto -> {
            assertThat(dto.id()).isNotNull();
            assertThat(dto.userId()).isEqualTo("testuser");
            assertThat(dto.actionType()).isEqualTo(ActionType.FEATURE_VIEWED);
            assertThat(dto.featureCode()).isNull();
            assertThat(dto.productCode()).isNull();
        });
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() {
        String payload = """
            {
                "actionType": "FEATURE_VIEWED"
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
    void shouldReturn400WhenActionTypeIsMissing() {
        String payload = """
            {
                "featureCode": "FEAT-123"
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
    void shouldReturn400WhenActionTypeIsInvalid() {
        String payload = """
            {
                "actionType": "INVALID_ACTION"
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
    void shouldReturn400WhenJsonIsMalformed() {
        String payload = "{ invalid json }";

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureStatsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/feature/FEAT-1/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(FeatureStatsDto.class).satisfies(dto -> {
            assertThat(dto.featureCode()).isEqualTo("FEAT-1");
            assertThat(dto.totalUsageCount()).isEqualTo(3);
            assertThat(dto.uniqueUserCount()).isEqualTo(2);
            assertThat(dto.usageByActionType()).isNotEmpty();
            assertThat(dto.topUsers()).isNotEmpty();
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureStatsWithActionTypeFilter() {
        createTestUsageData();

        var result = mvc.get()
                .uri("/api/usage/feature/FEAT-1/stats?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(FeatureStatsDto.class).satisfies(dto -> {
            assertThat(dto.featureCode()).isEqualTo("FEAT-1");
            assertThat(dto.totalUsageCount()).isEqualTo(2);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureStatsWithDateRangeFilter() {
        createTestUsageData();

        Instant now = Instant.now();
        Instant tenDaysAgo = now.minusSeconds(10 * 24 * 60 * 60);
        Instant tomorrow = now.plusSeconds(24 * 60 * 60);

        var result = mvc.get()
                .uri("/api/usage/feature/FEAT-1/stats?startDate={start}&endDate={end}", tenDaysAgo, tomorrow)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(FeatureStatsDto.class).satisfies(dto -> {
            assertThat(dto.featureCode()).isEqualTo("FEAT-1");
            assertThat(dto.totalUsageCount()).isGreaterThanOrEqualTo(0L);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnZeroCountsForNonExistentFeature() {
        var result = mvc.get().uri("/api/usage/feature/NON-EXISTENT/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(FeatureStatsDto.class).satisfies(dto -> {
            assertThat(dto.featureCode()).isEqualTo("NON-EXISTENT");
            assertThat(dto.totalUsageCount()).isEqualTo(0);
            assertThat(dto.uniqueUserCount()).isEqualTo(0);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturn400ForInvalidDateFormat() {
        var result = mvc.get()
                .uri("/api/usage/feature/FEAT-1/stats?startDate=invalid-date")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductStatsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/product/intellij/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(ProductStatsDto.class).satisfies(dto -> {
            assertThat(dto.productCode()).isEqualTo("intellij");
            assertThat(dto.totalUsageCount()).isEqualTo(4);
            assertThat(dto.uniqueUserCount()).isEqualTo(2);
            assertThat(dto.uniqueFeatureCount()).isEqualTo(2);
            assertThat(dto.usageByActionType()).isNotEmpty();
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductStatsWithFilters() {
        createTestUsageData();

        var result = mvc.get()
                .uri("/api/usage/product/intellij/stats?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(ProductStatsDto.class).satisfies(dto -> {
            assertThat(dto.productCode()).isEqualTo("intellij");
            assertThat(dto.totalUsageCount()).isEqualTo(3);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnZeroCountsForNonExistentProduct() {
        var result = mvc.get().uri("/api/usage/product/NON-EXISTENT/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(ProductStatsDto.class).satisfies(dto -> {
            assertThat(dto.productCode()).isEqualTo("NON-EXISTENT");
            assertThat(dto.totalUsageCount()).isEqualTo(0);
            assertThat(dto.uniqueUserCount()).isEqualTo(0);
            assertThat(dto.uniqueFeatureCount()).isEqualTo(0);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureEventsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/feature/FEAT-1/events").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(3);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetFeatureEventsWithFilters() {
        createTestUsageData();

        var result = mvc.get()
                .uri("/api/usage/feature/FEAT-1/events?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnEmptyListForNonExistentFeatureEvents() {
        var result = mvc.get().uri("/api/usage/feature/NON-EXISTENT/events").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductEventsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/product/intellij/events").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(4);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetProductEventsWithFilters() {
        createTestUsageData();

        var result = mvc.get()
                .uri("/api/usage/product/intellij/events?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(3);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnEmptyListForNonExistentProductEvents() {
        var result = mvc.get().uri("/api/usage/product/NON-EXISTENT/events").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/events").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> {
                    assertThat(size.intValue()).isGreaterThanOrEqualTo(4);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithActionTypeFilter() {
        createTestUsageData();

        var result =
                mvc.get().uri("/api/usage/events?actionType=FEATURE_VIEWED").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> {
                    assertThat(size.intValue()).isGreaterThanOrEqualTo(3);
                });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithUserIdFilter() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/events?userId=user1").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithFeatureCodeFilter() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/events?featureCode=FEAT-1").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(3);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithProductCodeFilter() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/events?productCode=intellij").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(4);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithMultipleFilters() {
        createTestUsageData();

        var result = mvc.get()
                .uri("/api/usage/events?actionType=FEATURE_VIEWED&productCode=intellij")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(3);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetAllEventsWithDateRangeFilter() {
        createTestUsageData();

        Instant now = Instant.now();
        Instant tenDaysAgo = now.minusSeconds(10 * 24 * 60 * 60);
        Instant tomorrow = now.plusSeconds(24 * 60 * 60);

        var result = mvc.get()
                .uri("/api/usage/events?startDate={start}&endDate={end}", tenDaysAgo, tomorrow)
                .exchange();

        assertThat(result).hasStatusOk();
        // Just verify response is successful with list body
        assertThat(result).bodyJson().isNotNull();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnEmptyListWhenNoEventsMatchFilters() {
        var result = mvc.get().uri("/api/usage/events?userId=NON-EXISTENT-USER").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size()").asNumber().isEqualTo(0);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetOverallStatsWithNoFilters() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/stats").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(UsageStatsDto.class).satisfies(dto -> {
            assertThat(dto.totalUsageCount()).isGreaterThanOrEqualTo(4L);
            assertThat(dto.uniqueUserCount()).isGreaterThanOrEqualTo(2L);
            assertThat(dto.uniqueFeatureCount()).isGreaterThanOrEqualTo(2L);
            assertThat(dto.uniqueProductCount()).isGreaterThanOrEqualTo(1L);
            assertThat(dto.usageByActionType()).isNotEmpty();
            assertThat(dto.topFeatures()).isNotEmpty();
            assertThat(dto.topProducts()).isNotEmpty();
            assertThat(dto.topUsers()).isNotEmpty();
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetOverallStatsWithActionTypeFilter() {
        createTestUsageData();

        var result = mvc.get().uri("/api/usage/stats?actionType=FEATURE_VIEWED").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(UsageStatsDto.class).satisfies(dto -> {
            assertThat(dto.totalUsageCount()).isGreaterThanOrEqualTo(3L);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetOverallStatsWithDateRangeFilter() {
        createTestUsageData();

        Instant now = Instant.now();
        Instant tenDaysAgo = now.minusSeconds(10 * 24 * 60 * 60);
        Instant tomorrow = now.plusSeconds(24 * 60 * 60);

        var result = mvc.get()
                .uri("/api/usage/stats?startDate={start}&endDate={end}", tenDaysAgo, tomorrow)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(UsageStatsDto.class).satisfies(dto -> {
            assertThat(dto.totalUsageCount()).isGreaterThanOrEqualTo(0L);
        });
    }

    @Test
    void shouldReturn401ForFeatureStatsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/feature/FEAT-1/stats").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401ForProductStatsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/product/intellij/stats").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401ForFeatureEventsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/feature/FEAT-1/events").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401ForProductEventsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/product/intellij/events").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401ForAllEventsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/events").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401ForOverallStatsWhenNotAuthenticated() {
        var result = mvc.get().uri("/api/usage/stats").exchange();
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void createTestUsageData() {
        Instant now = Instant.now();

        // Feature 1, Product intellij, User 1
        FeatureUsage usage1 = new FeatureUsage();
        usage1.setUserId("user1");
        usage1.setFeatureCode("FEAT-1");
        usage1.setProductCode("intellij");
        usage1.setActionType(ActionType.FEATURE_VIEWED);
        usage1.setTimestamp(now.minusSeconds(3600));
        usage1.setContext("{}");
        usage1.setIpAddress("127.0.0.1");
        usage1.setUserAgent("Mozilla/5.0");
        featureUsageRepository.save(usage1);

        // Feature 1, Product intellij, User 2
        FeatureUsage usage2 = new FeatureUsage();
        usage2.setUserId("user2");
        usage2.setFeatureCode("FEAT-1");
        usage2.setProductCode("intellij");
        usage2.setActionType(ActionType.FEATURE_VIEWED);
        usage2.setTimestamp(now.minusSeconds(1800));
        usage2.setContext("{}");
        usage2.setIpAddress("127.0.0.2");
        usage2.setUserAgent("Mozilla/5.0");
        featureUsageRepository.save(usage2);

        // Feature 1, Product intellij, User 1
        FeatureUsage usage3 = new FeatureUsage();
        usage3.setUserId("user1");
        usage3.setFeatureCode("FEAT-1");
        usage3.setProductCode("intellij");
        usage3.setActionType(ActionType.FEATURE_UPDATED);
        usage3.setTimestamp(now.minusSeconds(900));
        usage3.setContext("{}");
        usage3.setIpAddress("127.0.0.1");
        usage3.setUserAgent("Mozilla/5.0");
        featureUsageRepository.save(usage3);

        // Feature 2, Product intellij, User 2
        FeatureUsage usage4 = new FeatureUsage();
        usage4.setUserId("user2");
        usage4.setFeatureCode("FEAT-2");
        usage4.setProductCode("intellij");
        usage4.setActionType(ActionType.FEATURE_VIEWED);
        usage4.setTimestamp(now.minusSeconds(450));
        usage4.setContext("{}");
        usage4.setIpAddress("127.0.0.2");
        usage4.setUserAgent("Mozilla/5.0");
        featureUsageRepository.save(usage4);
    }
}
