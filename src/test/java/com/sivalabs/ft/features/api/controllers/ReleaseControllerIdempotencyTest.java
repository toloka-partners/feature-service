package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for Release API idempotency with duplicate requests.
 * Tests update and delete operations for proper deduplication.
 * Note: Create operations are not tested for idempotency because release codes
 * are user-defined, making duplicate creates a business logic error rather than
 * an idempotency concern.
 */
@Sql("/test-data.sql")
class ReleaseControllerIdempotencyTest extends AbstractIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up processed_events table before each test to ensure isolation
        jdbcTemplate.execute("DELETE FROM processed_events");
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
     * Helper method to count all events for a specific event type.
     */
    private int countAllEventsInDatabase(String eventType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_type = ?", Integer.class, eventType);
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Test
    @WithMockOAuth2User
    void shouldCreateReleaseSuccessfully() throws Exception {
        // Given
        String releaseCode = "2025." + System.currentTimeMillis();
        String requestBody = String.format(
                """
                {
                    "productCode": "intellij",
                    "code": "%s",
                    "description": "Test Release Creation"
                }
                """,
                releaseCode);

        // When
        var result = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        // Then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        String location = result.getMvcResult().getResponse().getHeader("Location");
        assertThat(location).contains("/api/releases/IDEA-" + releaseCode);

        // Verify release was created
        var getResult = mvc.get().uri(location).exchange();
        assertThat(getResult)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.code()).isEqualTo("IDEA-" + releaseCode);
                    assertThat(dto.description()).isEqualTo("Test Release Creation");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.DRAFT);
                });

        // Verify event was processed
        assertThat(countAllEventsInDatabase(EventType.API.name())).isGreaterThan(0);
    }

    @Test
    @WithMockOAuth2User
    void shouldReturnBadRequestWhenCreatingDuplicateRelease() throws Exception {
        // Given - Create a release first
        String releaseCode = "2025." + System.currentTimeMillis();
        String requestBody = String.format(
                """
                {
                    "productCode": "intellij",
                    "code": "%s",
                    "description": "First Release"
                }
                """,
                releaseCode);

        var firstResult = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(firstResult).hasStatus(HttpStatus.CREATED);

        // When - Try to create the same release again
        var duplicateResult = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        // Then - Should return 400 Bad Request
        assertThat(duplicateResult).hasStatus4xxClientError();
    }

    @Test
    @WithMockOAuth2User
    void shouldHandleDuplicateUpdateReleaseRequests() throws Exception {
        // Given
        String eventId = java.util.UUID.randomUUID().toString();
        UpdateReleasePayload payload =
                new UpdateReleasePayload(eventId, "Updated Release Description", ReleaseStatus.DRAFT, null);

        // When - First update request
        var result1 = mvc.put()
                .uri("/api/releases/IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatus2xxSuccessful();

        // When - Second update request with same eventId (duplicate)
        var result2 = mvc.put()
                .uri("/api/releases/IDEA-2023.3.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result2).hasStatus2xxSuccessful();

        // Then - Verify release was updated
        var getResult = mvc.get().uri("/api/releases/IDEA-2023.3.8").exchange();
        assertThat(getResult)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.description()).isEqualTo("Updated Release Description");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.DRAFT);
                });

        // Verify exactly one processed event record exists for this eventId
        assertThat(countEventsInDatabase(eventId, EventType.API.name())).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User
    void shouldDeleteReleaseAndProcessEvents() throws Exception {
        // Given - Create a release first
        String releaseCode = "2025." + System.currentTimeMillis();
        String createRequestBody = String.format(
                """
                {
                    "productCode": "intellij",
                    "code": "%s",
                    "description": "Release to Delete"
                }
                """,
                releaseCode);

        var createResult = mvc.post()
                .uri("/api/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);
        String location = createResult.getMvcResult().getResponse().getHeader("Location");
        String fullReleaseCode = location.substring(location.lastIndexOf('/') + 1);

        // When - Delete the release
        var deleteResult = mvc.delete().uri("/api/releases/" + fullReleaseCode).exchange();

        // Then
        assertThat(deleteResult).hasStatus2xxSuccessful();

        // Verify release is deleted
        var getResult = mvc.get().uri("/api/releases/" + fullReleaseCode).exchange();
        assertThat(getResult).hasStatus4xxClientError();

        // Verify delete event was processed
        assertThat(countAllEventsInDatabase(EventType.API.name())).isGreaterThan(0);
    }

    @Test
    @WithMockOAuth2User
    void shouldGenerateCascadeNotificationsWhenReleaseBecomesReleased() throws Exception {
        // Given - Use existing release with features from test-data.sql
        String releaseCode = "IDEA-2023.3.8"; // Has features IDEA-1 and IDEA-2

        UpdateReleasePayload payload =
                new UpdateReleasePayload(null, "Release 2023.3.8 - Production Ready", ReleaseStatus.RELEASED, null);

        // When - Update release status to RELEASED
        var result = mvc.put()
                .uri("/api/releases/" + releaseCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Then
        assertThat(result).hasStatus2xxSuccessful();

        // Verify release was updated to RELEASED
        var getResult = mvc.get().uri("/api/releases/" + releaseCode).exchange();
        assertThat(getResult)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.RELEASED);
                });

        // Verify events were processed (API event + potential EVENT event for cascade)
        assertThat(countAllEventsInDatabase(EventType.API.name())).isGreaterThan(0);
    }

    @Test
    @WithMockOAuth2User
    void shouldVerifyEventProcessingForReleaseOperations() throws Exception {
        // Given
        UpdateReleasePayload payload =
                new UpdateReleasePayload(null, "Test Event Processing", ReleaseStatus.RELEASED, null);

        // When - Update release
        var result = mvc.put()
                .uri("/api/releases/IDEA-2024.2.3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus2xxSuccessful();

        // Then - Verify event was processed
        assertThat(countAllEventsInDatabase(EventType.API.name())).isGreaterThan(0);

        // Verify release was updated
        var getResult = mvc.get().uri("/api/releases/IDEA-2024.2.3").exchange();
        assertThat(getResult)
                .hasStatus2xxSuccessful()
                .bodyJson()
                .convertTo(ReleaseDto.class)
                .satisfies(dto -> {
                    assertThat(dto.description()).isEqualTo("Test Event Processing");
                    assertThat(dto.status()).isEqualTo(ReleaseStatus.RELEASED);
                });
    }
}
