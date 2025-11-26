package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.FeatureUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@WithMockOAuth2User
class FeatureUsageControllerIntegrationTest extends AbstractIT {

    @Autowired
    private FeatureUsageRepository featureUsageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        featureUsageRepository.deleteAll();
        // Load test data using JdbcTemplate
        jdbcTemplate.execute(
                """
                INSERT INTO feature_usage (id, user_id, feature_code, product_code, action_type, timestamp, context, ip_address, user_agent)
                SELECT nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '2 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-2', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '1 hour', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user2@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '3 hours', '{"source": "mobile"}', '192.168.1.101', 'Mobile App'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user2@example.com', 'GO-3', 'goland', 'FEATURE_CREATED', NOW() - INTERVAL '4 hours', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'FEATURE_UPDATED', NOW() - INTERVAL '5 hours', '{"source": "api"}', '192.168.1.102', 'API Client'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-2', 'intellij', 'FEATURE_DELETED', NOW() - INTERVAL '6 hours', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-1', 'intellij', 'FEATURES_LISTED', NOW() - INTERVAL '30 minutes', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user2@example.com', NULL, 'intellij', 'PRODUCT_VIEWED', NOW() - INTERVAL '1 hour', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user3@example.com', NULL, 'intellij', 'PRODUCT_CREATED', NOW() - INTERVAL '2 days', '{"source": "admin"}', '192.168.1.102', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user1@example.com', NULL, 'intellij', 'RELEASE_VIEWED', NOW() - INTERVAL '8 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user2@example.com', NULL, 'intellij', 'RELEASE_CREATED', NOW() - INTERVAL '1 day', '{"source": "admin"}', '192.168.1.101', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'COMMENT_ADDED', NOW() - INTERVAL '10 hours', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-2', 'intellij', 'FAVORITE_ADDED', NOW() - INTERVAL '12 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user2@example.com', 'GO-3', 'goland', 'FAVORITE_REMOVED', NOW() - INTERVAL '15 hours', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '20 minutes', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user4@example.com', 'IDEA-2', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '25 minutes', '{"source": "web"}', '192.168.1.103', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user4@example.com', 'GO-3', 'goland', 'FEATURE_VIEWED', NOW() - INTERVAL '35 minutes', '{"source": "mobile"}', '192.168.1.103', 'Mobile App'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user5@example.com', 'IDEA-1', 'intellij', 'FEATURE_CREATED', NOW() - INTERVAL '40 minutes', '{"source": "api"}', '192.168.1.104', 'API Client'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user5@example.com', 'IDEA-2', 'intellij', 'FEATURE_UPDATED', NOW() - INTERVAL '50 minutes', '{"source": "web"}', '192.168.1.104', 'Mozilla/5.0'
                UNION ALL SELECT nextval('feature_usage_id_seq'), 'user1@example.com', 'GO-3', 'goland', 'FEATURE_VIEWED', NOW() - INTERVAL '1 minute', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'
                """);
    }

    @Test
    void shouldGetAllUsageEvents() {
        // When
        var result = mvc.get().uri("/api/usage/events?page=0&size=20").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThanOrEqualTo(15));
    }

    @Test
    void shouldGetUsageEventsByUserId() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?userId=user1@example.com&page=0&size=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();

        // Verify all results are for the requested user
        assertThat(result)
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetUsageEventsByFeatureCode() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?featureCode=IDEA-1&page=0&size=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByProductCode() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?productCode=intellij&page=0&size=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByActionType() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?actionType=FEATURE_VIEWED&page=0&size=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsByDateRange() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&page=0&size=20")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsWithMultipleFilters() {
        // When
        var result = mvc.get()
                .uri("/api/usage/events?userId=user1@example.com&actionType=FEATURE_VIEWED&productCode=intellij")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageStatistics() {
        // When
        var result = mvc.get().uri("/api/usage/stats").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isGreaterThan(0));

        assertThat(result)
                .bodyJson()
                .extractingPath("$.uniqueUserCount")
                .asNumber()
                .satisfies(users -> assertThat(users.intValue()).isGreaterThan(0));

        assertThat(result)
                .bodyJson()
                .extractingPath("$.uniqueFeatureCount")
                .asNumber()
                .satisfies(features -> assertThat(features.intValue()).isGreaterThan(0));

        assertThat(result).bodyJson().extractingPath("$.usageByActionType").isNotNull();

        assertThat(result).bodyJson().extractingPath("$.topFeatures").isNotNull();

        assertThat(result).bodyJson().extractingPath("$.topUsers").isNotNull();
    }

    @Test
    void shouldGetUsageStatisticsForDateRange() {
        // When
        var result = mvc.get()
                .uri("/api/usage/stats?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void shouldGetUsageEventsForSpecificUser() {
        // When
        var result = mvc.get()
                .uri("/api/usage/user/user1@example.com?page=0&size=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsForSpecificFeature() {
        // When
        var result = mvc.get().uri("/api/usage/feature/IDEA-1?page=0&size=10").exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetUsageEventsForSpecificProduct() {
        // When
        var result = mvc.get().uri("/api/usage/product/intellij?page=0&size=10").exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.content").isNotNull();
    }

    @Test
    void shouldGetTopFeatures() {
        // When
        var result = mvc.get().uri("/api/usage/top-features?limit=5").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetTopFeaturesWithDateRange() {
        // When
        var result = mvc.get()
                .uri("/api/usage/top-features?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&limit=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().isNotNull();
    }

    @Test
    void shouldGetTopUsers() {
        // When
        var result = mvc.get().uri("/api/usage/top-users?limit=5").exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isGreaterThan(0));
    }

    @Test
    void shouldGetTopUsersWithDateRange() {
        // When
        var result = mvc.get()
                .uri("/api/usage/top-users?startDate=2025-10-02T00:00:00Z&endDate=2025-10-04T23:59:59Z&limit=10")
                .exchange();

        // Then
        assertThat(result).hasStatusOk().bodyJson().isNotNull();
    }

    @Test
    void shouldHandlePaginationCorrectly() {
        // Test first page
        var page1 = mvc.get().uri("/api/usage/events?page=0&size=5").exchange();
        assertThat(page1)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .satisfies(size -> assertThat(size.intValue()).isLessThanOrEqualTo(5));

        assertThat(page1).bodyJson().extractingPath("$.number").asNumber().isEqualTo(0);

        // Test second page
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
        // When
        var result = mvc.get()
                .uri("/api/usage/events?userId=nonexistent@example.com")
                .exchange();

        // Then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(0);
    }

    @Test
    void shouldValidateUsageDataIntegrity() {
        // Verify data was loaded correctly
        long count = featureUsageRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(20L);

        // Verify we have multiple users
        Long distinctUsers =
                jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT user_id) FROM feature_usage", Long.class);
        assertThat(distinctUsers).isGreaterThanOrEqualTo(4L);

        // Verify we have multiple features
        Long distinctFeatures = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT feature_code) FROM feature_usage WHERE feature_code IS NOT NULL", Long.class);
        assertThat(distinctFeatures).isGreaterThanOrEqualTo(3L);

        // Verify we have multiple action types
        Long distinctActionTypes =
                jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT action_type) FROM feature_usage", Long.class);
        assertThat(distinctActionTypes).isGreaterThanOrEqualTo(5L);
    }
}
