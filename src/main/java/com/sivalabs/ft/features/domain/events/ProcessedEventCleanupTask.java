package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.UniversalIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanupTask.class);

    private final UniversalIdempotencyService universalIdempotencyService;

    public ProcessedEventCleanupTask(UniversalIdempotencyService universalIdempotencyService) {
        this.universalIdempotencyService = universalIdempotencyService;
    }

    /**
     * Clean up expired events every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredEvents() {
        log.info("Starting cleanup of expired processed events");
        try {
            universalIdempotencyService.cleanupExpiredEvents();
            log.info("Successfully completed cleanup of expired processed events");
        } catch (Exception e) {
            log.error("Error during cleanup of expired processed events", e);
        }
    }
}
