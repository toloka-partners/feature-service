package com.sivalabs.ft.features.messaging;

import com.sivalabs.ft.features.config.RabbitMQProperties;
import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.events.FeatureDeletedEvent;
import com.sivalabs.ft.features.domain.events.FeatureUpdatedEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final RabbitMQErrorHandler errorHandler;

    public RabbitMQEventPublisher(
            RabbitTemplate rabbitTemplate, RabbitMQProperties properties, RabbitMQErrorHandler errorHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.errorHandler = errorHandler;
    }

    public void publishFeatureCreatedEvent(FeatureCreatedEvent event) {
        String routingKey = properties.exchange().routingKeys().featureCreated();
        publishEvent(event, routingKey, "FeatureCreated");
    }

    public void publishFeatureUpdatedEvent(FeatureUpdatedEvent event) {
        String routingKey = properties.exchange().routingKeys().featureUpdated();
        publishEvent(event, routingKey, "FeatureUpdated");
    }

    public void publishFeatureDeletedEvent(FeatureDeletedEvent event) {
        String routingKey = properties.exchange().routingKeys().featureDeleted();
        publishEvent(event, routingKey, "FeatureDeleted");
    }

    private void publishEvent(Object event, String routingKey, String eventType) {
        try {
            logger.info("Publishing {} event to RabbitMQ with routing key: {}", eventType, routingKey);

            rabbitTemplate.convertAndSend(
                    properties.exchange().name(), routingKey, createEventMessage(event, eventType));

            logger.info("Successfully published {} event to RabbitMQ", eventType);

        } catch (AmqpException e) {
            logger.error("Failed to publish {} event to RabbitMQ: {}", eventType, e.getMessage(), e);
            errorHandler.handlePublishingError(event, routingKey, eventType, e);
        } catch (Exception e) {
            logger.error("Unexpected error while publishing {} event to RabbitMQ: {}", eventType, e.getMessage(), e);
            errorHandler.handlePublishingError(event, routingKey, eventType, e);
        }
    }

    private EventMessage createEventMessage(Object event, String eventType) {
        return new EventMessage(eventType, event, Instant.now());
    }

    public record EventMessage(String eventType, Object payload, Instant timestamp) {}
}
