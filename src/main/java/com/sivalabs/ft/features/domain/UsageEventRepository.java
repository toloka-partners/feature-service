package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.UsageEvent;
import com.sivalabs.ft.features.domain.models.UsageEventType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

interface UsageEventRepository extends ListCrudRepository<UsageEvent, Long> {

    List<UsageEvent> findByFeatureCodeOrderByCreatedAtDesc(String featureCode);

    List<UsageEvent> findByProductCodeOrderByCreatedAtDesc(String productCode);

    List<UsageEvent> findByFeatureCodeAndEventTypeOrderByCreatedAtDesc(String featureCode, UsageEventType eventType);

    List<UsageEvent> findByProductCodeAndEventTypeOrderByCreatedAtDesc(String productCode, UsageEventType eventType);

    @Query(
            """
            SELECT ue FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND (:eventType IS NULL OR ue.eventType = :eventType)
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            ORDER BY ue.createdAt DESC
            """)
    List<UsageEvent> findByFeatureCodeWithFilters(
            @Param("featureCode") String featureCode,
            @Param("eventType") UsageEventType eventType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT ue FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:eventType IS NULL OR ue.eventType = :eventType)
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            ORDER BY ue.createdAt DESC
            """)
    List<UsageEvent> findByProductCodeWithFilters(
            @Param("productCode") String productCode,
            @Param("eventType") UsageEventType eventType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT COUNT(ue) FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Long countByFeatureCode(
            @Param("featureCode") String featureCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT COUNT(DISTINCT ue.userId) FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Long countDistinctUsersByFeatureCode(
            @Param("featureCode") String featureCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT COUNT(ue) FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Long countByProductCode(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT COUNT(DISTINCT ue.userId) FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Long countDistinctUsersByProductCode(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT COUNT(DISTINCT ue.featureCode) FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Long countDistinctFeaturesByProductCode(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT MIN(ue.createdAt) FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Instant findFirstEventTimeByFeatureCode(
            @Param("featureCode") String featureCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT MAX(ue.createdAt) FROM UsageEvent ue
            WHERE ue.featureCode = :featureCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Instant findLastEventTimeByFeatureCode(
            @Param("featureCode") String featureCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT MIN(ue.createdAt) FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Instant findFirstEventTimeByProductCode(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            """
            SELECT MAX(ue.createdAt) FROM UsageEvent ue
            WHERE ue.productCode = :productCode
            AND (:startDate IS NULL OR ue.createdAt >= :startDate)
            AND (:endDate IS NULL OR ue.createdAt <= :endDate)
            """)
    Instant findLastEventTimeByProductCode(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
