package com.sivalabs.ft.features.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sivalabs.ft.features.ApplicationProperties;
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
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ApplicationProperties properties;

    @Mock
    private ApplicationProperties.RabbitMQProperties rabbitMQProperties;

    @Mock
    private ApplicationProperties.RabbitMQProperties.RoutingKeyProperties routingKeyProperties;

    private RabbitMQEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(properties.rabbitmq()).thenReturn(rabbitMQProperties);
        when(rabbitMQProperties.exchange()).thenReturn("test-exchange");
        when(rabbitMQProperties.routingKey()).thenReturn(routingKeyProperties);

        publisher = new RabbitMQEventPublisher(rabbitTemplate, properties);
    }

    @Test
    void shouldPublishFeatureCreatedEventSuccessfully() {
        when(routingKeyProperties.created()).thenReturn("feature.created");

        FeatureCreatedEvent event = new FeatureCreatedEvent(
                1L,
                "TEST-1",
                "Test Feature",
                "Test Description",
                FeatureStatus.NEW,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now());

        publisher.publishFeatureCreatedEvent(event);

        verify(rabbitTemplate).convertAndSend("test-exchange", "feature.created", event);
    }

    @Test
    void shouldPublishFeatureUpdatedEventSuccessfully() {
        when(routingKeyProperties.updated()).thenReturn("feature.updated");

        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                1L,
                "TEST-1",
                "Updated Feature",
                "Updated Description",
                FeatureStatus.IN_PROGRESS,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now(),
                "updater",
                Instant.now());

        publisher.publishFeatureUpdatedEvent(event);

        verify(rabbitTemplate).convertAndSend("test-exchange", "feature.updated", event);
    }

    @Test
    void shouldPublishFeatureDeletedEventSuccessfully() {
        when(routingKeyProperties.deleted()).thenReturn("feature.deleted");

        FeatureDeletedEvent event = new FeatureDeletedEvent(
                1L,
                "TEST-1",
                "Deleted Feature",
                "Deleted Description",
                FeatureStatus.RELEASED,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now(),
                "updater",
                Instant.now(),
                "deleter",
                Instant.now());

        publisher.publishFeatureDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend("test-exchange", "feature.deleted", event);
    }

    @Test
    void shouldThrowExceptionWhenPublishingFeatureCreatedEventFails() {
        when(routingKeyProperties.created()).thenReturn("feature.created");

        FeatureCreatedEvent event = new FeatureCreatedEvent(
                1L,
                "TEST-1",
                "Test Feature",
                "Test Description",
                FeatureStatus.NEW,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now());

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("test-exchange", "feature.created", event);

        assertThatThrownBy(() -> publisher.publishFeatureCreatedEvent(event))
                .isInstanceOf(RabbitMQEventPublisher.RabbitMQPublishException.class)
                .hasMessageContaining("Failed to publish FeatureCreatedEvent")
                .hasCauseInstanceOf(AmqpException.class);
    }

    @Test
    void shouldThrowExceptionWhenPublishingFeatureUpdatedEventFails() {
        when(routingKeyProperties.updated()).thenReturn("feature.updated");

        FeatureUpdatedEvent event = new FeatureUpdatedEvent(
                1L,
                "TEST-1",
                "Updated Feature",
                "Updated Description",
                FeatureStatus.IN_PROGRESS,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now(),
                "updater",
                Instant.now());

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("test-exchange", "feature.updated", event);

        assertThatThrownBy(() -> publisher.publishFeatureUpdatedEvent(event))
                .isInstanceOf(RabbitMQEventPublisher.RabbitMQPublishException.class)
                .hasMessageContaining("Failed to publish FeatureUpdatedEvent");
    }

    @Test
    void shouldThrowExceptionWhenPublishingFeatureDeletedEventFails() {
        when(routingKeyProperties.deleted()).thenReturn("feature.deleted");

        FeatureDeletedEvent event = new FeatureDeletedEvent(
                1L,
                "TEST-1",
                "Deleted Feature",
                "Deleted Description",
                FeatureStatus.RELEASED,
                "RELEASE-1",
                "user1",
                "creator",
                Instant.now(),
                "updater",
                Instant.now(),
                "deleter",
                Instant.now());

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("test-exchange", "feature.deleted", event);

        assertThatThrownBy(() -> publisher.publishFeatureDeletedEvent(event))
                .isInstanceOf(RabbitMQEventPublisher.RabbitMQPublishException.class)
                .hasMessageContaining("Failed to publish FeatureDeletedEvent");
    }
}
