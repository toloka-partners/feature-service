package com.sivalabs.ft.features.messaging;

import com.sivalabs.ft.features.config.RabbitMQProperties;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQErrorHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final AtomicInteger retryCounter = new AtomicInteger(0);

    public RabbitMQErrorHandler(RabbitTemplate rabbitTemplate, RabbitMQProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void handlePublishingError(Object event, String routingKey, String eventType, Exception error) {
        int currentAttempt = retryCounter.incrementAndGet();

        logger.error(
                "Publishing error for {} event (attempt {}): {}", eventType, currentAttempt, error.getMessage(), error);

        // Log event context for debugging
        logEventContext(event, routingKey, eventType, currentAttempt, error);

        // If max attempts reached, send to dead letter queue
        if (currentAttempt >= properties.retry().maxAttempts()) {
            logger.warn(
                    "Max retry attempts ({}) reached for {} event. Sending to dead letter queue.",
                    properties.retry().maxAttempts(),
                    eventType);
            sendToDeadLetterQueue(event, routingKey, eventType, error);
            retryCounter.set(0); // Reset counter for next event
        }
    }

    private void logEventContext(Object event, String routingKey, String eventType, int attempt, Exception error) {
        logger.info(
                "Event context - Type: {}, RoutingKey: {}, Attempt: {}, Timestamp: {}, Payload: {}",
                eventType,
                routingKey,
                attempt,
                Instant.now(),
                event);
        logger.error(
                "Error details - Message: {}, Cause: {}",
                error.getMessage(),
                error.getCause() != null ? error.getCause().getMessage() : "None");
    }

    private void sendToDeadLetterQueue(Object event, String originalRoutingKey, String eventType, Exception error) {
        try {
            DeadLetterMessage deadLetterMessage = new DeadLetterMessage(
                    eventType,
                    event,
                    originalRoutingKey,
                    error.getMessage(),
                    Instant.now(),
                    properties.retry().maxAttempts());

            rabbitTemplate.convertAndSend(
                    properties.deadLetter().exchangeName(),
                    properties.deadLetter().routingKey(),
                    deadLetterMessage);

            logger.info("Successfully sent {} event to dead letter queue", eventType);

        } catch (Exception dlqError) {
            logger.error(
                    "Failed to send {} event to dead letter queue: {}", eventType, dlqError.getMessage(), dlqError);
        }
    }

    public record DeadLetterMessage(
            String originalEventType,
            Object originalPayload,
            String originalRoutingKey,
            String errorMessage,
            Instant failedAt,
            int maxAttemptsReached) {}
}
