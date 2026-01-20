package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.domain.entities.EventStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class EventStoreRepositoryTest extends AbstractIT {

    @Autowired
    private EventStoreRepository eventStoreRepository;

    private EventStore testEvent1;
    private EventStore testEvent2;
    private EventStore testEvent3;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        eventStoreRepository.deleteAll();

        baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        long timestamp = System.currentTimeMillis();

        testEvent1 = new EventStore(
                "event-1-" + timestamp,
                "FeatureCreatedEvent",
                "FT-001",
                "Feature",
                "{\"id\":1,\"code\":\"FT-001\",\"title\":\"Test Feature 1\"}",
                "{\"metadata\":\"test\"}",
                baseTime,
                1L);

        testEvent2 = new EventStore(
                "event-2-" + timestamp,
                "FeatureUpdatedEvent",
                "FT-001",
                "Feature",
                "{\"id\":1,\"code\":\"FT-001\",\"title\":\"Updated Test Feature 1\"}",
                "{\"metadata\":\"test\"}",
                baseTime.plus(1, ChronoUnit.HOURS),
                2L);

        testEvent3 = new EventStore(
                "event-3-" + timestamp,
                "FeatureCreatedEvent",
                "FT-002",
                "Feature",
                "{\"id\":2,\"code\":\"FT-002\",\"title\":\"Test Feature 2\"}",
                "{\"metadata\":\"test\"}",
                baseTime.plus(2, ChronoUnit.HOURS),
                1L);

        eventStoreRepository.saveAll(List.of(testEvent1, testEvent2, testEvent3));
    }

    @Test
    void shouldFindEventsByCodeOrderedByCreatedAt() {
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc("FT-001");

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventId()).isEqualTo(testEvent1.getEventId());
        assertThat(events.get(1).getEventId()).isEqualTo(testEvent2.getEventId());
    }

    @Test
    void shouldFindEventsByAggregateType() {
        List<EventStore> events = eventStoreRepository.findByAggregateTypeOrderByCreatedAtAsc("Feature");

        assertThat(events).hasSize(3);
        assertThat(events)
                .extracting(EventStore::getEventId)
                .containsExactly(testEvent1.getEventId(), testEvent2.getEventId(), testEvent3.getEventId());
    }

    @Test
    void shouldFindEventsByTimeRange() {
        Instant fromTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant toTime = baseTime.plus(90, ChronoUnit.MINUTES);

        List<EventStore> events = eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime);

        assertThat(events).hasSize(2);
        assertThat(events)
                .extracting(EventStore::getEventId)
                .containsExactly(testEvent1.getEventId(), testEvent2.getEventId());
    }

    @Test
    void shouldFindEventsByCodeAndTimeRange() {
        Instant fromTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant toTime = baseTime.plus(90, ChronoUnit.MINUTES);

        List<EventStore> events =
                eventStoreRepository.findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc("FT-001", fromTime, toTime);

        assertThat(events).hasSize(2);
        assertThat(events)
                .extracting(EventStore::getEventId)
                .containsExactly(testEvent1.getEventId(), testEvent2.getEventId());
    }

    @Test
    void shouldFindEventsByEventType() {
        List<EventStore> events = eventStoreRepository.findByEventTypeOrderByCreatedAtAsc("FeatureCreatedEvent");

        assertThat(events).hasSize(2);
        assertThat(events).extracting(EventStore::getCode).containsExactly("FT-001", "FT-002");
    }

    @Test
    void shouldCheckEventIdExists() {
        boolean exists = eventStoreRepository.existsByEventId(testEvent1.getEventId());
        assertThat(exists).isTrue();

        boolean notExists = eventStoreRepository.existsByEventId("non-existent");
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldFindEventsByVersionGreaterThan() {
        List<EventStore> events = eventStoreRepository.findByCodeAndVersionGreaterThanOrderByVersionAsc("FT-001", 1L);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventId()).isEqualTo(testEvent2.getEventId());
        assertThat(events.get(0).getVersion()).isEqualTo(2L);
    }

    @Test
    void shouldFindMaxVersionByCode() {
        Long maxVersion = eventStoreRepository.findMaxVersionByCode("FT-001");
        assertThat(maxVersion).isEqualTo(2L);

        Long maxVersionForNewAggregate = eventStoreRepository.findMaxVersionByCode("FT-002");
        assertThat(maxVersionForNewAggregate).isEqualTo(1L);

        Long maxVersionForNonExistent = eventStoreRepository.findMaxVersionByCode("NON-EXISTENT");
        assertThat(maxVersionForNonExistent).isNull();
    }

    @Test
    void shouldFindEventsByEventTypeAndTimeRange() {
        Instant fromTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant toTime = baseTime.plus(3, ChronoUnit.HOURS);

        List<EventStore> events = eventStoreRepository.findByEventTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
                "FeatureCreatedEvent", fromTime, toTime);

        assertThat(events).hasSize(2);
        assertThat(events).extracting(EventStore::getCode).containsExactly("FT-001", "FT-002");
    }

    @Test
    void shouldFindEventsByAggregateTypeAndTimeRange() {
        Instant fromTime = baseTime.plus(30, ChronoUnit.MINUTES);
        Instant toTime = baseTime.plus(3, ChronoUnit.HOURS);

        List<EventStore> events = eventStoreRepository.findByAggregateTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
                "Feature", fromTime, toTime);

        assertThat(events).hasSize(2);
        assertThat(events)
                .extracting(EventStore::getEventId)
                .containsExactly(testEvent2.getEventId(), testEvent3.getEventId());
    }
}
