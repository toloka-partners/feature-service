package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.events.ProcessedEvent;
import com.sivalabs.ft.features.domain.events.ProcessedEventRepository;
import com.sivalabs.ft.features.domain.models.EventType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversalIdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    public UniversalIdempotencyService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Check if an event has already been processed and store it if not.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     * @param resultData Optional result data to store
     * @return Optional containing the result data if event was already processed
     */
    @Transactional
    public Optional<String> checkAndProcess(String eventId, EventType eventType, String resultData) {
        return checkAndProcess(eventId, eventType, resultData, null);
    }

    /**
     * Check if an event has already been processed and store it if not.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     * @param resultData Optional result data to store
     * @param expiresAt Optional expiration time for the event
     * @return Optional containing the result data if event was already processed
     */
    @Transactional
    public Optional<String> checkAndProcess(String eventId, EventType eventType, String resultData, Instant expiresAt) {
        Optional<ProcessedEvent> existing =
                processedEventRepository.findByEventIdAndEventType(eventId, eventType.name());

        if (existing.isPresent()) {
            return Optional.ofNullable(existing.get().getResultData());
        }

        ProcessedEvent event = new ProcessedEvent(eventId, eventType.name(), resultData, expiresAt);
        processedEventRepository.save(event);

        return Optional.empty();
    }

    /**
     * Check if an event has been processed without storing it.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     * @return true if the event has been processed
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId, EventType eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType.name());
    }

    /**
     * Mark an event as processed.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     */
    @Transactional
    public void markAsProcessed(String eventId, EventType eventType) {
        markAsProcessed(eventId, eventType, null, null);
    }

    /**
     * Mark an event as processed with result data.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     * @param resultData Optional result data to store
     */
    @Transactional
    public void markAsProcessed(String eventId, EventType eventType, String resultData) {
        markAsProcessed(eventId, eventType, resultData, null);
    }

    /**
     * Mark an event as processed with result data and expiration.
     *
     * @param eventId The unique event identifier
     * @param eventType The type of event (API or EVENT)
     * @param resultData Optional result data to store
     * @param expiresAt Optional expiration time for the event
     */
    @Transactional
    public void markAsProcessed(String eventId, EventType eventType, String resultData, Instant expiresAt) {
        if (!isProcessed(eventId, eventType)) {
            ProcessedEvent event = new ProcessedEvent(eventId, eventType.name(), resultData, expiresAt);
            processedEventRepository.save(event);
        }
    }

    /**
     * Clean up expired events.
     */
    @Transactional
    public void cleanupExpiredEvents() {
        processedEventRepository.deleteExpiredEvents(Instant.now());
    }
}
