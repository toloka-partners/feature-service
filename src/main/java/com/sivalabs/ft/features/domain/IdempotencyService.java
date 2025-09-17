package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.events.ProcessedEvent;
import com.sivalabs.ft.features.domain.events.ProcessedEventRepository;
import com.sivalabs.ft.features.domain.models.EventType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKey {
    @Id
    private String key;

    private String operationType;
    private String result;
    private Instant createdAt;

    public IdempotencyKey() {}

    public IdempotencyKey(String key, String operationType, String result) {
        this.key = key;
        this.operationType = operationType;
        this.result = result;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getResult() {
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

@Repository
interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {}

@Service
public class IdempotencyService {
    private final IdempotencyKeyRepository repository;
    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyService(IdempotencyKeyRepository repository, ProcessedEventRepository processedEventRepository) {
        this.repository = repository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public Optional<String> checkAndStore(String key, String operationType, String result) {
        Optional<IdempotencyKey> existing = repository.findById(key);
        if (existing.isPresent()) {
            return Optional.of(existing.get().getResult());
        }

        repository.save(new IdempotencyKey(key, operationType, result));

        // Also track in processed_events table for API operations
        if (!processedEventRepository.existsById(key)) {
            processedEventRepository.save(new ProcessedEvent(key, EventType.API.name(), operationType));
        }

        return Optional.empty();
    }
}
