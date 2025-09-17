package com.sivalabs.ft.features.domain.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventDeduplicationService {
    private final ProcessedEventRepository repository;

    public EventDeduplicationService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean isEventProcessed(String eventId) {
        return repository.existsById(eventId);
    }

    @Transactional
    public void markEventAsProcessed(String eventId, String eventType, String aggregateId) {
        if (!repository.existsById(eventId)) {
            repository.save(new ProcessedEvent(eventId, eventType, aggregateId));
        }
    }
}
