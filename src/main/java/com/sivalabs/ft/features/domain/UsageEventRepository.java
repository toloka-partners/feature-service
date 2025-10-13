package com.sivalabs.ft.features.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    List<UsageEvent> findByFeatureCode(String featureCode);

    List<UsageEvent> findByProductCode(String productCode);

    @Query("SELECT u FROM UsageEvent u WHERE u.featureCode = :featureCode AND u.timestamp BETWEEN :start AND :end")
    List<UsageEvent> findByFeatureCodeAndTimestampBetween(
            @Param("featureCode") String featureCode, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT u FROM UsageEvent u WHERE u.productCode = :productCode AND u.timestamp BETWEEN :start AND :end")
    List<UsageEvent> findByProductCodeAndTimestampBetween(
            @Param("productCode") String productCode, @Param("start") Instant start, @Param("end") Instant end);
}
