package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.FeatureUsageRepository;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@WithMockOAuth2User
class FeatureUsageControllerTests extends AbstractIT {

    @Autowired
    private FeatureUsageRepository featureUsageRepository;

    @BeforeEach
    void setUp() {
        featureUsageRepository.deleteAll();
    }

    @Test
    void shouldGetUsageEvents() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-002", "PROD-001", ActionType.FEATURE_CREATED);

        // When & Then
        var result = mvc.get().uri("/api/usage/events").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetUsageEventsByUserId() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_CREATED);
        createFeatureUsage("user2", "FEAT-003", "PROD-002", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/events?userId=user1").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetUsageEventsByFeatureCode() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-001", "PROD-001", ActionType.FEATURE_UPDATED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/events?featureCode=FEAT-001").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetUsageEventsByActionType() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-002", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-003", "PROD-001", ActionType.FEATURE_CREATED);

        // When & Then
        var result =
                mvc.get().uri("/api/usage/events?actionType=FEATURE_VIEWED").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetUsageStats() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-002", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-003", "PROD-001", ActionType.FEATURE_CREATED);

        // When & Then
        var result = mvc.get().uri("/api/usage/stats").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalUsageCount")
                .asNumber()
                .isEqualTo(3);
    }

    @Test
    void shouldGetUserUsage() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_CREATED);
        createFeatureUsage("user2", "FEAT-003", "PROD-002", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/user/user1").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetFeatureUsage() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-001", "PROD-001", ActionType.FEATURE_UPDATED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/feature/FEAT-001").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetProductUsage() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-002", "PROD-001", ActionType.FEATURE_CREATED);
        createFeatureUsage("user1", "FEAT-003", "PROD-002", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/product/PROD-001").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    void shouldGetTopFeatures() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user2", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user3", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/top-features?limit=5").exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetTopUsers() {
        // Given
        createFeatureUsage("user1", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);
        createFeatureUsage("user1", "FEAT-002", "PROD-001", ActionType.FEATURE_CREATED);
        createFeatureUsage("user1", "FEAT-003", "PROD-001", ActionType.FEATURE_UPDATED);
        createFeatureUsage("user2", "FEAT-001", "PROD-001", ActionType.FEATURE_VIEWED);

        // When & Then
        var result = mvc.get().uri("/api/usage/top-users?limit=5").exchange();
        assertThat(result).hasStatusOk();
    }

    @Test
    void shouldGetUsageEventsWithPagination() {
        // Given
        for (int i = 0; i < 25; i++) {
            createFeatureUsage("user" + i, "FEAT-" + i, "PROD-001", ActionType.FEATURE_VIEWED);
        }

        // When & Then
        var result = mvc.get().uri("/api/usage/events?page=0&size=10").exchange();
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content.size()")
                .asNumber()
                .isEqualTo(10);

        assertThat(result)
                .bodyJson()
                .extractingPath("$.totalElements")
                .asNumber()
                .isEqualTo(25);
    }

    private void createFeatureUsage(String userId, String featureCode, String productCode, ActionType actionType) {
        FeatureUsage usage = new FeatureUsage();
        usage.setUserId(userId);
        usage.setFeatureCode(featureCode);
        usage.setProductCode(productCode);
        usage.setActionType(actionType);
        usage.setTimestamp(Instant.now());
        featureUsageRepository.save(usage);
    }
}
