package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

interface ReleaseRepository extends ListCrudRepository<Release, Long>, PagingAndSortingRepository<Release, Long> {
    Optional<Release> findByCode(String code);

    List<Release> findByProductCode(String productCode);

    @Modifying
    void deleteByCode(String code);

    boolean existsByCode(String code);

    // New query methods for advanced endpoints

    /**
     * Find releases that are overdue (past planned release date but not completed)
     */
    @Query(
            "SELECT r FROM Release r WHERE r.plannedReleaseDate < :currentTime AND r.status NOT IN ('COMPLETED', 'RELEASED')")
    List<Release> findOverdueReleases(@Param("currentTime") Instant currentTime);

    /**
     * Find releases that are overdue with pagination
     */
    @Query(
            "SELECT r FROM Release r WHERE r.plannedReleaseDate < :currentTime AND r.status NOT IN ('COMPLETED', 'RELEASED')")
    Page<Release> findOverdueReleases(@Param("currentTime") Instant currentTime, Pageable pageable);

    /**
     * Find releases at risk (approaching deadline within threshold)
     */
    @Query(
            "SELECT r FROM Release r WHERE r.plannedReleaseDate BETWEEN :currentTime AND :thresholdDate AND r.status NOT IN ('COMPLETED', 'RELEASED')")
    List<Release> findAtRiskReleases(
            @Param("currentTime") Instant currentTime, @Param("thresholdDate") Instant thresholdDate);

    /**
     * Find releases at risk with pagination
     */
    @Query(
            "SELECT r FROM Release r WHERE r.plannedReleaseDate BETWEEN :currentTime AND :thresholdDate AND r.status NOT IN ('COMPLETED', 'RELEASED')")
    Page<Release> findAtRiskReleases(
            @Param("currentTime") Instant currentTime,
            @Param("thresholdDate") Instant thresholdDate,
            Pageable pageable);

    /**
     * Find releases by status
     */
    List<Release> findByStatus(ReleaseStatus status);

    /**
     * Find releases by status with pagination
     */
    Page<Release> findByStatus(ReleaseStatus status, Pageable pageable);

    /**
     * Find releases by owner (created by)
     */
    List<Release> findByCreatedBy(String createdBy);

    /**
     * Find releases by owner with pagination
     */
    Page<Release> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find releases by planned release date range
     */
    List<Release> findByPlannedReleaseDateBetween(Instant startDate, Instant endDate);

    /**
     * Find releases by planned release date range with pagination
     */
    Page<Release> findByPlannedReleaseDateBetween(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Find all releases with filtering and pagination
     */
    @Query("SELECT r FROM Release r WHERE " + "(:status IS NULL OR r.status = :status) AND "
            + "(:owner IS NULL OR r.createdBy = :owner) AND "
            + "(:startDate IS NULL OR r.plannedReleaseDate >= :startDate) AND "
            + "(:endDate IS NULL OR r.plannedReleaseDate <= :endDate)")
    Page<Release> findAllWithFilters(
            @Param("status") ReleaseStatus status,
            @Param("owner") String owner,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);
}
