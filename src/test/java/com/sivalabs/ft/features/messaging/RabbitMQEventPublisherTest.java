package com.sivalabs.ft.features.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.config.RabbitMQProperties;
import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.events.FeatureDeletedEvent;
import com.sivalabs.ft.features.domain.events.FeatureUpdatedEvent;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQErrorHandler errorHandler;

    private RabbitMQProperties properties;
    private RabbitMQEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = createTestProperties();
        publisher = new RabbitMQEventPublisher(rabbitTemplate, properties, errorHandler);
    }

    @Test
    void shouldPublishFeatureCreatedEvent() {
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
        publisher.publishFeatureCreatedEvent(event);

        // Then
        ArgumentCaptor<RabbitMQEventPublisher.EventMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQEventPublisher.EventMessage.class);

        verify(rabbitTemplate).convertAndSend(eq("feature.events"), eq("feature.created"), messageCaptor.capture());

        RabbitMQEventPublisher.EventMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.eventType()).isEqualTo("FeatureCreated");
        assertThat(capturedMessage.payload()).isEqualTo(event);
        assertThat(capturedMessage.timestamp()).isNotNull();
    }

    @Test
    void shouldPublishFeatureUpdatedEvent() {
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
        publisher.publishFeatureUpdatedEvent(event);

        // Then
        ArgumentCaptor<RabbitMQEventPublisher.EventMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQEventPublisher.EventMessage.class);

        verify(rabbitTemplate).convertAndSend(eq("feature.events"), eq("feature.updated"), messageCaptor.capture());

        RabbitMQEventPublisher.EventMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.eventType()).isEqualTo("FeatureUpdated");
        assertThat(capturedMessage.payload()).isEqualTo(event);
    }

    @Test
    void shouldPublishFeatureDeletedEvent() {
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
        publisher.publishFeatureDeletedEvent(event);

        // Then
        ArgumentCaptor<RabbitMQEventPublisher.EventMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQEventPublisher.EventMessage.class);

        verify(rabbitTemplate).convertAndSend(eq("feature.events"), eq("feature.deleted"), messageCaptor.capture());

        RabbitMQEventPublisher.EventMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.eventType()).isEqualTo("FeatureDeleted");
        assertThat(capturedMessage.payload()).isEqualTo(event);
    }

    @Test
    void shouldHandleAmqpExceptionDuringPublishing() {
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

        AmqpException exception = new AmqpException("Connection failed");
        doThrow(exception).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        publisher.publishFeatureCreatedEvent(event);

        // Then
        verify(errorHandler).handlePublishingError(event, "feature.created", "FeatureCreated", exception);
    }

    @Test
    void shouldHandleGenericExceptionDuringPublishing() {
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

        RuntimeException exception = new RuntimeException("Unexpected error");
        doThrow(exception).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        publisher.publishFeatureCreatedEvent(event);

        // Then
        verify(errorHandler).handlePublishingError(event, "feature.created", "FeatureCreated", exception);
    }

    private RabbitMQProperties createTestProperties() {
        RabbitMQProperties.Exchange.RoutingKeys routingKeys =
                new RabbitMQProperties.Exchange.RoutingKeys("feature.created", "feature.updated", "feature.deleted");

        RabbitMQProperties.Exchange exchange =
                new RabbitMQProperties.Exchange("feature.events", "topic", true, routingKeys);

        RabbitMQProperties.Retry retry = new RabbitMQProperties.Retry(3, 1000L, 2.0, 10000L);

        RabbitMQProperties.DeadLetter deadLetter =
                new RabbitMQProperties.DeadLetter("feature.events.dlx", "feature.events.dlq", "feature.failed");

        return new RabbitMQProperties("localhost", 5672, "guest", "guest", "", exchange, retry, deadLetter);
    }
}
