package com.sivalabs.ft.features.domain.events;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Table(name = "processed_events")
class ProcessedEvent {
    @Id
    private String eventId;

    private String eventType;
    private String aggregateId;
    private Instant processedAt;

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId, String eventType, String aggregateId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}

@Repository
interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}

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
