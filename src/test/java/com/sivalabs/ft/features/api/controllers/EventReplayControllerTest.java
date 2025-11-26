package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.domain.EventStoreService;
import com.sivalabs.ft.features.domain.entities.EventStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class EventReplayControllerTest extends AbstractIT {

    @MockBean
    private EventStoreService eventStoreService;

    @Test
    void shouldGetEventsByFeature() throws Exception {
        String featureCode = "FT-001";
        List<EventStore> events = createTestEvents();
        when(eventStoreService.getEventsByFeature(featureCode)).thenReturn(events);

        var result =
                mvc.get().uri("/api/events/feature/{featureCode}", featureCode).exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    void shouldGetEventsByTimeRange() throws Exception {
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now();
        List<EventStore> events = createTestEvents();
        when(eventStoreService.getEventsByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(events);

        var result = mvc.get()
                .uri("/api/events/time-range?fromTime={fromTime}&toTime={toTime}", fromTime, toTime)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    void shouldGetEventsByFeatureAndTimeRange() throws Exception {
        String featureCode = "FT-001";
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now();
        List<EventStore> events = createTestEvents();
        when(eventStoreService.getEventsByFeatureAndTimeRange(eq(featureCode), any(Instant.class), any(Instant.class)))
                .thenReturn(events);

        var result = mvc.get()
                .uri(
                        "/api/events/feature/{featureCode}/time-range?fromTime={fromTime}&toTime={toTime}",
                        featureCode,
                        fromTime,
                        toTime)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.size()")
                .asNumber()
                .isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReplayEventsByFeature() throws Exception {
        String featureCode = "FT-001";
        var requestPayload =
                """
            {
                "fromTime": "%s",
                "toTime": "%s",
                "dryRun": false
            }
            """
                        .formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());

        EventStoreService.ReplayResult replayResult = createSuccessfulReplayResult();
        when(eventStoreService.replayEventsByFeatureAndTimeRange(
                        eq(featureCode), any(Instant.class), any(Instant.class), eq(false)))
                .thenReturn(replayResult);

        var result = mvc.post()
                .uri("/api/events/replay/feature/{featureCode}", featureCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalEvents")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReplayEventsByFeatures() throws Exception {
        var requestPayload =
                """
            {
                "featureCodes": ["FT-001", "FT-002"],
                "fromTime": "%s",
                "toTime": "%s",
                "dryRun": false
            }
            """
                        .formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());

        EventStoreService.ReplayResult replayResult = createSuccessfulReplayResult();
        when(eventStoreService.replayEventsByFeaturesAndTimeRange(
                        anySet(), any(Instant.class), any(Instant.class), eq(false)))
                .thenReturn(replayResult);

        var result = mvc.post()
                .uri("/api/events/replay/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalEvents")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldReplayEventsByTimeRange() throws Exception {
        var requestPayload =
                """
            {
                "fromTime": "%s",
                "toTime": "%s",
                "dryRun": true
            }
            """
                        .formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());

        EventStoreService.ReplayResult replayResult = createSuccessfulReplayResult();
        when(eventStoreService.replayEventsByTimeRange(any(Instant.class), any(Instant.class), eq(true)))
                .thenReturn(replayResult);

        var result = mvc.post()
                .uri("/api/events/replay/time-range")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalEvents")
                .asNumber()
                .isEqualTo(2);
    }

    @Test
    @WithMockOAuth2User(username = "user")
    void shouldHandleReplayWithErrors() throws Exception {
        var requestPayload =
                """
            {
                "fromTime": "%s",
                "toTime": "%s",
                "dryRun": false
            }
            """
                        .formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());

        EventStoreService.ReplayResult replayResult = createFailedReplayResult();
        when(eventStoreService.replayEventsByTimeRange(any(Instant.class), any(Instant.class), eq(false)))
                .thenReturn(replayResult);

        var result = mvc.post()
                .uri("/api/events/replay/time-range")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.hasErrors")
                .asBoolean()
                .isTrue();
    }

    private List<EventStore> createTestEvents() {
        Instant now = Instant.now();
        return List.of(new EventStore(
                "event-1", "FeatureCreatedEvent", "FT-001", "Feature", "test-data", "test-metadata", now, 1L));
    }

    private EventStoreService.ReplayResult createSuccessfulReplayResult() {
        EventStoreService.ReplayResult result = new EventStoreService.ReplayResult();
        result.incrementSuccessCount();
        result.incrementSuccessCount();
        return result;
    }

    private EventStoreService.ReplayResult createFailedReplayResult() {
        EventStoreService.ReplayResult result = new EventStoreService.ReplayResult();
        result.incrementSuccessCount();
        result.incrementFailureCount();
        result.addError("Test error message");
        return result;
    }
}
