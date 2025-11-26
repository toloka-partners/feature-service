package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureDependencyRepository extends JpaRepository<FeatureDependency, Long> {

    @Query(
            "SELECT fd FROM FeatureDependency fd JOIN FETCH fd.feature f JOIN FETCH fd.dependsOnFeature d WHERE f.code = :code")
    List<FeatureDependency> findByFeature_Code(@Param("code") String code);

    @Query(
            "SELECT fd FROM FeatureDependency fd JOIN FETCH fd.feature f JOIN FETCH fd.dependsOnFeature d WHERE d.code = :code")
    List<FeatureDependency> findByDependsOnFeature_Code(@Param("code") String code);

    @Query(
            "SELECT fd FROM FeatureDependency fd JOIN FETCH fd.feature f JOIN FETCH fd.dependsOnFeature d WHERE f.code = :featureCode AND d.code = :dependsOnFeatureCode")
    Optional<FeatureDependency> findByFeature_CodeAndDependsOnFeature_Code(
            @Param("featureCode") String featureCode, @Param("dependsOnFeatureCode") String dependsOnFeatureCode);

    boolean existsByFeature_CodeAndDependsOnFeature_Code(String featureCode, String dependsOnFeatureCode);

    void deleteByFeature_CodeAndDependsOnFeature_Code(String featureCode, String dependsOnFeatureCode);

    void deleteByFeature_Code(String featureCode);

    void deleteByDependsOnFeature_Code(String dependsOnFeatureCode);
}
