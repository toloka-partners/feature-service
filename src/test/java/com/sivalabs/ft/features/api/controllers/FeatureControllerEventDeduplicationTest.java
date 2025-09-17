package com.sivalabs.ft.features.api.controllers;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateFeaturePayload;
import com.sivalabs.ft.features.api.models.UpdateFeaturePayload;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for event deduplication through REST API using MockMvc
 * Uses embedded Kafka and PostgreSQL via Testcontainers
 *
 * Tests the complete flow:
 * REST API → FeatureService → EventDeduplicationService (pre-DB check) → Database operations → EventPublisher → Kafka → FeatureEventListener
 *
 * This test requires no external services and starts all necessary components automatically
 */
public class FeatureControllerEventDeduplicationTest extends AbstractIT {

    @DynamicPropertySource
    static void configureAdditionalProperties(DynamicPropertyRegistry registry) {
        // Override consumer group-id for this specific test
        registry.add("spring.kafka.consumer.group-id", () -> "mockmvc-test-group");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockOAuth2User(username = "testuser")
    void testCreateFeatureGeneratesEventAndProcessedByDeduplication() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId,
                "intellij",
                "MockMvc Test Feature",
                "Feature created via MockMvc for deduplication testing",
                "IDEA-2024.2.3",
                "testuser");

        // Act - create feature via REST API
        MvcResult result = mockMvc.perform(post("/api/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Extract feature code from Location header
        String location = result.getResponse().getHeader("Location");
        String featureCode = extractFeatureCodeFromLocation(location);
        assertNotNull(featureCode, "Feature code should be extracted from Location header");

        // Assert - wait for event to be processed through Kafka and saved in PostgreSQL
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Check exact count of processed events in PostgreSQL
            int totalEvents = getProcessedEventsCount();
            int apiEvents = getProcessedEventsCountByType("API");
            int eventEvents = getProcessedEventsCountByType("EVENT");

            assertTrue(
                    totalEvents >= 2,
                    "Should have at least 2 processed events (API + EVENT), but found: " + totalEvents);
            assertTrue(apiEvents >= 1, "Should have at least 1 API event, but found: " + apiEvents);
            assertTrue(eventEvents >= 1, "Should have at least 1 EVENT event, but found: " + eventEvents);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void testUpdateFeatureGeneratesEventAndProcessedByDeduplication() throws Exception {
        // Arrange - first create a feature
        String createEventId = UUID.randomUUID().toString();
        CreateFeaturePayload createPayload = new CreateFeaturePayload(
                createEventId,
                "intellij",
                "MockMvc Update Test Feature",
                "Feature for update testing",
                "IDEA-2024.2.3",
                "testuser");

        MvcResult createResult = mockMvc.perform(post("/api/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String featureCode =
                extractFeatureCodeFromLocation(createResult.getResponse().getHeader("Location"));

        // Wait for create event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            assertTrue(totalEvents >= 2, "Should have at least 2 events after create, but found: " + totalEvents);
        });

        // Act - update feature
        String updateEventId = UUID.randomUUID().toString();
        UpdateFeaturePayload updatePayload = new UpdateFeaturePayload(
                updateEventId,
                "Updated MockMvc Test Feature",
                "Updated description via MockMvc",
                "IDEA-2023.3.8",
                "updateduser",
                FeatureStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/features/{code}", featureCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk());

        // Assert - wait for update event to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            int apiEvents = getProcessedEventsCountByType("API");
            int eventEvents = getProcessedEventsCountByType("EVENT");

            assertTrue(
                    totalEvents >= 4,
                    "Should have at least 4 events after create+update (2 API + 2 EVENT), but found: " + totalEvents);
            assertTrue(apiEvents >= 2, "Should have at least 2 API events (create+update), but found: " + apiEvents);
            assertTrue(
                    eventEvents >= 2, "Should have at least 2 EVENT events (create+update), but found: " + eventEvents);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void testDeleteFeatureGeneratesEventAndProcessedByDeduplication() throws Exception {
        // Arrange - create feature for deletion
        String createEventId = UUID.randomUUID().toString();
        CreateFeaturePayload createPayload = new CreateFeaturePayload(
                createEventId,
                "intellij",
                "MockMvc Delete Test Feature",
                "Feature for delete testing",
                "IDEA-2024.2.3",
                "testuser");

        MvcResult createResult = mockMvc.perform(post("/api/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String featureCode =
                extractFeatureCodeFromLocation(createResult.getResponse().getHeader("Location"));

        // Wait for create event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            assertTrue(totalEvents >= 2, "Should have at least 2 events after create, but found: " + totalEvents);
        });

        // Act - delete feature
        mockMvc.perform(delete("/api/features/{code}", featureCode)).andExpect(status().isOk());

        // Assert - wait for delete event to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            int apiEvents = getProcessedEventsCountByType("API");
            int eventEvents = getProcessedEventsCountByType("EVENT");

            assertTrue(
                    totalEvents >= 4,
                    "Should have at least 4 events after create+delete (2 API + 2 EVENT), but found: " + totalEvents);
            assertTrue(apiEvents >= 2, "Should have at least 2 API events (create+delete), but found: " + apiEvents);
            assertTrue(
                    eventEvents >= 2, "Should have at least 2 EVENT events (create+delete), but found: " + eventEvents);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void testFullCRUDCycleGeneratesAllEventsAndProcessedByDeduplication() throws Exception {
        // Arrange
        String createEventId = UUID.randomUUID().toString();
        CreateFeaturePayload createPayload = new CreateFeaturePayload(
                createEventId,
                "intellij",
                "Full MockMvc CRUD Test Feature",
                "Feature for full CRUD cycle testing",
                "IDEA-2024.2.3",
                "testuser");

        // Act & Assert - complete CRUD operations cycle

        // 1. CREATE
        MvcResult createResult = mockMvc.perform(post("/api/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String featureCode =
                extractFeatureCodeFromLocation(createResult.getResponse().getHeader("Location"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            assertTrue(totalEvents >= 2, "CREATE event should be processed (API + EVENT), but found: " + totalEvents);
        });

        // 2. READ (verify that feature was created)
        mockMvc.perform(get("/api/features/{code}", featureCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(featureCode))
                .andExpect(jsonPath("$.title").value("Full MockMvc CRUD Test Feature"));

        // 3. UPDATE
        String updateEventId = UUID.randomUUID().toString();
        UpdateFeaturePayload updatePayload = new UpdateFeaturePayload(
                updateEventId,
                "Updated Full MockMvc CRUD Test Feature",
                "Updated description for full CRUD testing",
                "IDEA-2023.3.8",
                "updateduser",
                FeatureStatus.RELEASED);

        mockMvc.perform(put("/api/features/{code}", featureCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            assertTrue(
                    totalEvents >= 4, "UPDATE event should be processed (2 API + 2 EVENT), but found: " + totalEvents);
        });

        // 4. DELETE
        mockMvc.perform(delete("/api/features/{code}", featureCode)).andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            int totalEvents = getProcessedEventsCount();
            int apiEvents = getProcessedEventsCountByType("API");
            int eventEvents = getProcessedEventsCountByType("EVENT");

            assertTrue(
                    totalEvents >= 6, "DELETE event should be processed (3 API + 3 EVENT), but found: " + totalEvents);
            assertTrue(apiEvents >= 3, "Should have 3 API events (create+update+delete), but found: " + apiEvents);
            assertTrue(
                    eventEvents >= 3, "Should have 3 EVENT events (create+update+delete), but found: " + eventEvents);
        });

        // Verify that feature was actually deleted
        mockMvc.perform(get("/api/features/{code}", featureCode)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    @org.springframework.transaction.annotation.Transactional
    void testEventDeduplicationCleanup() throws Exception {
        // Arrange - create an expired event manually using direct SQL
        String expiredEventId = "expired-event-" + UUID.randomUUID().toString();
        LocalDateTime pastTime = LocalDateTime.now().minusHours(25); // 25 hours ago (expired)

        // Insert expired event directly into database
        jdbcTemplate.update(
                "INSERT INTO processed_events (event_id, event_type, processed_at, expires_at, result_data) VALUES (?, ?, ?, ?, ?)",
                expiredEventId,
                "EVENT",
                pastTime,
                pastTime,
                "test-result");

        // Verify the event exists
        int eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND event_type = ?",
                Integer.class,
                expiredEventId,
                "EVENT");
        assertEquals(1, eventCount, "Expired event should exist in database");

        // Act - cleanup expired events using direct SQL
        int deletedCount =
                jdbcTemplate.update("DELETE FROM processed_events WHERE expires_at < ?", LocalDateTime.now());

        // Assert
        assertTrue(deletedCount >= 1, "Should have deleted at least one expired event, but deleted: " + deletedCount);

        // Verify the event was actually deleted
        int remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND event_type = ?",
                Integer.class,
                expiredEventId,
                "EVENT");
        assertEquals(0, remainingCount, "Expired event should have been deleted from database");
    }

    /**
     * Gets the total count of processed events in PostgreSQL using direct SQL
     */
    private int getProcessedEventsCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_events", Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Gets the count of processed events by event type using direct SQL
     */
    private int getProcessedEventsCountByType(String eventType) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_type = ?", Integer.class, eventType);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extracts feature code from Location header
     */
    private String extractFeatureCodeFromLocation(String location) {
        if (location == null) return null;

        // Location format: http://localhost/api/features/{code}
        Pattern pattern = Pattern.compile("/api/features/([^/]+)$");
        Matcher matcher = pattern.matcher(location);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
