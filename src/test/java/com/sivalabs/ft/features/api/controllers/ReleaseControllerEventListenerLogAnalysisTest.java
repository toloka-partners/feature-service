package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.domain.events.ReleaseEventListener;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for Release API idempotency with EventListener business logic log analysis.
 * Tests that business logic in ReleaseEventListener is executed only once per unique eventId.
 */
@Sql("/test-data.sql")
class ReleaseControllerEventListenerLogAnalysisTest extends AbstractIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @BeforeEach
    void setUp() {
        // Set up the logger and appender to capture log messages from ReleaseEventListener
        logger = (Logger) LoggerFactory.getLogger(ReleaseEventListener.class);
        // Wait for ALL containers to be running
        /**
         * await().atMost(Duration.ofSeconds(10))
         * .pollInterval(Duration.ofMillis(500))
         * .until(() -> kafkaListenerEndpointRegistry.getListenerContainers().stream()
         * .allMatch(container -> container.isRunning()));
         */
        // Clear any existing appenders and logs
        if (listAppender != null) {
            listAppender.stop();
            logger.detachAppender(listAppender);
            listAppender.list.clear();
        }

        // Create fresh appender for each test
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Ensure logs are cleared
        listAppender.list.clear();
    }

    /**
     * Helper method to count business logic execution logs for a specific eventId.
     */
    private long countBusinessLogicLogsForEventId(String eventId) {
        return listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> event.getFormattedMessage().startsWith("EventListener business logic"))
                .filter(event -> event.getFormattedMessage().contains(eventId))
                .count();
    }

    /**
     * Helper method to count events in database using direct SQL query.
     */
    private int countEventsInDatabase(String eventId, String eventType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND event_type = ?",
                    Integer.class,
                    eventId,
                    eventType);
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }
    /**
     * Helper method to create a release for testing
     */
    private String createRelease(String eventId, String releaseCode, String description) throws Exception {
        String requestBody;
        if (eventId != null) {
            requestBody = String.format(
                    """
                    {
                        "eventId": "%s",
                        "productCode": "intellij",
                        "code": "%s",
                        "description": "%s"
                    }
                    """,
                    eventId, releaseCode, description);
        } else {
            requestBody = String.format(
                    """
                    {
                        "productCode": "intellij",
                        "code": "%s",
                        "description": "%s"
                    }
                    """,
                    releaseCode, description);
        }

        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getMvcResult().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }

    /**
     * Helper method to create a feature for testing
     */
    private void createFeature(String releaseCode, String title, String assignedTo) throws Exception {
        String requestBody = String.format(
                """
                {
                    "productCode": "intellij",
                    "releaseCode": "%s",
                    "title": "%s",
                    "description": "Test feature",
                    "assignedTo": "%s"
                }
                """,
                releaseCode, title, assignedTo);

        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForDuplicateCreateRequests() throws Exception {
        // Clear logs at the start of test
        listAppender.list.clear();

        // Given
        String eventId = UUID.randomUUID().toString();
        String releaseCode = "LOG-" + System.currentTimeMillis();

        // When - First create request
        String location1 = createRelease(eventId, releaseCode, "Release for Log Analysis");
        // Wait for async event processing
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(eventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // When - Second create request with same eventId (duplicate)
        String location2 = createRelease(eventId, releaseCode, "Release for Log Analysis");

        // Then - API should return same result (idempotent)
        assertThat(location1).isEqualTo(location2);

        // Verify EventListener business logic was executed only once through log analysis
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API.name())).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT.name())).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForDuplicateUpdateRequests() throws Exception {
        // Clear logs at the start of test
        listAppender.list.clear();

        // Given - Use existing release from test-data.sql
        String fullReleaseCode = "GO-2024.2.3";

        // Test update idempotency
        String eventId = UUID.randomUUID().toString();
        UpdateReleasePayload payload =
                new UpdateReleasePayload(eventId, "Updated via Log Analysis Test", ReleaseStatus.DRAFT, null);

        // When - First update request
        var result1 = mvc.put()
                .uri("/api/releases/" + fullReleaseCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatus2xxSuccessful();

        // Wait for async event processing
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(eventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // When - Second update request with same eventId (duplicate)
        var result2 = mvc.put()
                .uri("/api/releases/" + fullReleaseCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatus2xxSuccessful();

        // Then - Verify EventListener business logic was executed only once through log analysis
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist
        assertThat(countEventsInDatabase(eventId, EventType.API.name())).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT.name())).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicOnlyOnceForDuplicateDeleteRequests() throws Exception {
        // Clear logs at the start of test
        listAppender.list.clear();

        // Given - First create a release to delete
        String createEventId = UUID.randomUUID().toString();
        String releaseCode = "DEL-" + System.currentTimeMillis();
        String fullReleaseCode = createRelease(createEventId, releaseCode, "Release for Delete Test");

        // Wait for create event processing
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(createEventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // Clear logs before delete test
        listAppender.list.clear();

        // When - First delete request
        String deleteEventId = UUID.randomUUID().toString();
        var result1 = mvc.delete()
                .uri("/api/releases/{code}?eventId={eventId}", fullReleaseCode, deleteEventId)
                .exchange();

        assertThat(result1).hasStatus2xxSuccessful();

        // Wait for async event processing
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(deleteEventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // When - Second delete request with same eventId (duplicate)
        var result2 = mvc.delete()
                .uri("/api/releases/{code}?eventId={eventId}", fullReleaseCode, deleteEventId)
                .exchange();

        // The second request returns 404 because release is already deleted
        assertThat(result2).hasStatus4xxClientError();

        // Then - Verify EventListener business logic was executed only once through log analysis
        long businessLogicExecutions = countBusinessLogicLogsForEventId(deleteEventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify exactly one API and one EVENT record exist for delete operation
        assertThat(countEventsInDatabase(deleteEventId, EventType.API.name())).isEqualTo(1);
        assertThat(countEventsInDatabase(deleteEventId, EventType.EVENT.name())).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldExecuteEventListenerBusinessLogicForReleaseStatusChange() throws Exception {
        // Clear logs at the start of test
        listAppender.list.clear();

        // Given - Create a new release with features for cascade testing
        String createEventId = UUID.randomUUID().toString();
        String releaseCode = "CASCADE-" + System.currentTimeMillis();
        String fullReleaseCode = createRelease(createEventId, releaseCode, "Release for Cascade Test");

        // Wait for create to complete
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(createEventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // Create features for this release
        createFeature(fullReleaseCode, "Feature 1 for Cascade", "user1");
        createFeature(fullReleaseCode, "Feature 2 for Cascade", "user2");

        // Clear logs before the main test
        listAppender.list.clear();

        // Now update release to RELEASED to trigger cascade notifications
        String eventId = UUID.randomUUID().toString();
        UpdateReleasePayload payload =
                new UpdateReleasePayload(eventId, "Release Ready for Production", ReleaseStatus.RELEASED, null);

        // When - Update release to RELEASED status
        var result = mvc.put()
                .uri("/api/releases/" + fullReleaseCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Wait for async event processing
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countEventsInDatabase(eventId, EventType.EVENT.name()))
                        .isEqualTo(1));

        // Then - Verify EventListener business logic was executed through log analysis
        long businessLogicExecutions = countBusinessLogicLogsForEventId(eventId);
        assertThat(businessLogicExecutions).isEqualTo(1);

        // Verify cascade notification logs exist
        long cascadeLogs = listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .filter(event -> event.getFormattedMessage().contains("Processing cascade notifications"))
                .count();
        assertThat(cascadeLogs).isGreaterThanOrEqualTo(1);

        // Verify exactly one API and one EVENT record exist for the RELEASED update
        assertThat(countEventsInDatabase(eventId, EventType.API.name())).isEqualTo(1);
        assertThat(countEventsInDatabase(eventId, EventType.EVENT.name())).isEqualTo(1);
    }
}
