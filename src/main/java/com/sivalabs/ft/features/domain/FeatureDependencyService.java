package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.models.DependencyType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeatureDependencyService {
    private static final Logger log = LoggerFactory.getLogger(FeatureDependencyService.class);

    private final FeatureDependencyRepository featureDependencyRepository;
    private final FeatureRepository featureRepository;
    private final EventPublisher eventPublisher;

    public FeatureDependencyService(
            FeatureDependencyRepository featureDependencyRepository,
            FeatureRepository featureRepository,
            EventPublisher eventPublisher) {
        this.featureDependencyRepository = featureDependencyRepository;
        this.featureRepository = featureRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> findDependenciesByFeatureCode(String featureCode) {
        return featureDependencyRepository.findByFeature_Code(featureCode);
    }

    @Transactional(readOnly = true)
    public List<FeatureDependency> findDependentFeatures(String featureCode) {
        return featureDependencyRepository.findByDependsOnFeature_Code(featureCode);
    }

    @Transactional(readOnly = true)
    public Optional<FeatureDependency> findDependency(String featureCode, String dependsOnFeatureCode) {
        return featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(
                featureCode, dependsOnFeatureCode);
    }

    public FeatureDependency createDependency(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String createdBy) {

        if (featureCode.equals(dependsOnFeatureCode)) {
            throw new BadRequestException("A feature cannot depend on itself");
        }

        Feature feature = featureRepository
                .findByCode(featureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + featureCode));

        Feature dependsOnFeature = featureRepository
                .findByCode(dependsOnFeatureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Target feature not found: " + dependsOnFeatureCode));

        Optional<FeatureDependency> existingDependency =
                featureDependencyRepository.findByFeature_CodeAndDependsOnFeature_Code(
                        featureCode, dependsOnFeatureCode);

        if (existingDependency.isPresent()) {
            throw new BadRequestException("Dependency already exists between these features");
        }

        validateNoCyclicDependency(featureCode, dependsOnFeatureCode);

        FeatureDependency dependency = new FeatureDependency();
        dependency.setFeature(feature);
        dependency.setDependsOnFeature(dependsOnFeature);
        dependency.setDependencyType(dependencyType);
        dependency.setNotes(notes);
        dependency.setCreatedAt(Instant.now());

        FeatureDependency saved = featureDependencyRepository.save(dependency);

        log.info(
                "Created dependency: {} -> {} (type: {}) by user: {}",
                featureCode,
                dependsOnFeatureCode,
                dependencyType,
                createdBy);

        eventPublisher.publishDependencyCreatedEvent(saved, createdBy);

        return saved;
    }

    public FeatureDependency updateDependency(
            String featureCode,
            String dependsOnFeatureCode,
            DependencyType dependencyType,
            String notes,
            String updatedBy) {

        FeatureDependency dependency = featureDependencyRepository
                .findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependency not found between " + featureCode + " and " + dependsOnFeatureCode));

        dependency.setDependencyType(dependencyType);
        dependency.setNotes(notes);

        FeatureDependency saved = featureDependencyRepository.save(dependency);

        log.info(
                "Updated dependency: {} -> {} (type: {}) by user: {}",
                featureCode,
                dependsOnFeatureCode,
                dependencyType,
                updatedBy);

        eventPublisher.publishDependencyUpdatedEvent(saved, updatedBy);

        return saved;
    }

    public void deleteDependency(String featureCode, String dependsOnFeatureCode, String deletedBy) {
        FeatureDependency dependency = featureDependencyRepository
                .findByFeature_CodeAndDependsOnFeature_Code(featureCode, dependsOnFeatureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependency not found between " + featureCode + " and " + dependsOnFeatureCode));

        DependencyType dependencyType = dependency.getDependencyType();
        String notes = dependency.getNotes();

        featureDependencyRepository.delete(dependency);

        log.info("Deleted dependency: {} -> {} by user: {}", featureCode, dependsOnFeatureCode, deletedBy);

        eventPublisher.publishDependencyDeletedEvent(
                featureCode, dependsOnFeatureCode, dependencyType, notes, deletedBy);
    }

    private void validateNoCyclicDependency(String featureCode, String dependsOnFeatureCode) {
        if (hasCyclicDependency(featureCode, dependsOnFeatureCode)) {
            throw new BadRequestException("Creating this dependency would result in a cyclic dependency");
        }
    }

    private boolean hasCyclicDependency(String featureCode, String targetFeatureCode) {
        return hasCyclicDependencyRecursive(targetFeatureCode, featureCode, new java.util.HashSet<>());
    }

    private boolean hasCyclicDependencyRecursive(String currentCode, String targetCode, java.util.Set<String> visited) {
        if (currentCode.equals(targetCode)) {
            return true;
        }

        if (visited.contains(currentCode)) {
            return false;
        }

        visited.add(currentCode);

        List<FeatureDependency> dependencies = featureDependencyRepository.findByFeature_Code(currentCode);
        for (FeatureDependency dependency : dependencies) {
            if (hasCyclicDependencyRecursive(dependency.getDependsOnFeature().getCode(), targetCode, visited)) {
                return true;
            }
        }

        return false;
    }
}
