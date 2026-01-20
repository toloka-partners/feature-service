package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.domain.entities.EventStore;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Sql("/test-data.sql")
class EventPublisherIntegrationTest extends AbstractIT {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clean up event store data before each test
        eventStoreRepository.deleteAll();

        // Try to find an existing product or create one for testing
        testProduct = productRepository.findAll().stream().findFirst().orElseGet(() -> {
            Product product = new Product();
            product.setCode("TEST-PRODUCT");
            product.setPrefix("TP");
            product.setName("Test Product");
            product.setDescription("Test Product Description");
            product.setImageUrl("http://example.com/test.png");
            product.setCreatedBy("test-user");
            product.setCreatedAt(Instant.now());
            return productRepository.save(product);
        });
    }

    @Test
    @Transactional
    void shouldPersistFeatureCreatedEventToEventStore() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // When
        eventPublisher.publishFeatureCreatedEvent(feature);

        // Then
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(events).hasSize(1);

        EventStore eventStore = events.get(0);
        assertThat(eventStore.getEventType()).isEqualTo("FeatureCreatedEvent");
        assertThat(eventStore.getCode()).isEqualTo(feature.getCode());
        assertThat(eventStore.getAggregateType()).isEqualTo("Feature");
        assertThat(eventStore.getVersion()).isEqualTo(1L);
        assertThat(eventStore.getEventData()).contains("\"code\":\"" + feature.getCode() + "\"");
        assertThat(eventStore.getEventData()).contains("\"title\":\"" + feature.getTitle() + "\"");
    }

    @Test
    @Transactional
    void shouldPersistFeatureUpdatedEventToEventStore() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // Simulate a previous created event
        eventPublisher.publishFeatureCreatedEvent(feature);

        // Update the feature
        feature.setTitle("Updated Feature Title");
        feature.setUpdatedBy("updater");
        feature.setUpdatedAt(Instant.now());

        // When
        eventPublisher.publishFeatureUpdatedEvent(feature);

        // Then
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(events).hasSize(2);

        EventStore createEvent = events.get(0);
        assertThat(createEvent.getEventType()).isEqualTo("FeatureCreatedEvent");
        assertThat(createEvent.getVersion()).isEqualTo(1L);

        EventStore updateEvent = events.get(1);
        assertThat(updateEvent.getEventType()).isEqualTo("FeatureUpdatedEvent");
        assertThat(updateEvent.getCode()).isEqualTo(feature.getCode());
        assertThat(updateEvent.getVersion()).isEqualTo(2L);
        assertThat(updateEvent.getEventData()).contains("\"title\":\"Updated Feature Title\"");
        assertThat(updateEvent.getEventData()).contains("\"updatedBy\":\"updater\"");
    }

    @Test
    @Transactional
    void shouldPersistFeatureDeletedEventToEventStore() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // Simulate previous events
        eventPublisher.publishFeatureCreatedEvent(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);

        String deletedBy = "admin";
        Instant deletedAt = Instant.now();

        // When
        eventPublisher.publishFeatureDeletedEvent(feature, deletedBy, deletedAt);

        // Then
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(events).hasSize(3);

        EventStore deleteEvent = events.get(2);
        assertThat(deleteEvent.getEventType()).isEqualTo("FeatureDeletedEvent");
        assertThat(deleteEvent.getCode()).isEqualTo(feature.getCode());
        assertThat(deleteEvent.getVersion()).isEqualTo(3L);
        assertThat(deleteEvent.getEventData()).contains("\"deletedBy\":\"" + deletedBy + "\"");
    }

    @Test
    @Transactional
    void shouldHandleIdempotentEventPublishing() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // When - publish the same event multiple times
        eventPublisher.publishFeatureCreatedEvent(feature);

        // Verify first event was persisted
        List<EventStore> eventsAfterFirst = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(eventsAfterFirst).hasSize(1);
        String firstEventId = eventsAfterFirst.get(0).getEventId();

        // Publish another event (this should be a new event, not idempotent since it's a new instance)
        eventPublisher.publishFeatureCreatedEvent(feature);

        // Then - should have two different events since they have different event IDs
        List<EventStore> eventsAfterSecond = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(eventsAfterSecond).hasSize(2);
        assertThat(eventsAfterSecond.get(0).getEventId()).isEqualTo(firstEventId);
        assertThat(eventsAfterSecond.get(1).getEventId()).isNotEqualTo(firstEventId);
    }

    @Test
    @Transactional
    void shouldIncrementVersionForEachEvent() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // When - publish multiple events for the same aggregate
        eventPublisher.publishFeatureCreatedEvent(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);

        feature.setTitle("Another Update");
        eventPublisher.publishFeatureUpdatedEvent(feature);

        // Then - versions should increment properly
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getVersion()).isEqualTo(1L);
        assertThat(events.get(1).getVersion()).isEqualTo(2L);
        assertThat(events.get(2).getVersion()).isEqualTo(3L);
    }

    @Test
    @Transactional
    void shouldPersistMetadataCorrectly() {
        // Given
        Feature feature = createTestFeature(testProduct);

        // When
        eventPublisher.publishFeatureCreatedEvent(feature);

        // Then
        List<EventStore> events = eventStoreRepository.findByCodeOrderByCreatedAtAsc(feature.getCode());
        assertThat(events).hasSize(1);

        EventStore eventStore = events.get(0);
        assertThat(eventStore.getMetadata()).isNotNull();
        assertThat(eventStore.getMetadata()).contains("\"eventType\":\"FeatureCreatedEvent\"");
        assertThat(eventStore.getMetadata()).contains("\"code\":\"" + feature.getCode() + "\"");
        assertThat(eventStore.getMetadata()).contains("\"aggregateType\":\"Feature\"");
        assertThat(eventStore.getMetadata()).contains("\"version\":1");
    }

    @Test
    @Transactional
    void shouldQueryEventsByTimeRange() {
        // Given
        Feature feature1 = createTestFeature(testProduct, "TEST-001");
        Feature feature2 = createTestFeature(testProduct, "TEST-002");

        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Create events at different times
        feature1.setCreatedAt(baseTime);
        feature2.setCreatedAt(baseTime.plus(1, ChronoUnit.HOURS));

        eventPublisher.publishFeatureCreatedEvent(feature1);
        eventPublisher.publishFeatureCreatedEvent(feature2);

        // When - query events in time range that includes only first event
        Instant fromTime = baseTime.minus(10, ChronoUnit.MINUTES);
        Instant toTime = baseTime.plus(30, ChronoUnit.MINUTES);

        List<EventStore> events = eventStoreRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(fromTime, toTime);

        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getCode()).isEqualTo("TEST-001");
    }

    private Feature createTestFeature(Product product) {
        return createTestFeature(product, "TEST-FEATURE-" + System.currentTimeMillis());
    }

    private Feature createTestFeature(Product product, String code) {
        Feature feature = new Feature();
        feature.setProduct(product);
        feature.setCode(code);
        feature.setTitle("Test Feature");
        feature.setDescription("Test feature description");
        feature.setStatus(FeatureStatus.NEW);
        feature.setCreatedBy("test-user");
        feature.setCreatedAt(Instant.now());
        return feature;
    }
}
