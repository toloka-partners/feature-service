package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, Long> {

    List<FeatureUsage> findByFeatureCode(String featureCode);

    List<FeatureUsage> findByFeatureCodeAndActionType(String featureCode, ActionType actionType);

    List<FeatureUsage> findByFeatureCodeAndTimestampBetween(String featureCode, Instant startDate, Instant endDate);

    List<FeatureUsage> findByFeatureCodeAndActionTypeAndTimestampBetween(
            String featureCode, ActionType actionType, Instant startDate, Instant endDate);

    List<FeatureUsage> findByProductCode(String productCode);

    List<FeatureUsage> findByProductCodeAndActionType(String productCode, ActionType actionType);

    List<FeatureUsage> findByProductCodeAndTimestampBetween(String productCode, Instant startDate, Instant endDate);

    List<FeatureUsage> findByProductCodeAndActionTypeAndTimestampBetween(
            String productCode, ActionType actionType, Instant startDate, Instant endDate);

    List<FeatureUsage> findByActionType(ActionType actionType);

    List<FeatureUsage> findByUserId(String userId);

    List<FeatureUsage> findByTimestampBetween(Instant startDate, Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.featureCode = :featureCode")
    long countDistinctUsersByFeatureCode(@Param("featureCode") String featureCode);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.featureCode = :featureCode AND fu.actionType = :actionType")
    long countDistinctUsersByFeatureCodeAndActionType(
            @Param("featureCode") String featureCode, @Param("actionType") ActionType actionType);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.featureCode = :featureCode AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByFeatureCodeAndTimestampBetween(
            @Param("featureCode") String featureCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.featureCode = :featureCode AND fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByFeatureCodeAndActionTypeAndTimestampBetween(
            @Param("featureCode") String featureCode,
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.productCode = :productCode")
    long countDistinctUsersByProductCode(@Param("productCode") String productCode);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.actionType = :actionType")
    long countDistinctUsersByProductCodeAndActionType(
            @Param("productCode") String productCode, @Param("actionType") ActionType actionType);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByProductCodeAndTimestampBetween(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByProductCodeAndActionTypeAndTimestampBetween(
            @Param("productCode") String productCode,
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.productCode = :productCode")
    long countDistinctFeaturesByProductCode(@Param("productCode") String productCode);

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.actionType = :actionType")
    long countDistinctFeaturesByProductCodeAndActionType(
            @Param("productCode") String productCode, @Param("actionType") ActionType actionType);

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctFeaturesByProductCodeAndTimestampBetween(
            @Param("productCode") String productCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.productCode = :productCode AND fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctFeaturesByProductCodeAndActionTypeAndTimestampBetween(
            @Param("productCode") String productCode,
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu")
    long countDistinctUsers();

    @Query("SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.actionType = :actionType")
    long countDistinctUsersByActionType(@Param("actionType") ActionType actionType);

    @Query("SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByTimestampBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.userId) FROM FeatureUsage fu WHERE fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctUsersByActionTypeAndTimestampBetween(
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.featureCode IS NOT NULL")
    long countDistinctFeatures();

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.featureCode IS NOT NULL AND fu.actionType = :actionType")
    long countDistinctFeaturesByActionType(@Param("actionType") ActionType actionType);

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.featureCode IS NOT NULL AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctFeaturesByTimestampBetween(
            @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.featureCode) FROM FeatureUsage fu WHERE fu.featureCode IS NOT NULL AND fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctFeaturesByActionTypeAndTimestampBetween(
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(DISTINCT fu.productCode) FROM FeatureUsage fu WHERE fu.productCode IS NOT NULL")
    long countDistinctProducts();

    @Query(
            "SELECT COUNT(DISTINCT fu.productCode) FROM FeatureUsage fu WHERE fu.productCode IS NOT NULL AND fu.actionType = :actionType")
    long countDistinctProductsByActionType(@Param("actionType") ActionType actionType);

    @Query(
            "SELECT COUNT(DISTINCT fu.productCode) FROM FeatureUsage fu WHERE fu.productCode IS NOT NULL AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctProductsByTimestampBetween(
            @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(
            "SELECT COUNT(DISTINCT fu.productCode) FROM FeatureUsage fu WHERE fu.productCode IS NOT NULL AND fu.actionType = :actionType AND fu.timestamp BETWEEN :startDate AND :endDate")
    long countDistinctProductsByActionTypeAndTimestampBetween(
            @Param("actionType") ActionType actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
