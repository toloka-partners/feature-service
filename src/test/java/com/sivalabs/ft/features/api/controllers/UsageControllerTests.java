package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.UsageEvent;
import com.sivalabs.ft.features.domain.UsageEventRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class UsageControllerTests extends AbstractIT {
    @Autowired
    UsageEventRepository usageEventRepository;

    @BeforeEach
    void setup() {
        usageEventRepository.deleteAll();
    }

    @Test
    void postUsageEvent_shouldReturn201_forValidInput() {
        var payload =
                """
                {
                    "featureCode": "F1",
                    "productCode": "P1",
                    "userId": "user1",
                    "metadata": "{}"
                }
                """;

        var result = mvc.post()
                .uri("/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(usageEventRepository.count()).isEqualTo(1);
    }

    @Test
    void postUsageEvent_shouldReturn400_forMissingFields() {
        var payload =
                """
                {
                    "featureCode": "F1"
                }
                """;

        var result = mvc.post()
                .uri("/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void getFeatureUsage_shouldReturnEvents_forAuthorizedUser() {
        UsageEvent event = new UsageEvent();
        event.setFeatureCode("F2");
        event.setProductCode("P2");
        event.setUserId("user2");
        event.setTimestamp(Instant.now());
        usageEventRepository.save(event);

        var result = mvc.get().uri("/usage/feature/F2").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].featureCode")
                .isEqualTo("F2");
    }

    @Test
    void getFeatureUsage_shouldReturn401_forUnauthorizedUser() {
        var result = mvc.get().uri("/usage/feature/F2").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void getProductUsage_shouldReturnEvents_forAuthorizedUser() {
        UsageEvent event = new UsageEvent();
        event.setFeatureCode("F3");
        event.setProductCode("P3");
        event.setUserId("user3");
        event.setTimestamp(Instant.now());
        usageEventRepository.save(event);

        var result = mvc.get().uri("/usage/product/P3").exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].productCode")
                .isEqualTo("P3");
    }

    @Test
    @WithMockOAuth2User(
            username = "admin",
            roles = {"ADMIN"})
    void getFeatureUsage_shouldSupportDateFiltering() {
        UsageEvent event = new UsageEvent();
        event.setFeatureCode("F4");
        event.setProductCode("P4");
        event.setUserId("user4");
        event.setTimestamp(Instant.parse("2025-10-10T10:00:00Z"));
        usageEventRepository.save(event);

        var result = mvc.get()
                .uri("/usage/feature/F4?start=2025-10-10T00:00:00Z&end=2025-10-11T00:00:00Z")
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].featureCode")
                .isEqualTo("F4");
    }
}
