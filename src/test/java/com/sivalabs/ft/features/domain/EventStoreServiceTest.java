package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.entities.EventStore;
import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class EventStoreServiceTest {

    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private EventStoreService eventStoreService;
    private ObjectMapper objectMapper;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        eventStoreService = new EventStoreService(eventStoreRepository, objectMapper, kafkaTemplate);
        baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void shouldGetEventsByFeatureAndTimeRange() {
        String featureCode = "FT-001";
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);

        List<EventStore> expectedEvents = createTestEvents();
        when(eventStoreRepository.findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc(featureCode, fromTime, toTime))
                .thenReturn(expectedEvents);

        List<EventStore> result = eventStoreService.getEventsByFeatureAndTimeRange(featureCode, fromTime, toTime);

        assertThat(result).isEqualTo(expectedEvents);
        verify(eventStoreRepository).findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc(featureCode, fromTime, toTime);
    }

    @Test
    void shouldGetEventsByFeaturesAndTimeRange() {
        Set<String> featureCodes = Set.of("FT-001", "FT-002");
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);

        List<EventStore> allEvents = createTestEventsForMultipleFeatures();
        when(eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime))
                .thenReturn(allEvents);

        List<EventStore> result = eventStoreService.getEventsByFeaturesAndTimeRange(featureCodes, fromTime, toTime);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventStore::getCode).containsExactlyInAnyOrder("FT-001", "FT-002");
    }

    @Test
    void shouldGetEventsByTimeRange() {
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);

        List<EventStore> expectedEvents = createTestEvents();
        when(eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime))
                .thenReturn(expectedEvents);

        List<EventStore> result = eventStoreService.getEventsByTimeRange(fromTime, toTime);

        assertThat(result).isEqualTo(expectedEvents);
        verify(eventStoreRepository).findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime);
    }

    @Test
    void shouldGetEventsByFeature() {
        String featureCode = "FT-001";
        List<EventStore> expectedEvents = createTestEvents();
        when(eventStoreRepository.findByCodeOrderByCreatedAtAsc(featureCode)).thenReturn(expectedEvents);

        List<EventStore> result = eventStoreService.getEventsByFeature(featureCode);

        assertThat(result).isEqualTo(expectedEvents);
        verify(eventStoreRepository).findByCodeOrderByCreatedAtAsc(featureCode);
    }

    @Test
    void shouldReplayEventsSuccessfully() throws Exception {
        List<EventStore> events = createTestEventsWithValidJson();

        EventStoreService.ReplayResult result = eventStoreService.replayEvents(events, false);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.hasErrors()).isFalse();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("feature-created-replay");
        assertThat(eventCaptor.getValue()).isInstanceOf(FeatureCreatedEvent.class);
    }

    @Test
    void shouldHandleDryRunReplay() {
        List<EventStore> events = createTestEventsWithValidJson();

        EventStoreService.ReplayResult result = eventStoreService.replayEvents(events, true);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.hasErrors()).isFalse();

        // Verify no Kafka messages were sent in dry run mode
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldHandleInvalidEventDataGracefully() {
        List<EventStore> events = List.of(createEventStoreWithInvalidJson());

        EventStoreService.ReplayResult result = eventStoreService.replayEvents(events, false);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Failed to replay event ID");
    }

    @Test
    void shouldReplayEventsByFeatureAndTimeRange() {
        String featureCode = "FT-001";
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);
        List<EventStore> events = createTestEventsWithValidJson();

        when(eventStoreRepository.findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc(featureCode, fromTime, toTime))
                .thenReturn(events);

        EventStoreService.ReplayResult result =
                eventStoreService.replayEventsByFeatureAndTimeRange(featureCode, fromTime, toTime, false);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void shouldReplayEventsByFeaturesAndTimeRange() {
        Set<String> featureCodes = Set.of("FT-001");
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);
        List<EventStore> events = createTestEventsWithValidJson();

        when(eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime))
                .thenReturn(events);

        EventStoreService.ReplayResult result =
                eventStoreService.replayEventsByFeaturesAndTimeRange(featureCodes, fromTime, toTime, false);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void shouldReplayEventsByTimeRange() {
        Instant fromTime = baseTime;
        Instant toTime = baseTime.plus(1, ChronoUnit.HOURS);
        List<EventStore> events = createTestEventsWithValidJson();

        when(eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime))
                .thenReturn(events);

        EventStoreService.ReplayResult result = eventStoreService.replayEventsByTimeRange(fromTime, toTime, false);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void replayResultShouldCalculateTotalCount() {
        EventStoreService.ReplayResult result = new EventStoreService.ReplayResult();
        result.incrementSuccessCount();
        result.incrementSuccessCount();
        result.incrementFailureCount();

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.hasErrors()).isTrue();
    }

    private List<EventStore> createTestEvents() {
        return List.of(new EventStore(
                "event-1", "FeatureCreatedEvent", "FT-001", "Feature", "test-data", "test-metadata", baseTime, 1L));
    }

    private List<EventStore> createTestEventsForMultipleFeatures() {
        return List.of(
                new EventStore(
                        "event-1",
                        "FeatureCreatedEvent",
                        "FT-001",
                        "Feature",
                        "test-data",
                        "test-metadata",
                        baseTime,
                        1L),
                new EventStore(
                        "event-2",
                        "FeatureCreatedEvent",
                        "FT-002",
                        "Feature",
                        "test-data",
                        "test-metadata",
                        baseTime.plus(10, ChronoUnit.MINUTES),
                        1L),
                new EventStore(
                        "event-3",
                        "FeatureCreatedEvent",
                        "FT-003",
                        "Feature",
                        "test-data",
                        "test-metadata",
                        baseTime.plus(20, ChronoUnit.MINUTES),
                        1L));
    }

    private List<EventStore> createTestEventsWithValidJson() {
        try {
            FeatureCreatedEvent event = new FeatureCreatedEvent(
                    1L, "FT-001", "Test Feature", "Description", FeatureStatus.NEW, null, "user1", "user1", baseTime);
            String eventData = objectMapper.writeValueAsString(event);

            return List.of(new EventStore(
                    "event-1", "FeatureCreatedEvent", "FT-001", "Feature", eventData, "test-metadata", baseTime, 1L));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private EventStore createEventStoreWithInvalidJson() {
        return new EventStore(
                "event-invalid",
                "FeatureCreatedEvent",
                "FT-001",
                "Feature",
                "invalid-json",
                "test-metadata",
                baseTime,
                1L);
    }
}
