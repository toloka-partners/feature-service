package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.FeatureUsageRepository;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@WithMockOAuth2User
class FeatureUsageControllerIntegrationTest extends AbstractIT {

    @Autowired
    private FeatureUsageRepository featureUsageRepository;

    @Test
    void shouldGetAllUsageEvents() {
        var result = mvc.get().uri("/api/usage/events?page=0&size=20").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThanOrEqualTo(15));
    }

    @Test
    void shouldGetUsageEventsByUserId() {
        var result = mvc.get()
                .uri("/api/usage/events?userId=user1@example.com&page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();

        assertThat(result)
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetUsageEventsByFeatureCode() {
        var result = mvc.get()
                .uri("/api/usage/events?featureCode=IDEA-1&page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByProductCode() {
        var result = mvc.get()
                .uri("/api/usage/events?productCode=intellij&page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByActionType() {
        var result = mvc.get()
                .uri("/api/usage/events?actionType=FEATURE_VIEWED&page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByDateRange() {
        var result = mvc.get()
                .uri("/api/usage/events?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&page=0&size=20")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsWithMultipleFilters() {
        var result = mvc.get()
                .uri("/api/usage/events?userId=user1@example.com&actionType=FEATURE_VIEWED&productCode=intellij")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageStatistics() {
        var result = mvc.get().uri("/api/usage/stats").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalEvents")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isGreaterThan(0));

        assertThat(result).bodyJson().extractingPath("$.uniqueUsers").asNumber().satisfies(users -> assertThat(
                        users.intValue())
                .isGreaterThan(0));

        assertThat(result)
                .bodyJson()
                .extractingPath("$.uniqueFeatures")
                .asNumber()
                .satisfies(features -> assertThat(features.intValue()).isGreaterThan(0));

        assertThat(result).bodyJson().extractingPath("$.eventsByActionType").isNotNull();

        assertThat(result).bodyJson().extractingPath("$.topFeatures").isNotNull();

        assertThat(result).bodyJson().extractingPath("$.topUsers").isNotNull();
    }

    @Test
    void shouldGetUsageStatisticsForDateRange() {
        var result = mvc.get()
                .uri("/api/usage/stats?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalEvents")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void shouldGetUsageEventsForSpecificUser() {
        var result = mvc.get()
                .uri("/api/usage/user/user1@example.com?page=0&size=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsForSpecificFeature() {
        var result = mvc.get().uri("/api/usage/feature/IDEA-1?page=0&size=10").exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsForSpecificProduct() {
        var result = mvc.get().uri("/api/usage/product/intellij?page=0&size=10").exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetTopFeatures() {
        var result = mvc.get().uri("/api/usage/top-features?limit=5").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetTopFeaturesWithDateRange() {
        var result = mvc.get()
                .uri("/api/usage/top-features?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&limit=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().isNotNull();
    }

    @Test
    void shouldGetTopUsers() {
        var result = mvc.get().uri("/api/usage/top-users?limit=5").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetTopUsersWithDateRange() {
        var result = mvc.get()
                .uri("/api/usage/top-users?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&limit=10")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().isNotNull();
    }

    @Test
    void shouldHandlePaginationCorrectly() {
        var page1 = mvc.get().uri("/api/usage/events?page=0&size=5").exchange();
        assertThat(page1)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isLessThanOrEqualTo(5));

        assertThat(page1).bodyJson().extractingPath("$.number").asNumber().isEqualTo(0);

        var page2 = mvc.get().uri("/api/usage/events?page=1&size=5").exchange();
        assertThat(page2)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.number")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyResultsForNonExistentData() {
        var result = mvc.get()
                .uri("/api/usage/events?userId=nonexistent@example.com")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    void shouldValidateUsageDataIntegrity() {
        long count = featureUsageRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(20L);

        long distinctUsers = featureUsageRepository.findAll().stream()
                .map(FeatureUsage::getUserId)
                .distinct()
                .count();
        assertThat(distinctUsers).isGreaterThanOrEqualTo(4L);

        long distinctFeatures = featureUsageRepository.findAll().stream()
                .map(FeatureUsage::getFeatureCode)
                .filter(code -> code != null)
                .distinct()
                .count();
        assertThat(distinctFeatures).isGreaterThanOrEqualTo(3L);

        long distinctActionTypes = featureUsageRepository.findAll().stream()
                .map(FeatureUsage::getActionType)
                .distinct()
                .count();
        assertThat(distinctActionTypes).isGreaterThanOrEqualTo(5L);
    }

    @Test
    void shouldCreateUsageEventViaPostEndpoint() {
        var requestBody =
                """
                {
                    "actionType": "FEATURE_VIEWED",
                    "featureCode": "FEAT-001",
                    "productCode": "PROD-001",
                    "context": {
                        "source": "web",
                        "device": "desktop"
                    }
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus(201);

        assertThat(result).bodyJson().extractingPath("$.featureCode").asString().isEqualTo("FEAT-001");

        assertThat(result).bodyJson().extractingPath("$.id").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.actionType").asString().isEqualTo("FEATURE_VIEWED");
    }

    @Test
    void shouldCreateUsageEventWithMinimalData() {
        var requestBody =
                """
                {
                    "actionType": "FEATURE_CREATED"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus(201);
        assertThat(result).bodyJson().extractingPath("$.actionType").asString().isEqualTo("FEATURE_CREATED");
    }

    @Test
    void shouldRejectPostUsageEventWithoutActionType() {
        var requestBody =
                """
                {
                    "featureCode": "FEAT-001",
                    "productCode": "PROD-001"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldRejectPostUsageEventWithInvalidActionType() {
        var requestBody =
                """
                {
                    "actionType": "INVALID_ACTION",
                    "featureCode": "FEAT-001"
                }
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldRejectPostUsageEventWithMalformedJson() {
        var requestBody =
                """
                {
                    "actionType": "FEATURE_VIEWED",
                    "featureCode": "FEAT-001"
                """;

        var result = mvc.post()
                .uri("/api/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldGetFeatureUsageEventsList() {
        var result = mvc.get().uri("/api/usage/feature/IDEA-1/events").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetFeatureUsageEventsWithActionTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/feature/IDEA-1/events?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$").isNotNull();
    }

    @Test
    void shouldGetFeatureUsageEventsWithDateRangeFilter() {
        Instant startDate = Instant.now().minusSeconds(7200);
        Instant endDate = Instant.now();

        var result = mvc.get()
                .uri("/api/usage/feature/IDEA-1/events?startDate=" + startDate.toString() + "&endDate="
                        + endDate.toString())
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetProductUsageEventsList() {
        var result = mvc.get().uri("/api/usage/product/intellij/events").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetProductUsageEventsWithActionTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/product/intellij/events?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$").isNotNull();
    }

    @Test
    void shouldGetProductUsageEventsWithDateRangeFilter() {
        Instant startDate = Instant.now().minusSeconds(7200);
        Instant endDate = Instant.now();

        var result = mvc.get()
                .uri("/api/usage/product/intellij/events?startDate=" + startDate.toString() + "&endDate="
                        + endDate.toString())
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetFeatureStatsWithActionTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/feature/IDEA-1/stats?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .satisfies(count -> assertThat(count.intValue()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void shouldGetProductStatsWithActionTypeFilter() {
        var result = mvc.get()
                .uri("/api/usage/product/intellij/stats?actionType=FEATURE_VIEWED")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .satisfies(count -> assertThat(count.intValue()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void shouldHandleInvalidDateFormatInFeatureStats() {
        var result = mvc.get()
                .uri("/api/usage/feature/FEAT-001/stats?startDate=invalid-date")
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldHandleInvalidDateFormatInProductStats() {
        var result = mvc.get()
                .uri("/api/usage/product/PROD-001/stats?endDate=invalid-date")
                .exchange();

        assertThat(result).hasStatus4xxClientError();
    }

    @Test
    void shouldReturnEmptyStatsForNonExistentFeature() {
        var result = mvc.get().uri("/api/usage/feature/NON-EXISTENT/stats").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyStatsForNonExistentProduct() {
        var result = mvc.get().uri("/api/usage/product/NON-EXISTENT/stats").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .isEqualTo(0);
    }

    private FeatureUsage createFeatureUsage(
            String userId, String featureCode, String productCode, ActionType actionType) {
        FeatureUsage usage = new FeatureUsage();
        usage.setUserId(userId);
        usage.setFeatureCode(featureCode);
        usage.setProductCode(productCode);
        usage.setActionType(actionType);
        usage.setTimestamp(Instant.now());
        return featureUsageRepository.save(usage);
    }
}
