package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.EventStore;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, Long> {

    /**
     * Find events by feature code ordered by creation time
     */
    List<EventStore> findByCodeOrderByCreatedAtAsc(String code);

    /**
     * Find events by aggregate type ordered by creation time
     */
    List<EventStore> findByAggregateTypeOrderByCreatedAtAsc(String aggregateType);

    /**
     * Find events within a time range ordered by creation time
     */
    List<EventStore> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant fromTime, Instant toTime);

    /**
     * Find events by feature code within a time range
     */
    List<EventStore> findByCodeAndCreatedAtBetweenOrderByCreatedAtAsc(String code, Instant fromTime, Instant toTime);

    /**
     * Find events by aggregate type within a time range
     */
    List<EventStore> findByAggregateTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            String aggregateType, Instant fromTime, Instant toTime);

    /**
     * Find events by event type ordered by creation time
     */
    List<EventStore> findByEventTypeOrderByCreatedAtAsc(String eventType);

    /**
     * Find events by event type within a time range
     */
    List<EventStore> findByEventTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            String eventType, Instant fromTime, Instant toTime);

    /**
     * Check if an event ID already exists (for idempotency)
     */
    boolean existsByEventId(String eventId);

    /**
     * Find events after a specific version for a feature code
     */
    @Query("SELECT e FROM EventStore e WHERE e.code = :code AND e.version > :version ORDER BY e.version ASC")
    List<EventStore> findByCodeAndVersionGreaterThanOrderByVersionAsc(
            @Param("code") String code, @Param("version") Long version);

    /**
     * Get the latest version for a feature code
     */
    @Query("SELECT MAX(e.version) FROM EventStore e WHERE e.code = :code")
    Long findMaxVersionByCode(@Param("code") String code);
}
