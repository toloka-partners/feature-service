package com.sivalabs.ft.features.domain.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    @Query("SELECT p FROM ProcessedEvent p WHERE p.eventId = :eventId AND p.eventType = :eventType")
    Optional<ProcessedEvent> findByEventIdAndEventType(
            @Param("eventId") String eventId, @Param("eventType") String eventType);

    @Query(
            "SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProcessedEvent p WHERE p.eventId = :eventId AND p.eventType = :eventType")
    boolean existsByEventIdAndEventType(@Param("eventId") String eventId, @Param("eventType") String eventType);

    @Query("DELETE FROM ProcessedEvent p WHERE p.expiresAt IS NOT NULL AND p.expiresAt < :now")
    void deleteExpiredEvents(@Param("now") Instant now);

    @Query("SELECT p FROM ProcessedEvent p WHERE p.expiresAt IS NOT NULL AND p.expiresAt < :now")
    List<ProcessedEvent> findExpiredEvents(@Param("now") Instant now);
}
