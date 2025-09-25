package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureDependencyService {
    private final FeatureRepository featureRepository;
    private final FeatureDependencyRepository featureDependencyRepository;

    FeatureDependencyService(
            FeatureRepository featureRepository, FeatureDependencyRepository featureDependencyRepository) {
        this.featureRepository = featureRepository;
        this.featureDependencyRepository = featureDependencyRepository;
    }

    @Transactional
    public void createFeatureDependency(Commands.CreateFeatureDependencyCommand cmd) {
        var feature = featureRepository.findByCode(cmd.featureCode()).orElseThrow();
        var dependsOnFeature =
                featureRepository.findByCode(cmd.dependsOnFeatureCode()).orElseThrow();

        // Validate dependency type
        DependencyType dependencyType;
        try {
            dependencyType = DependencyType.valueOf(cmd.dependencyType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid dependency type: " + cmd.dependencyType() + ". Supported types are: HARD, SOFT, OPTIONAL");
        }

        var dependency = new FeatureDependency();
        dependency.setFeature(feature);
        dependency.setDependsOnFeature(dependsOnFeature);
        dependency.setDependencyType(dependencyType);
        dependency.setNotes(cmd.notes());
        dependency.setCreatedAt(Instant.now());
        featureDependencyRepository.save(dependency);
    }

    @Transactional
    public void updateFeatureDependency(Commands.UpdateFeatureDependencyCommand cmd) {
        var dependency = featureDependencyRepository
                .findByFeature_CodeAndDependsOnFeature_Code(cmd.featureCode(), cmd.dependsOnFeatureCode())
                .orElseThrow();

        // Validate dependency type
        DependencyType dependencyType;
        try {
            dependencyType = DependencyType.valueOf(cmd.dependencyType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid dependency type: " + cmd.dependencyType() + ". Supported types are: HARD, SOFT, OPTIONAL");
        }

        dependency.setDependencyType(dependencyType);
        dependency.setNotes(cmd.notes());
        featureDependencyRepository.save(dependency);
    }

    @Transactional
    public void deleteFeatureDependency(Commands.DeleteFeatureDependencyCommand cmd) {
        var dependency = featureDependencyRepository
                .findByFeature_CodeAndDependsOnFeature_Code(cmd.featureCode(), cmd.dependsOnFeatureCode())
                .orElseThrow();

        featureDependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> getDependencies(String featureCode) {
        List<FeatureDependency> dependencies = featureDependencyRepository.findByFeature_Code(featureCode);
        // Initialize lazy associations
        dependencies.forEach(dep -> {
            dep.getDependsOnFeature().getProduct().getCode();
            if (dep.getDependsOnFeature().getRelease() != null) {
                dep.getDependsOnFeature().getRelease().getCode();
            }
        });
        return dependencies;
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> getDependents(String featureCode) {
        List<FeatureDependency> dependents = featureDependencyRepository.findByDependsOnFeature_Code(featureCode);
        // Initialize lazy associations
        dependents.forEach(dep -> {
            dep.getFeature().getProduct().getCode();
            if (dep.getFeature().getRelease() != null) {
                dep.getFeature().getRelease().getCode();
            }
        });
        return dependents;
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> getDependenciesWithFiltering(
            String featureCode, String productCode, String releaseCode, String status) {
        List<FeatureDependency> dependencies = getDependencies(featureCode);

        return dependencies.stream()
                .peek(dep -> {
                    // Initialize lazy associations
                    dep.getDependsOnFeature().getProduct().getCode();
                    if (dep.getDependsOnFeature().getRelease() != null) {
                        dep.getDependsOnFeature().getRelease().getCode();
                    }
                })
                .filter(dep -> {
                    Feature dependsOnFeature = dep.getDependsOnFeature();
                    boolean matches = true;

                    if (productCode != null
                            && !productCode.equals(dependsOnFeature.getProduct().getCode())) {
                        matches = false;
                    }

                    if (releaseCode != null
                            && (dependsOnFeature.getRelease() == null
                                    || !releaseCode.equals(
                                            dependsOnFeature.getRelease().getCode()))) {
                        matches = false;
                    }

                    if (status != null
                            && !status.equals(dependsOnFeature.getStatus().name())) {
                        matches = false;
                    }

                    return matches;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> getDependentsWithFiltering(
            String featureCode, String productCode, String releaseCode, String status) {
        List<FeatureDependency> dependents = getDependents(featureCode);

        return dependents.stream()
                .peek(dep -> {
                    // Initialize lazy associations
                    dep.getFeature().getProduct().getCode();
                    if (dep.getFeature().getRelease() != null) {
                        dep.getFeature().getRelease().getCode();
                    }
                })
                .filter(dep -> {
                    Feature dependentFeature = dep.getFeature();
                    boolean matches = true;

                    if (productCode != null
                            && !productCode.equals(dependentFeature.getProduct().getCode())) {
                        matches = false;
                    }

                    if (releaseCode != null
                            && (dependentFeature.getRelease() == null
                                    || !releaseCode.equals(
                                            dependentFeature.getRelease().getCode()))) {
                        matches = false;
                    }

                    if (status != null
                            && !status.equals(dependentFeature.getStatus().name())) {
                        matches = false;
                    }

                    return matches;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> getAllAffectedFeatureCodes(String featureCode) {
        Set<String> affectedFeatures = new java.util.HashSet<>();
        Set<String> processedFeatures = new java.util.HashSet<>();

        // Start recursion with the original feature, but don't include it in affected features
        getAllAffectedFeatureCodesRecursive(featureCode, affectedFeatures, processedFeatures, featureCode);

        return affectedFeatures;
    }

    private void getAllAffectedFeatureCodesRecursive(
            String featureCode,
            Set<String> affectedFeatures,
            Set<String> processedFeatures,
            String originalFeatureCode) {
        // Avoid infinite loops in circular dependencies
        if (processedFeatures.contains(featureCode)) {
            return;
        }
        processedFeatures.add(featureCode);

        // Get all features that depend on this feature (direct dependents)
        List<FeatureDependency> directDependents = getDependents(featureCode);

        for (FeatureDependency dep : directDependents) {
            String dependentCode = dep.getFeature().getCode();

            // Don't add the original feature to affected features
            if (dependentCode.equals(originalFeatureCode)) {
                continue;
            }

            if (!affectedFeatures.contains(dependentCode)) {
                affectedFeatures.add(dependentCode);

                // Recursively get dependents of this dependent
                getAllAffectedFeatureCodesRecursive(
                        dependentCode, affectedFeatures, processedFeatures, originalFeatureCode);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> getImpactAnalysis(
            String featureCode, String productCode, String releaseCode, String status) {
        // 1. Получить все затронутые features
        Set<String> allAffectedCodes = getAllAffectedFeatureCodes(featureCode);

        // 2. Отфильтровать features по критериям (product, release, status)
        Set<String> filteredAffectedCodes =
                filterFeaturesByCriteria(allAffectedCodes, productCode, releaseCode, status);

        List<FeatureDependency> allAffectedDependencies = new java.util.ArrayList<>();

        // 3. Добавить прямых зависимых с фильтрацией
        List<FeatureDependency> directDependents =
                getDependentsWithFiltering(featureCode, productCode, releaseCode, status);
        allAffectedDependencies.addAll(directDependents);

        // 4. Добавить зависимости отфильтрованных affected features (исключая дубли и круговые зависимости)
        Set<String> processedDependencies = new java.util.HashSet<>();
        for (String affectedCode : filteredAffectedCodes) {
            List<FeatureDependency> affectedDeps = getDependencies(affectedCode);
            for (FeatureDependency dep : affectedDeps) {
                // Исключить зависимости, где dependsOnFeature является исходной feature (круговые зависимости)
                if (featureCode.equals(dep.getDependsOnFeature().getCode())) {
                    continue;
                }

                String depKey = dep.getFeature().getCode() + "->"
                        + dep.getDependsOnFeature().getCode();
                if (!processedDependencies.contains(depKey)) {
                    processedDependencies.add(depKey);
                    allAffectedDependencies.add(dep);
                }
            }
        }

        return allAffectedDependencies;
    }

    private Set<String> filterFeaturesByCriteria(
            Set<String> featureCodes, String productCode, String releaseCode, String status) {
        if (productCode == null && releaseCode == null && status == null) {
            return featureCodes;
        }

        Set<String> filteredCodes = new java.util.HashSet<>();

        for (String featureCode : featureCodes) {
            Feature feature = featureRepository.findByCode(featureCode).orElse(null);
            if (feature == null) continue;

            boolean matches = true;

            if (productCode != null && !productCode.equals(feature.getProduct().getCode())) {
                matches = false;
            }

            if (releaseCode != null
                    && (feature.getRelease() == null
                            || !releaseCode.equals(feature.getRelease().getCode()))) {
                matches = false;
            }

            if (status != null && !status.equals(feature.getStatus().name())) {
                matches = false;
            }

            if (matches) {
                filteredCodes.add(featureCode);
            }
        }

        return filteredCodes;
    }
}
