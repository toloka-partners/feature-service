package com.sivalabs.ft.features.messaging;

import com.sivalabs.ft.features.domain.events.FeatureCreatedEvent;
import com.sivalabs.ft.features.domain.events.FeatureDeletedEvent;
import com.sivalabs.ft.features.domain.events.FeatureUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FeatureEventListener {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEventListener.class);

    private final RabbitMQEventPublisher rabbitMQEventPublisher;

    public FeatureEventListener(RabbitMQEventPublisher rabbitMQEventPublisher) {
        this.rabbitMQEventPublisher = rabbitMQEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFeatureCreatedEvent(FeatureCreatedEvent event) {
        logger.info("Received FeatureCreatedEvent for feature ID: {}, code: {}", event.id(), event.code());

        try {
            rabbitMQEventPublisher.publishFeatureCreatedEvent(event);
            logger.debug("Successfully relayed FeatureCreatedEvent to RabbitMQ for feature ID: {}", event.id());
        } catch (Exception e) {
            logger.error(
                    "Failed to relay FeatureCreatedEvent to RabbitMQ for feature ID: {}: {}",
                    event.id(),
                    e.getMessage(),
                    e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFeatureUpdatedEvent(FeatureUpdatedEvent event) {
        logger.info("Received FeatureUpdatedEvent for feature ID: {}, code: {}", event.id(), event.code());

        try {
            rabbitMQEventPublisher.publishFeatureUpdatedEvent(event);
            logger.debug("Successfully relayed FeatureUpdatedEvent to RabbitMQ for feature ID: {}", event.id());
        } catch (Exception e) {
            logger.error(
                    "Failed to relay FeatureUpdatedEvent to RabbitMQ for feature ID: {}: {}",
                    event.id(),
                    e.getMessage(),
                    e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFeatureDeletedEvent(FeatureDeletedEvent event) {
        logger.info("Received FeatureDeletedEvent for feature ID: {}, code: {}", event.id(), event.code());

        try {
            rabbitMQEventPublisher.publishFeatureDeletedEvent(event);
            logger.debug("Successfully relayed FeatureDeletedEvent to RabbitMQ for feature ID: {}", event.id());
        } catch (Exception e) {
            logger.error(
                    "Failed to relay FeatureDeletedEvent to RabbitMQ for feature ID: {}: {}",
                    event.id(),
                    e.getMessage(),
                    e);
        }
    }
}
