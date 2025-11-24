package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.Commands.AssignFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.CreateFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.DeleteFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeaturePlanningCommand;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureService {
    private static final Logger log = LoggerFactory.getLogger(FeatureService.class);
    public static final String FEATURE_SEPARATOR = "-";
    private final FavoriteFeatureService favoriteFeatureService;
    private final ReleaseRepository releaseRepository;
    private final FeatureRepository featureRepository;
    private final ProductRepository productRepository;
    private final FavoriteFeatureRepository favoriteFeatureRepository;
    private final EventPublisher eventPublisher;
    private final FeatureMapper featureMapper;

    FeatureService(
            FavoriteFeatureService favoriteFeatureService,
            ReleaseRepository releaseRepository,
            FeatureRepository featureRepository,
            ProductRepository productRepository,
            FavoriteFeatureRepository favoriteFeatureRepository,
            EventPublisher eventPublisher,
            FeatureMapper featureMapper) {
        this.favoriteFeatureService = favoriteFeatureService;
        this.releaseRepository = releaseRepository;
        this.featureRepository = featureRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.favoriteFeatureRepository = favoriteFeatureRepository;
        this.featureMapper = featureMapper;
    }

    @Transactional(readOnly = true)
    public Optional<FeatureDto> findFeatureByCode(String username, String code) {
        Optional<Feature> optionalFeature = featureRepository.findByCode(code);
        if (optionalFeature.isEmpty()) {
            return Optional.empty();
        }
        List<FeatureDto> featureDtos = updateFavoriteStatus(List.of(optionalFeature.get()), username);
        return Optional.ofNullable(featureDtos.getFirst());
    }

    @Transactional(readOnly = true)
    public List<FeatureDto> findFeaturesByRelease(String username, String releaseCode) {
        List<Feature> features = featureRepository.findByReleaseCode(releaseCode);
        return updateFavoriteStatus(features, username);
    }

    @Transactional(readOnly = true)
    public List<FeatureDto> findFeaturesByReleaseWithFilters(
            String username,
            String releaseCode,
            FeaturePlanningStatus status,
            String owner,
            boolean overdue,
            boolean blocked) {
        List<Feature> features =
                featureRepository.findByReleaseCodeWithFilters(releaseCode, status, owner, overdue, blocked);
        return updateFavoriteStatus(features, username);
    }

    @Transactional(readOnly = true)
    public List<FeatureDto> findFeaturesByProduct(String username, String productCode) {
        List<Feature> features = featureRepository.findByProductCode(productCode);
        return updateFavoriteStatus(features, username);
    }

    private List<FeatureDto> updateFavoriteStatus(List<Feature> features, String username) {
        if (username == null || features.isEmpty()) {
            return features.stream().map(featureMapper::toDto).toList();
        }
        Set<String> featureCodes = features.stream().map(Feature::getCode).collect(Collectors.toSet());
        Map<String, Boolean> favoriteFeatures = favoriteFeatureService.getFavoriteFeatures(username, featureCodes);
        return features.stream()
                .map(feature -> {
                    var dto = featureMapper.toDto(feature);
                    dto.makeFavorite(favoriteFeatures.get(feature.getCode()));
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFeatureExists(String code) {
        return featureRepository.existsByCode(code);
    }

    @Transactional
    public String createFeature(CreateFeatureCommand cmd) {
        Product product = productRepository.findByCode(cmd.productCode()).orElseThrow();
        Release release = releaseRepository.findByCode(cmd.releaseCode()).orElse(null);
        String code = product.getPrefix() + FEATURE_SEPARATOR + featureRepository.getNextFeatureId();
        var feature = new Feature();
        feature.setProduct(product);
        feature.setRelease(release);
        feature.setCode(code);
        feature.setTitle(cmd.title());
        feature.setDescription(cmd.description());
        feature.setStatus(FeatureStatus.NEW);
        feature.setAssignedTo(cmd.assignedTo());
        feature.setCreatedBy(cmd.createdBy());
        feature.setCreatedAt(Instant.now());
        featureRepository.save(feature);
        eventPublisher.publishFeatureCreatedEvent(feature);
        return code;
    }

    @Transactional
    public void updateFeature(UpdateFeatureCommand cmd) {
        Feature feature = featureRepository.findByCode(cmd.code()).orElseThrow();
        feature.setTitle(cmd.title());
        feature.setDescription(cmd.description());
        if (cmd.releaseCode() != null) {
            Release release = releaseRepository.findByCode(cmd.releaseCode()).orElse(null);
            feature.setRelease(release);
        } else {
            feature.setRelease(null);
        }
        feature.setAssignedTo(cmd.assignedTo());
        feature.setStatus(cmd.status());
        feature.setUpdatedBy(cmd.updatedBy());
        feature.setUpdatedAt(Instant.now());
        featureRepository.save(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);
    }

    @Transactional
    public void assignFeatureToRelease(AssignFeatureCommand cmd) {
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));
        Release release = releaseRepository
                .findByCode(cmd.releaseCode())
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + cmd.releaseCode()));

        feature.setRelease(release);
        feature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        feature.setPlannedCompletionDate(cmd.plannedCompletionDate());
        feature.setFeatureOwner(cmd.featureOwner());
        feature.setPlanningNotes(cmd.notes());
        feature.setUpdatedBy(cmd.assignedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        log.info(
                "Feature {} assigned to release {} by {}",
                cmd.featureCode(),
                cmd.releaseCode(),
                cmd.assignedBy());
    }

    @Transactional
    public void updateFeaturePlanning(UpdateFeaturePlanningCommand cmd) {
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));

        // Validate status transition if status is being changed
        if (cmd.planningStatus() != null && feature.getPlanningStatus() != null) {
            feature.getPlanningStatus().validateTransition(cmd.planningStatus());
        }

        if (cmd.plannedCompletionDate() != null) {
            feature.setPlannedCompletionDate(cmd.plannedCompletionDate());
        }
        if (cmd.planningStatus() != null) {
            feature.setPlanningStatus(cmd.planningStatus());
        }
        if (cmd.featureOwner() != null) {
            feature.setFeatureOwner(cmd.featureOwner());
        }
        if (cmd.blockageReason() != null) {
            feature.setBlockageReason(cmd.blockageReason());
        }
        if (cmd.notes() != null) {
            feature.setPlanningNotes(cmd.notes());
        }
        feature.setUpdatedBy(cmd.updatedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        log.info(
                "Feature planning updated for {} by {} - Status: {}, Owner: {}",
                cmd.featureCode(),
                cmd.updatedBy(),
                cmd.planningStatus(),
                cmd.featureOwner());
    }

    @Transactional
    public void moveFeatureBetweenReleases(MoveFeatureCommand cmd) {
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));
        Release targetRelease = releaseRepository
                .findByCode(cmd.targetReleaseCode())
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + cmd.targetReleaseCode()));

        String sourceRelease = feature.getRelease() != null ? feature.getRelease().getCode() : "none";

        feature.setRelease(targetRelease);
        // Reset planning status when moving
        feature.setPlanningStatus(FeaturePlanningStatus.NOT_STARTED);
        feature.setUpdatedBy(cmd.movedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        log.info(
                "Feature {} moved from release {} to {} by {}. Rationale: {}",
                cmd.featureCode(),
                sourceRelease,
                cmd.targetReleaseCode(),
                cmd.movedBy(),
                cmd.rationale());
    }

    @Transactional
    public void removeFeatureFromRelease(RemoveFeatureCommand cmd) {
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));

        String releaseCode = feature.getRelease() != null ? feature.getRelease().getCode() : "none";

        feature.setRelease(null);
        feature.setPlanningStatus(null);
        feature.setPlannedCompletionDate(null);
        feature.setFeatureOwner(null);
        feature.setBlockageReason(null);
        feature.setPlanningNotes(null);
        feature.setUpdatedBy(cmd.removedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        log.info(
                "Feature {} removed from release {} by {}. Rationale: {}",
                cmd.featureCode(),
                releaseCode,
                cmd.removedBy(),
                cmd.rationale());
    }

    @Transactional
    public void deleteFeature(DeleteFeatureCommand cmd) {
        Feature feature = featureRepository.findByCode(cmd.code()).orElseThrow();
        favoriteFeatureRepository.deleteByFeatureCode(cmd.code());
        featureRepository.deleteByCode(cmd.code());
        eventPublisher.publishFeatureDeletedEvent(feature, cmd.deletedBy(), Instant.now());
    }
}
