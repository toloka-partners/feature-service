package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.Commands.AssignFeatureToReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.CreateFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.DeleteFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.MoveFeatureBetweenReleasesCommand;
import com.sivalabs.ft.features.domain.Commands.RemoveFeatureFromReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateFeaturePlanningCommand;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.models.FeaturePlanningStatus;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
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

    private static final Map<FeaturePlanningStatus, Set<FeaturePlanningStatus>> ALLOWED_TRANSITIONS = Map.of(
            FeaturePlanningStatus.NOT_STARTED,
                    EnumSet.of(FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.BLOCKED),
            FeaturePlanningStatus.IN_PROGRESS,
                    EnumSet.of(
                            FeaturePlanningStatus.DONE,
                            FeaturePlanningStatus.BLOCKED,
                            FeaturePlanningStatus.NOT_STARTED),
            FeaturePlanningStatus.BLOCKED,
                    EnumSet.of(FeaturePlanningStatus.IN_PROGRESS, FeaturePlanningStatus.NOT_STARTED),
            FeaturePlanningStatus.DONE,
                    EnumSet.of(FeaturePlanningStatus.NOT_STARTED, FeaturePlanningStatus.IN_PROGRESS));

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
    public void deleteFeature(DeleteFeatureCommand cmd) {
        Feature feature = featureRepository.findByCode(cmd.code()).orElseThrow();
        favoriteFeatureRepository.deleteByFeatureCode(cmd.code());
        featureRepository.deleteByCode(cmd.code());
        eventPublisher.publishFeatureDeletedEvent(feature, cmd.deletedBy(), Instant.now());
    }

    public void validatePlanningStatusTransition(FeaturePlanningStatus currentStatus, FeaturePlanningStatus newStatus) {
        if (currentStatus == null || currentStatus == newStatus) {
            return;
        }
        Set<FeaturePlanningStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedStatuses == null || !allowedStatuses.contains(newStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    @Transactional
    public void assignFeatureToRelease(AssignFeatureToReleaseCommand cmd) {
        log.info("Assigning feature {} to release {} by {}", cmd.featureCode(), cmd.releaseCode(), cmd.assignedBy());
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));
        Release release = releaseRepository
                .findByCode(cmd.releaseCode())
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + cmd.releaseCode()));

        feature.setRelease(release);
        feature.setPlannedCompletionDate(cmd.plannedCompletionDate());
        feature.setFeatureOwner(cmd.featureOwner());
        if (cmd.notes() != null) {
            feature.setNotes(cmd.notes());
        }
        feature.setUpdatedBy(cmd.assignedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);
    }

    @Transactional
    public void updateFeaturePlanning(UpdateFeaturePlanningCommand cmd) {
        log.info(
                "Updating feature planning for feature {} in release {} by {}",
                cmd.featureCode(),
                cmd.releaseCode(),
                cmd.updatedBy());
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));

        if (feature.getRelease() == null || !feature.getRelease().getCode().equals(cmd.releaseCode())) {
            throw new BadRequestException(
                    "Feature " + cmd.featureCode() + " is not assigned to release " + cmd.releaseCode());
        }

        if (cmd.planningStatus() != null) {
            validatePlanningStatusTransition(feature.getPlanningStatus(), cmd.planningStatus());
            feature.setPlanningStatus(cmd.planningStatus());
        }

        if (cmd.plannedCompletionDate() != null) {
            feature.setPlannedCompletionDate(cmd.plannedCompletionDate());
        }
        if (cmd.featureOwner() != null) {
            feature.setFeatureOwner(cmd.featureOwner());
        }
        if (cmd.blockageReason() != null) {
            feature.setBlockageReason(cmd.blockageReason());
        }

        feature.setUpdatedBy(cmd.updatedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);
    }

    @Transactional
    public void moveFeatureBetweenReleases(MoveFeatureBetweenReleasesCommand cmd) {
        log.info(
                "Moving feature {} to release {} by {}: {}",
                cmd.featureCode(),
                cmd.targetReleaseCode(),
                cmd.movedBy(),
                cmd.rationale());
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));
        Release targetRelease = releaseRepository
                .findByCode(cmd.targetReleaseCode())
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + cmd.targetReleaseCode()));

        feature.setRelease(targetRelease);

        // Append rationale to notes
        String currentNotes = feature.getNotes() != null ? feature.getNotes() : "";
        String timestamp = Instant.now().toString();
        String noteEntry = String.format(
                "%s[%s] Moved to release %s by %s: %s",
                currentNotes.isEmpty() ? "" : currentNotes + "\n",
                timestamp,
                cmd.targetReleaseCode(),
                cmd.movedBy(),
                cmd.rationale());
        feature.setNotes(noteEntry);

        feature.setUpdatedBy(cmd.movedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);
    }

    @Transactional
    public void removeFeatureFromRelease(RemoveFeatureFromReleaseCommand cmd) {
        log.info(
                "Removing feature {} from release {} by {}: {}",
                cmd.featureCode(),
                cmd.releaseCode(),
                cmd.removedBy(),
                cmd.rationale());
        Feature feature = featureRepository
                .findByCode(cmd.featureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + cmd.featureCode()));

        if (feature.getRelease() == null || !feature.getRelease().getCode().equals(cmd.releaseCode())) {
            throw new BadRequestException(
                    "Feature " + cmd.featureCode() + " is not assigned to release " + cmd.releaseCode());
        }

        feature.setRelease(null);

        // Append rationale to notes
        String currentNotes = feature.getNotes() != null ? feature.getNotes() : "";
        String timestamp = Instant.now().toString();
        String noteEntry = String.format(
                "%s[%s] Removed from release %s by %s: %s",
                currentNotes.isEmpty() ? "" : currentNotes + "\n",
                timestamp,
                cmd.releaseCode(),
                cmd.removedBy(),
                cmd.rationale());
        feature.setNotes(noteEntry);

        feature.setUpdatedBy(cmd.removedBy());
        feature.setUpdatedAt(Instant.now());

        featureRepository.save(feature);
        eventPublisher.publishFeatureUpdatedEvent(feature);
    }

    @Transactional(readOnly = true)
    public List<FeatureDto> findFeaturesByReleaseWithFilters(
            String username,
            String releaseCode,
            FeaturePlanningStatus planningStatus,
            String owner,
            Boolean overdue,
            Boolean blocked) {
        List<Feature> features = featureRepository.findByReleaseCode(releaseCode);

        // Apply filters
        List<Feature> filteredFeatures = features.stream()
                .filter(f -> planningStatus == null || planningStatus.equals(f.getPlanningStatus()))
                .filter(f -> owner == null || owner.equals(f.getFeatureOwner()))
                .filter(f -> blocked == null || !blocked || FeaturePlanningStatus.BLOCKED.equals(f.getPlanningStatus()))
                .filter(f -> overdue == null
                        || !overdue
                        || (f.getPlannedCompletionDate() != null
                                && f.getPlannedCompletionDate().isBefore(LocalDate.now())
                                && !FeaturePlanningStatus.DONE.equals(f.getPlanningStatus())))
                .toList();

        return updateFavoriteStatus(filteredFeatures, username);
    }
}
