package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureDependencyMapper;
import com.sivalabs.ft.features.domain.models.DependencyType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureDependencyService {
    private final FeatureRepository featureRepository;
    private final FeatureDependencyRepository featureDependencyRepository;
    private final FeatureDependencyMapper featureDependencyMapper;

    FeatureDependencyService(
            FeatureRepository featureRepository,
            FeatureDependencyRepository featureDependencyRepository,
            FeatureDependencyMapper featureDependencyMapper) {
        this.featureRepository = featureRepository;
        this.featureDependencyRepository = featureDependencyRepository;
        this.featureDependencyMapper = featureDependencyMapper;
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
    public List<FeatureDependencyDto> getFeatureDependencies(String featureCode) {
        featureRepository
                .findByCode(featureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found with code: " + featureCode));

        List<FeatureDependency> dependencies = featureDependencyRepository.findByFeature_Code(featureCode);
        return dependencies.stream().map(featureDependencyMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeatureDependencyDto> getFeatureDependents(String featureCode) {
        featureRepository
                .findByCode(featureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found with code: " + featureCode));

        List<FeatureDependency> dependents = featureDependencyRepository.findByDependsOnFeature_Code(featureCode);
        return dependents.stream().map(featureDependencyMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeatureDependencyDto> getFeatureImpact(
            String featureCode, String productCode, String releaseCode, String status) {
        featureRepository
                .findByCode(featureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found with code: " + featureCode));

        Set<String> visitedFeatures = new HashSet<>();
        Set<String> impactedFeatureCodes = new HashSet<>();

        findAllDependents(featureCode, visitedFeatures, impactedFeatureCodes);

        if (impactedFeatureCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Feature> impactedFeatures = featureRepository.findAllById(
                featureRepository.findByCodeIn(new ArrayList<>(impactedFeatureCodes)).stream()
                        .map(Feature::getId)
                        .collect(Collectors.toList()));

        // Apply filters
        if (productCode != null && !productCode.isBlank()) {
            impactedFeatures = impactedFeatures.stream()
                    .filter(f -> f.getProduct() != null
                            && productCode.equals(f.getProduct().getCode()))
                    .collect(Collectors.toList());
        }

        if (releaseCode != null && !releaseCode.isBlank()) {
            impactedFeatures = impactedFeatures.stream()
                    .filter(f -> f.getRelease() != null
                            && releaseCode.equals(f.getRelease().getCode()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            try {
                FeatureStatus featureStatus = FeatureStatus.valueOf(status.toUpperCase());
                impactedFeatures = impactedFeatures.stream()
                        .filter(f -> featureStatus.equals(f.getStatus()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid feature status: " + status);
            }
        }

        // Convert to DTOs with dependency information
        List<FeatureDependencyDto> results = new ArrayList<>();
        for (Feature impactedFeature : impactedFeatures) {
            List<FeatureDependency> dependencies =
                    featureDependencyRepository.findByFeature_Code(impactedFeature.getCode());
            for (FeatureDependency dep : dependencies) {
                if (impactedFeatureCodes.contains(dep.getDependsOnFeature().getCode())
                        || featureCode.equals(dep.getDependsOnFeature().getCode())) {
                    results.add(featureDependencyMapper.toDto(dep));
                }
            }
        }

        return results;
    }

    private void findAllDependents(String featureCode, Set<String> visited, Set<String> dependents) {
        if (visited.contains(featureCode)) {
            return;
        }
        visited.add(featureCode);

        List<FeatureDependency> directDependents = featureDependencyRepository.findByDependsOnFeature_Code(featureCode);
        for (FeatureDependency dep : directDependents) {
            String dependentCode = dep.getFeature().getCode();
            if (!visited.contains(dependentCode)) {
                dependents.add(dependentCode);
                findAllDependents(dependentCode, visited, dependents);
            }
        }
    }
}
