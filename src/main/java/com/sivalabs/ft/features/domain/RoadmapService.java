package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.*;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoadmapService {
    private final ReleaseRepository releaseRepository;
    private final FeatureRepository featureRepository;
    private final ProductRepository productRepository;
    private final FavoriteFeatureService favoriteFeatureService;
    private final ReleaseMapper releaseMapper;
    private final FeatureMapper featureMapper;

    RoadmapService(
            ReleaseRepository releaseRepository,
            FeatureRepository featureRepository,
            ProductRepository productRepository,
            FavoriteFeatureService favoriteFeatureService,
            ReleaseMapper releaseMapper,
            FeatureMapper featureMapper) {
        this.releaseRepository = releaseRepository;
        this.featureRepository = featureRepository;
        this.productRepository = productRepository;
        this.favoriteFeatureService = favoriteFeatureService;
        this.releaseMapper = releaseMapper;
        this.featureMapper = featureMapper;
    }

    @Transactional(readOnly = true)
    public RoadmapResponseDto getRoadmap(
            String productCode,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String owner,
            String username) {
        List<Release> releases = getReleases(productCode, startDate, endDate, includeCompleted);
        List<RoadmapItemDto> roadmapItems = buildRoadmapItems(releases, username);

        if (groupBy != null) {
            roadmapItems = applyGrouping(roadmapItems, groupBy);
        }

        if (owner != null) {
            roadmapItems = filterByOwner(roadmapItems, owner);
        }

        RoadmapSummaryDto summary = calculateSummary(roadmapItems);
        AppliedFiltersDto filters =
                new AppliedFiltersDto(productCode, startDate, endDate, includeCompleted, groupBy, owner);

        return new RoadmapResponseDto(roadmapItems, summary, filters);
    }

    @Transactional(readOnly = true)
    public MultiProductRoadmapResponseDto getMultiProductRoadmap(
            LocalDate startDate, LocalDate endDate, Boolean includeCompleted, GroupBy groupBy, String username) {
        List<Product> allProducts = productRepository.findAll();
        List<ProductRoadmapDto> productRoadmaps = allProducts.stream()
                .map(product -> {
                    RoadmapResponseDto roadmap = getRoadmap(
                            product.getCode(), startDate, endDate, includeCompleted, groupBy, null, username);
                    return new ProductRoadmapDto(
                            product.getId(),
                            product.getName(),
                            product.getCode(),
                            roadmap.roadmapItems(),
                            roadmap.summary(),
                            roadmap.appliedFilters());
                })
                .toList();

        return new MultiProductRoadmapResponseDto(productRoadmaps);
    }

    @Transactional(readOnly = true)
    public RoadmapByOwnerResponseDto getRoadmapByOwner(
            String owner,
            String productCode,
            LocalDate startDate,
            LocalDate endDate,
            Boolean includeCompleted,
            GroupBy groupBy,
            String username) {
        RoadmapResponseDto roadmap =
                getRoadmap(productCode, startDate, endDate, includeCompleted, groupBy, owner, username);
        return new RoadmapByOwnerResponseDto(
                owner, roadmap.roadmapItems(), roadmap.summary(), roadmap.appliedFilters());
    }

    private List<Release> getReleases(
            String productCode, LocalDate startDate, LocalDate endDate, Boolean includeCompleted) {
        List<Release> releases;
        if (productCode != null) {
            releases = releaseRepository.findByProductCode(productCode);
        } else {
            releases = releaseRepository.findAll();
        }

        return releases.stream()
                .filter(release -> {
                    if (includeCompleted != null && !includeCompleted) {
                        return release.getStatus() != ReleaseStatus.RELEASED;
                    }
                    return true;
                })
                .filter(release -> {
                    if (startDate != null && release.getCreatedAt() != null) {
                        LocalDate createdDate = release.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        if (createdDate.isBefore(startDate)) {
                            return false;
                        }
                    }
                    if (endDate != null && release.getCreatedAt() != null) {
                        LocalDate createdDate = release.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        if (createdDate.isAfter(endDate)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private List<RoadmapItemDto> buildRoadmapItems(List<Release> releases, String username) {
        return releases.stream()
                .map(release -> {
                    ReleaseDto releaseDto = releaseMapper.toDto(release);
                    List<Feature> features = featureRepository.findByReleaseCode(release.getCode());
                    List<FeatureDto> featureDtos = convertToFeatureDtos(features, username);

                    ProgressMetricsDto progressMetrics = calculateProgressMetrics(features);
                    HealthIndicatorsDto healthIndicators = calculateHealthIndicators(release, features);

                    return new RoadmapItemDto(releaseDto, progressMetrics, healthIndicators, featureDtos);
                })
                .toList();
    }

    private List<FeatureDto> convertToFeatureDtos(List<Feature> features, String username) {
        if (username == null || features.isEmpty()) {
            return features.stream().map(featureMapper::toDto).toList();
        }
        Set<String> featureCodes = features.stream().map(Feature::getCode).collect(Collectors.toSet());
        Map<String, Boolean> favoriteFeatures = favoriteFeatureService.getFavoriteFeatures(username, featureCodes);
        return features.stream()
                .map(feature -> {
                    var dto = featureMapper.toDto(feature);
                    return dto.makeFavorite(favoriteFeatures.getOrDefault(feature.getCode(), false));
                })
                .toList();
    }

    private ProgressMetricsDto calculateProgressMetrics(List<Feature> features) {
        int total = features.size();
        long completed = features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.RELEASED)
                .count();
        long inProgress = features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.IN_PROGRESS)
                .count();
        long newFeatures = features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.NEW)
                .count();
        long onHold = features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.ON_HOLD)
                .count();

        double completionPercentage = total > 0 ? (completed * 100.0 / total) : 0.0;

        return new ProgressMetricsDto(
                total, (int) completed, (int) inProgress, (int) newFeatures, (int) onHold, completionPercentage);
    }

    private HealthIndicatorsDto calculateHealthIndicators(Release release, List<Feature> features) {
        TimelineAdherence timelineAdherence = calculateTimelineAdherence(release);
        RiskLevel riskLevel = calculateRiskLevel(features, timelineAdherence);
        int blockedFeatures = (int) features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.ON_HOLD)
                .count();

        return new HealthIndicatorsDto(timelineAdherence, riskLevel, blockedFeatures);
    }

    private TimelineAdherence calculateTimelineAdherence(Release release) {
        if (release.getStatus() == ReleaseStatus.RELEASED) {
            return TimelineAdherence.ON_SCHEDULE;
        }

        Instant createdAt = release.getCreatedAt();
        if (createdAt == null) {
            return TimelineAdherence.ON_SCHEDULE;
        }

        long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, Instant.now());

        if (daysSinceCreation > 180) {
            return TimelineAdherence.CRITICAL;
        } else if (daysSinceCreation > 30) {
            return TimelineAdherence.DELAYED;
        } else {
            return TimelineAdherence.ON_SCHEDULE;
        }
    }

    private RiskLevel calculateRiskLevel(List<Feature> features, TimelineAdherence timelineAdherence) {
        if (features.isEmpty()) {
            return RiskLevel.LOW;
        }

        double completionPercentage = features.stream()
                        .filter(f -> f.getStatus() == FeatureStatus.RELEASED)
                        .count()
                * 100.0
                / features.size();
        long blockedCount = features.stream()
                .filter(f -> f.getStatus() == FeatureStatus.ON_HOLD)
                .count();
        double blockedPercentage = blockedCount * 100.0 / features.size();

        if (timelineAdherence == TimelineAdherence.CRITICAL || blockedPercentage > 30 || completionPercentage < 20) {
            return RiskLevel.HIGH;
        } else if (timelineAdherence == TimelineAdherence.DELAYED
                || blockedPercentage > 10
                || completionPercentage < 50) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private List<RoadmapItemDto> applyGrouping(List<RoadmapItemDto> roadmapItems, GroupBy groupBy) {
        return switch (groupBy) {
            case PRODUCT -> roadmapItems;
            case STATUS -> groupByStatus(roadmapItems);
            case ASSIGNEE -> groupByAssignee(roadmapItems);
        };
    }

    private List<RoadmapItemDto> groupByStatus(List<RoadmapItemDto> roadmapItems) {
        return roadmapItems;
    }

    private List<RoadmapItemDto> groupByAssignee(List<RoadmapItemDto> roadmapItems) {
        return roadmapItems;
    }

    private List<RoadmapItemDto> filterByOwner(List<RoadmapItemDto> roadmapItems, String owner) {
        return roadmapItems.stream()
                .map(item -> {
                    List<FeatureDto> filteredFeatures = item.features().stream()
                            .filter(f -> owner.equals(f.assignedTo()))
                            .toList();
                    if (filteredFeatures.isEmpty()) {
                        return null;
                    }
                    ProgressMetricsDto recalculatedMetrics = recalculateProgressMetrics(filteredFeatures);
                    return new RoadmapItemDto(
                            item.release(), recalculatedMetrics, item.healthIndicators(), filteredFeatures);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private ProgressMetricsDto recalculateProgressMetrics(List<FeatureDto> features) {
        int total = features.size();
        long completed = features.stream()
                .filter(f -> f.status() == FeatureStatus.RELEASED)
                .count();
        long inProgress = features.stream()
                .filter(f -> f.status() == FeatureStatus.IN_PROGRESS)
                .count();
        long newFeatures =
                features.stream().filter(f -> f.status() == FeatureStatus.NEW).count();
        long onHold = features.stream()
                .filter(f -> f.status() == FeatureStatus.ON_HOLD)
                .count();

        double completionPercentage = total > 0 ? (completed * 100.0 / total) : 0.0;

        return new ProgressMetricsDto(
                total, (int) completed, (int) inProgress, (int) newFeatures, (int) onHold, completionPercentage);
    }

    private RoadmapSummaryDto calculateSummary(List<RoadmapItemDto> roadmapItems) {
        int totalReleases = roadmapItems.size();
        long completedReleases = roadmapItems.stream()
                .filter(item -> item.release().status() == ReleaseStatus.RELEASED)
                .count();
        long draftReleases = roadmapItems.stream()
                .filter(item -> item.release().status() == ReleaseStatus.DRAFT)
                .count();
        int totalFeatures =
                roadmapItems.stream().mapToInt(item -> item.features().size()).sum();

        long totalCompletedFeatures = roadmapItems.stream()
                .flatMap(item -> item.features().stream())
                .filter(f -> f.status() == FeatureStatus.RELEASED)
                .count();

        double overallCompletionPercentage = totalFeatures > 0 ? (totalCompletedFeatures * 100.0 / totalFeatures) : 0.0;

        return new RoadmapSummaryDto(
                totalReleases,
                (int) completedReleases,
                (int) draftReleases,
                totalFeatures,
                overallCompletionPercentage);
    }
}
