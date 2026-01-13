package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReleaseRepository extends JpaRepository<Release, Long> {
    Optional<Release> findByCode(String code);

    List<Release> findByProductCode(String productCode);

    @Modifying
    void deleteByCode(String code);

    boolean existsByCode(String code);

    @Query(
            """
            SELECT r FROM Release r
            LEFT JOIN FETCH r.product p
            LEFT JOIN FETCH r.features f
            WHERE (:productCodes IS NULL OR p.code IN :productCodes)
            AND (:statuses IS NULL OR r.status IN :statuses)
            AND (:owner IS NULL OR r.owner = :owner)
            """)
    List<Release> findRoadmapReleases(
            @Param("productCodes") List<String> productCodes,
            @Param("statuses") List<ReleaseStatus> statuses,
            @Param("owner") String owner);

    boolean existsByOwner(String owner);
}
