package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.UsageEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

interface UsageEventRepository extends ListCrudRepository<UsageEvent, Long> {

    @Query(
            """
            SELECT ue FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND ue.eventType = COALESCE(:eventType, ue.eventType)
            AND ue.createdAt >= COALESCE(:startDate, ue.createdAt)
            AND ue.createdAt <= COALESCE(:endDate, ue.createdAt)
            ORDER BY ue.createdAt DESC
            """)
    List<UsageEvent> findByFeatureCodeWithFilters(
            @Param("featureCode") String featureCode,
            @Param("eventType") String eventType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT ue FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND ue.eventType = COALESCE(:eventType, ue.eventType)
            AND ue.createdAt >= COALESCE(:startDate, ue.createdAt)
            AND ue.createdAt <= COALESCE(:endDate, ue.createdAt)
            ORDER BY ue.createdAt DESC
            """)
    List<UsageEvent> findByProductCodeWithFilters(
            @Param("productCode") String productCode,
            @Param("eventType") String eventType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
