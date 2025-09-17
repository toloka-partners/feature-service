package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateFeaturePayload;
import com.sivalabs.ft.features.api.models.UpdateFeaturePayload;
import com.sivalabs.ft.features.domain.events.FeatureEventListener;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for API idempotency with EventListener business logic log analysis.
 * Tests that business logic in EventListener is executed only once per unique eventId
 * when called through external API endpoints.
 */
@Sql("/test-data.sql")
class FeatureControllerEventListenerLogAnalysisTest extends AbstractIT {
    @DynamicPropertySource
    static void configureAdditionalProperties(DynamicPropertyRegistry registry) {
        // Override consumer group-id for this specific test
        registry.add("spring.kafka.consumer.group-id", () -> "mockmvc-test-group");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Clean up processed_events table before each test to ensure isolation
        jdbcTemplate.execute("DELETE FROM processed_events");

        // Set up the logger and appender to capture log messages from FeatureEventListener
        logger = (Logger) LoggerFactory.getLogger(FeatureEventListener.class);

        // Clear any existing appenders and logs
        if (listAppender != null) {
            listAppender.stop();
            logger.detachAppender(listAppender);
        }

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Helper method to count business logic execution logs.
     */
    private long countBusinessLogicLogs(String expectedMessage) {
        return listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> event.getFormattedMessage().startsWith("EventListener business logic: "))
                .filter(event -> event.getFormattedMessage().contains(expectedMessage))
                .count();
    }

    /**
     * Helper method to count business logic execution logs for a specific eventId.
     */
    private long countBusinessLogicLogsForEventId(String eventId) {
        return listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> event.getFormattedMessage().startsWith("EventListener business logic: "))
                .filter(event -> event.getFormattedMessage().contains("(eventId: " + eventId + ")"))
                .count();
    }

    /**
     * Helper method to count events in database using direct SQL query.
     */
    private int countEventsInDatabase(String eventId, EventType eventType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND event_type = ?",
                    Integer.class,
                    eventId,
                    eventType.name());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    /**
     * Helper method to count all events for a specific event type.
     */
    private int countAllEventsInDatabase(EventType eventType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_type = ?", Integer.class, eventType.name());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForDuplicateCreateRequests() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload =
                new CreateFeaturePayload(eventId, "intellij", "Feature Title", "Feature Description", null, "user1");

        // When - First request
        var result1 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatus(HttpStatus.CREATED);
        String location1 = result1.getMvcResult().getResponse().getHeader("Location");

        // Wait a bit for async event processing
        Thread.sleep(500);

        // When - Second request with same eventId (duplicate)
        var result2 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatus(HttpStatus.CREATED);
        String location2 = result2.getMvcResult().getResponse().getHeader("Location");

        // Wait a bit for async event processing
        Thread.sleep(500);

        // Then - API should return same result (idempotent)
        assertThat(location1).isEqualTo(location2);

        // Verify EventListener business logic was executed only once through log analysis
        // In execute-first approach, we count logs containing the specific eventId
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT)).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForDuplicateUpdateRequests() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        UpdateFeaturePayload payload = new UpdateFeaturePayload(
                eventId, "Updated Title", "Updated Description", null, "user1", FeatureStatus.IN_PROGRESS);

        // When - First update request
        var result1 = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatusOk();

        // Wait a bit for async event processing
        Thread.sleep(100);

        // When - Second update request with same eventId (duplicate)
        var result2 = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatusOk();

        // Wait a bit for async event processing
        Thread.sleep(100);

        // Then - Verify EventListener business logic was executed only once through log analysis
        // In execute-first approach, we count logs containing the specific eventId
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT)).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForConcurrentCreateRequests() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Concurrent Feature", "Concurrent Description", null, "user1");

        // When - Submit multiple sequential requests with same eventId (simulating concurrent behavior)
        var result1 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatus(HttpStatus.CREATED);
        String location1 = result1.getMvcResult().getResponse().getHeader("Location");

        var result2 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatus(HttpStatus.CREATED);
        String location2 = result2.getMvcResult().getResponse().getHeader("Location");

        var result3 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result3).hasStatus(HttpStatus.CREATED);
        String location3 = result3.getMvcResult().getResponse().getHeader("Location");

        // Wait a bit for async event processing
        Thread.sleep(200);

        // Then - All requests should return the same location (idempotent)
        assertThat(location1).isEqualTo(location2);
        assertThat(location2).isEqualTo(location3);

        // Verify EventListener business logic was executed only once through log analysis
        // In execute-first approach, we count logs containing the specific eventId
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT)).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForConcurrentUpdateRequests() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        UpdateFeaturePayload payload = new UpdateFeaturePayload(
                eventId, "Concurrent Update", "Concurrent Description", null, "user1", FeatureStatus.RELEASED);

        // When - Submit multiple sequential update requests with same eventId (simulating concurrent behavior)
        var result1 = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatusOk();

        var result2 = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatusOk();

        var result3 = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result3).hasStatusOk();

        // Wait a bit for async event processing
        Thread.sleep(200);

        // Then - Verify EventListener business logic was executed only once through log analysis
        // In execute-first approach, we count logs containing the specific eventId
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT)).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldAllowDifferentEventIdsForSameOperation() throws Exception {
        // Given
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();

        CreateFeaturePayload payload1 =
                new CreateFeaturePayload(eventId1, "intellij", "Feature 1", "Description 1", null, "user1");

        CreateFeaturePayload payload2 =
                new CreateFeaturePayload(eventId2, "intellij", "Feature 2", "Description 2", null, "user1");

        // When - Create features with different eventIds
        var result1 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload1))
                .exchange();

        assertThat(result1).hasStatus(HttpStatus.CREATED);
        String location1 = result1.getMvcResult().getResponse().getHeader("Location");

        var result2 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload2))
                .exchange();

        assertThat(result2).hasStatus(HttpStatus.CREATED);
        String location2 = result2.getMvcResult().getResponse().getHeader("Location");

        // Wait a bit for async event processing
        Thread.sleep(200);

        // Then - Different features should be created
        assertThat(location1).isNotEqualTo(location2);

        // Verify EventListener business logic was executed for both events through log analysis
        long businessLogicExecutions = countBusinessLogicLogs("Processing feature created");
        assertThat(businessLogicExecutions).isEqualTo(2);

        // Verify exactly one processed event record exists for each eventId
        assertThat(countEventsInDatabase(eventId1, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId1, EventType.EVENT)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId2, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId2, EventType.EVENT)).isEqualTo(1);

        // Verify total count of API and EVENT events is exactly 2 each
        assertThat(countAllEventsInDatabase(EventType.API)).isEqualTo(2);
        assertThat(countAllEventsInDatabase(EventType.EVENT)).isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicForMixedOperations() throws Exception {
        // Given
        String createEventId = UUID.randomUUID().toString();
        String updateEventId = UUID.randomUUID().toString();

        CreateFeaturePayload createPayload = new CreateFeaturePayload(
                createEventId, "intellij", "Mixed Feature", "Mixed Description", null, "user1");

        UpdateFeaturePayload updatePayload = new UpdateFeaturePayload(
                updateEventId,
                "Mixed Updated Title",
                "Mixed Updated Description",
                null,
                "user1",
                FeatureStatus.IN_PROGRESS);

        // When - Create feature
        var createResult = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPayload))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // When - Update feature
        var updateResult = mvc.put()
                .uri("/api/features/IDEA-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload))
                .exchange();

        assertThat(updateResult).hasStatusOk();

        // Wait a bit for async event processing
        Thread.sleep(200);

        // Then - Verify EventListener business logic was executed for both operations through log analysis
        long createExecutions = countBusinessLogicLogs("Processing feature created");
        long updateExecutions = countBusinessLogicLogs("Processing feature updated");

        assertThat(createExecutions).isEqualTo(1);
        assertThat(updateExecutions).isEqualTo(1);

        // Verify exactly one processed event record exists for each eventId and type
        assertThat(countEventsInDatabase(createEventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(createEventId, EventType.EVENT)).isEqualTo(1);
        assertThat(countEventsInDatabase(updateEventId, EventType.API)).isEqualTo(1);
        assertThat(countEventsInDatabase(updateEventId, EventType.EVENT)).isEqualTo(1);

        // Verify total count of API and EVENT events is exactly 2 each
        assertThat(countAllEventsInDatabase(EventType.API)).isEqualTo(2);
        assertThat(countAllEventsInDatabase(EventType.EVENT)).isEqualTo(2);
    }
}
