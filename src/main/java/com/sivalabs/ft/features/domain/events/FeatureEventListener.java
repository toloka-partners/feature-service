package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.entities.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Component that listens for Feature-related application events.
 * This listener handles events asynchronously using a dedicated thread pool.
 * The @Async annotation ensures that event processing does not block the main thread.
 */
@Component
public class FeatureEventListener {
    private static final Logger logger = LoggerFactory.getLogger(FeatureEventListener.class);

    /**
     * Handles FeatureCreatedApplicationEvent asynchronously.
     * This method is executed on a thread from the custom event task executor pool.
     * It logs feature details including the thread name to verify async execution.
     *
     * @param event the FeatureCreatedApplicationEvent containing the created Feature
     */
    @Async
    @EventListener
    public void handleFeatureCreatedEvent(FeatureCreatedApplicationEvent event) {
        Feature feature = event.getFeature();
        String threadName = Thread.currentThread().getName();

        logger.info(
                "Feature created event received on thread [{}] - ID: {}, Name: {}",
                threadName,
                feature.getId(),
                feature.getTitle());

        // Simulate some processing time to demonstrate async behavior
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Event processing interrupted", e);
        }

        logger.info("Feature created event processing completed on thread [{}] - ID: {}", threadName, feature.getId());

        // Additional business logic can be added here
        // For example:
        // - Send notifications
        // - Update statistics
        // - Trigger other processes
    }
}
