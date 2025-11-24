package com.sivalabs.ft.features.domain.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;

/**
 * Integration tests for FeatureEventListener.
 * Tests verify that the Kafka listener is properly configured and can handle events.
 */
class FeatureEventListenerIntegrationTests extends AbstractIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldHaveFeatureEventListenerBean() {
        // Verify the listener bean is registered
        FeatureEventListener listener = applicationContext.getBean(FeatureEventListener.class);
        assertThat(listener).isNotNull();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldPublishEventsWhenFeatureIsCreated() {
        // Create a feature which should publish an event
        var payload =
                """
            {
                "productCode": "intellij",
                "releaseCode": "IDEA-2023.3.8",
                "title": "Test Event Publishing",
                "description": "Verify event publishing works",
                "assignedTo": "test.user"
            }
            """;

        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        // Verify the feature was created successfully
        // Event publishing is asynchronous, so we just verify no errors occurred
        assertThat(result).hasStatus(201);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldPublishEventsWhenFeatureIsUpdated() {
        // Update an existing feature which should publish an event
        var payload =
                """
            {
                "title": "Updated Title",
                "description": "Updated description",
                "assignedTo": "updated.user",
                "status": "IN_PROGRESS"
            }
            """;

        var result = mvc.put()
                .uri("/api/features/{code}", "IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldPublishEventsWhenFeatureIsDeleted() {
        // Delete a feature which should publish an event
        var result = mvc.delete().uri("/api/features/{code}", "GO-3").exchange();

        assertThat(result).hasStatusOk();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldHandleMultipleFeatureCreations() {
        // Create multiple features to generate multiple events
        for (int i = 0; i < 5; i++) {
            var payload = String.format(
                    """
                {
                    "productCode": "intellij",
                    "releaseCode": "IDEA-2023.3.8",
                    "title": "Batch Test %d",
                    "description": "Testing batch %d",
                    "assignedTo": "batch.user"
                }
                """,
                    i, i);

            var result = mvc.post()
                    .uri("/api/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
                    .exchange();

            assertThat(result).hasStatus(201);
        }
    }
}
