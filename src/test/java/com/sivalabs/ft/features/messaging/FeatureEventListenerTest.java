package com.sivalabs.ft.features.messaging;

import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.events.FeatureDeletedEvent;
import com.sivalabs.ft.features.domain.events.FeatureUpdatedEvent;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureEventListenerTest {

    @Mock
    private RabbitMQEventPublisher rabbitMQEventPublisher;

    private FeatureEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new FeatureEventListener(rabbitMQEventPublisher);
    }

    @Test
    void shouldHandleFeatureCreatedEvent() {
        // Given
        FeatureCreatedEvent event = new FeatureCreatedEvent(
                1L,
                "FEAT-001",
                "Test Feature",
                "Description",
                FeatureStatus.NEW,
                "REL-001",
                "user1",
                "creator",
                Instant.now());

        // When
        eventListener.handleFeatureCreatedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureCreatedEvent(event);
    }

    @Test
    void shouldHandleFeatureUpdatedEvent() {
        // Given
        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                1L,
                "FEAT-001",
                "Updated Feature",
                "Updated Description",
                FeatureStatus.IN_PROGRESS,
                "REL-001",
                "user1",
                "creator",
                Instant.now().minusSeconds(3600),
                "updater",
                Instant.now());

        // When
        eventListener.handleFeatureUpdatedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureUpdatedEvent(event);
    }

    @Test
    void shouldHandleFeatureDeletedEvent() {
        // Given
        FeatureDeletedEvent event = new FeatureDeletedEvent(
                1L,
                "FEAT-001",
                "Deleted Feature",
                "Description",
                FeatureStatus.RELEASED,
                "REL-001",
                "user1",
                "creator",
                Instant.now().minusSeconds(7200),
                "updater",
                Instant.now().minusSeconds(3600),
                "deleter",
                Instant.now());

        // When
        eventListener.handleFeatureDeletedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureDeletedEvent(event);
    }

    @Test
    void shouldHandleExceptionInFeatureCreatedEvent() {
        // Given
        FeatureCreatedEvent event = new FeatureCreatedEvent(
                1L,
                "FEAT-001",
                "Test Feature",
                "Description",
                FeatureStatus.NEW,
                "REL-001",
                "user1",
                "creator",
                Instant.now());

        doThrow(new RuntimeException("Publishing failed"))
                .when(rabbitMQEventPublisher)
                .publishFeatureCreatedEvent(event);

        // When
        eventListener.handleFeatureCreatedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureCreatedEvent(event);
        // Exception should be caught and logged, not propagated
    }

    @Test
    void shouldHandleExceptionInFeatureUpdatedEvent() {
        // Given
        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                1L,
                "FEAT-001",
                "Updated Feature",
                "Updated Description",
                FeatureStatus.IN_PROGRESS,
                "REL-001",
                "user1",
                "creator",
                Instant.now().minusSeconds(3600),
                "updater",
                Instant.now());

        doThrow(new RuntimeException("Publishing failed"))
                .when(rabbitMQEventPublisher)
                .publishFeatureUpdatedEvent(event);

        // When
        eventListener.handleFeatureUpdatedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureUpdatedEvent(event);
        // Exception should be caught and logged, not propagated
    }

    @Test
    void shouldHandleExceptionInFeatureDeletedEvent() {
        // Given
        FeatureDeletedEvent event = new FeatureDeletedEvent(
                1L,
                "FEAT-001",
                "Deleted Feature",
                "Description",
                FeatureStatus.RELEASED,
                "REL-001",
                "user1",
                "creator",
                Instant.now().minusSeconds(7200),
                "updater",
                Instant.now().minusSeconds(3600),
                "deleter",
                Instant.now());

        doThrow(new RuntimeException("Publishing failed"))
                .when(rabbitMQEventPublisher)
                .publishFeatureDeletedEvent(event);

        // When
        eventListener.handleFeatureDeletedEvent(event);

        // Then
        verify(rabbitMQEventPublisher).publishFeatureDeletedEvent(event);
        // Exception should be caught and logged, not propagated
    }
}
