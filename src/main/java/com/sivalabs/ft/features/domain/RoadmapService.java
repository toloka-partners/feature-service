package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.*;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.mappers.FeatureMapper;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import com.sivalabs.ft.features.domain.models.RiskLevel;
import com.sivalabs.ft.features.domain.models.TimelineAdherence;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoadmapService {
    private final ReleaseRepository releaseRepository;
    private final ReleaseMapper releaseMapper;
    private final FeatureMapper featureMapper;

    public RoadmapService(
            ReleaseRepository releaseRepository, ReleaseMapper releaseMapper, FeatureMapper featureMapper) {
        this.releaseRepository = releaseRepository;
        this.releaseMapper = releaseMapper;
        this.featureMapper = featureMapper;
    }

    @Transactional(readOnly = true)
    public RoadmapResponseDto getRoadmap(
            List<String> productCodes,
            List<String> statuses,
            LocalDate dateFrom,
            LocalDate dateTo,
            String groupBy,
            String owner) {

        // Validate owner exists if specified
        if (owner != null && !releaseRepository.existsByOwner(owner)) {
            return createEmptyRoadmapResponse(productCodes, statuses, dateFrom, dateTo, groupBy, owner);
        }

        // Convert status strings to enums
        List<ReleaseStatus> releaseStatuses = null;
        if (statuses != null && !statuses.isEmpty()) {
            releaseStatuses = statuses.stream()
                    .filter(Objects::nonNull)
                    .map(status -> {
                        try {
                            return ReleaseStatus.valueOf(status.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        // Convert dates to Instant
        Instant dateFromInstant =
                dateFrom != null ? dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant dateToInstant = dateTo != null
                ? dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        // Fetch releases with filters
        List<Release> releases = releaseRepository.findRoadmapReleases(productCodes, releaseStatuses, owner);

        // Filter by date range in Java since we moved it out of the query
        if (dateFromInstant != null || dateToInstant != null) {
            releases = releases.stream()
                    .filter(release -> {
                        Instant releaseDate = getReleaseSortDate(release);
                        boolean afterFrom = dateFromInstant == null || !releaseDate.isBefore(dateFromInstant);
                        boolean beforeTo = dateToInstant == null || !releaseDate.isAfter(dateToInstant);
                        return afterFrom && beforeTo;
                    })
                    .collect(Collectors.toList());
        }

        // Sort releases by date (descending)
        releases.sort(this::compareReleasesByDate);

        // Convert to roadmap items
        List<RoadmapItemDto> roadmapItems =
                releases.stream().map(this::convertToRoadmapItem).toList();

        // Group if requested
        if (groupBy != null) {
            roadmapItems = groupRoadmapItems(roadmapItems, groupBy);
        }

        // Create summary
        RoadmapSummaryDto summary = createSummary(roadmapItems);

        // Create applied filters
        RoadmapFiltersDto appliedFilters =
                new RoadmapFiltersDto(productCodes, statuses, dateFrom, dateTo, groupBy, owner);

        return new RoadmapResponseDto(roadmapItems, summary, appliedFilters);
    }

    private int compareReleasesByDate(Release r1, Release r2) {
        Instant date1 = getReleaseSortDate(r1);
        Instant date2 = getReleaseSortDate(r2);

        if (date1 == null && date2 == null) return 0;
        if (date1 == null) return 1;
        if (date2 == null) return -1;

        return date2.compareTo(date1); // Descending order
    }

    private Instant getReleaseSortDate(Release release) {
        if (release.getActualReleaseDate() != null) return release.getActualReleaseDate();
        if (release.getReleasedAt() != null) return release.getReleasedAt();
        if (release.getPlannedReleaseDate() != null) return release.getPlannedReleaseDate();
        return release.getCreatedAt();
    }

    private RoadmapItemDto convertToRoadmapItem(Release release) {
        ReleaseDto releaseDto = releaseMapper.toDto(release);

        List<FeatureDto> features =
                release.getFeatures().stream().map(featureMapper::toDto).toList();

        ProgressMetricsDto progressMetrics = calculateProgressMetrics(release.getFeatures());
        HealthIndicatorsDto healthIndicators = calculateHealthIndicators(release);

        return new RoadmapItemDto(releaseDto, progressMetrics, healthIndicators, features);
    }

    private ProgressMetricsDto calculateProgressMetrics(java.util.Set<Feature> features) {
        if (features == null || features.isEmpty()) {
            return new ProgressMetricsDto(0, 0, 0, 0, 0, 0.0);
        }

        Map<FeatureStatus, Long> statusCounts =
                features.stream().collect(Collectors.groupingBy(Feature::getStatus, Collectors.counting()));

        int total = features.size();
        int completed = statusCounts.getOrDefault(FeatureStatus.RELEASED, 0L).intValue();
        int inProgress =
                statusCounts.getOrDefault(FeatureStatus.IN_PROGRESS, 0L).intValue();
        int newFeatures = statusCounts.getOrDefault(FeatureStatus.NEW, 0L).intValue();
        int onHold = statusCounts.getOrDefault(FeatureStatus.ON_HOLD, 0L).intValue();

        double completionPercentage = total > 0 ? (double) completed / total * 100.0 : 0.0;

        return new ProgressMetricsDto(total, completed, inProgress, newFeatures, onHold, completionPercentage);
    }

    private HealthIndicatorsDto calculateHealthIndicators(Release release) {
        String riskLevel = calculateRiskLevel(release.getFeatures()).name();
        String timelineAdherence = calculateTimelineAdherence(release);

        return new HealthIndicatorsDto(riskLevel, timelineAdherence);
    }

    private RiskLevel calculateRiskLevel(java.util.Set<Feature> features) {
        if (features == null || features.isEmpty()) {
            return RiskLevel.ZERO;
        }

        long onHoldCount = features.stream()
                .mapToLong(f -> f.getStatus() == FeatureStatus.ON_HOLD ? 1 : 0)
                .sum();

        if (onHoldCount == 0) return RiskLevel.ZERO;

        double onHoldPercentage = (double) onHoldCount / features.size() * 100.0;

        if (onHoldPercentage > 30) return RiskLevel.HIGH;
        if (onHoldPercentage > 10) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private String calculateTimelineAdherence(Release release) {
        if (release.getPlannedReleaseDate() == null) {
            return null;
        }

        Instant actualDate =
                release.getActualReleaseDate() != null ? release.getActualReleaseDate() : release.getReleasedAt();

        Instant comparisonDate = actualDate != null ? actualDate : Instant.now();

        long daysDiff = ChronoUnit.DAYS.between(release.getPlannedReleaseDate(), comparisonDate);

        if (daysDiff <= 0) return TimelineAdherence.ON_SCHEDULE.name();
        if (daysDiff < 14) return TimelineAdherence.DELAYED.name();
        return TimelineAdherence.CRITICAL.name();
    }

    private List<RoadmapItemDto> groupRoadmapItems(List<RoadmapItemDto> items, String groupBy) {
        switch (groupBy.toLowerCase()) {
            case "productcode":
                return items.stream()
                        .sorted(Comparator.comparing(item -> item.release()
                                .code()
                                .substring(0, item.release().code().indexOf('-'))))
                        .toList();
            case "status":
                return items.stream()
                        .sorted(Comparator.comparing(
                                item -> item.release().status().name()))
                        .toList();
            case "owner":
                return items.stream()
                        .sorted(Comparator.comparing(item ->
                                item.release().owner() != null ? item.release().owner() : ""))
                        .toList();
            default:
                return items;
        }
    }

    private RoadmapSummaryDto createSummary(List<RoadmapItemDto> roadmapItems) {
        int totalReleases = roadmapItems.size();

        long completedReleases = roadmapItems.stream()
                .filter(item -> item.release().status() == ReleaseStatus.COMPLETED
                        || item.release().status() == ReleaseStatus.RELEASED)
                .count();

        long draftReleases = roadmapItems.stream()
                .filter(item -> item.release().status() == ReleaseStatus.DRAFT)
                .count();

        int totalFeatures = roadmapItems.stream()
                .mapToInt(item -> item.progressMetrics().totalFeatures())
                .sum();

        double overallCompletion = 0.0;
        if (totalFeatures > 0) {
            int totalCompleted = roadmapItems.stream()
                    .mapToInt(item -> item.progressMetrics().completedFeatures())
                    .sum();
            overallCompletion = (double) totalCompleted / totalFeatures * 100.0;
        }

        return new RoadmapSummaryDto(
                totalReleases, (int) completedReleases, (int) draftReleases, totalFeatures, overallCompletion);
    }

    private RoadmapResponseDto createEmptyRoadmapResponse(
            List<String> productCodes,
            List<String> statuses,
            LocalDate dateFrom,
            LocalDate dateTo,
            String groupBy,
            String owner) {

        RoadmapSummaryDto summary = new RoadmapSummaryDto(0, 0, 0, 0, 0.0);
        RoadmapFiltersDto appliedFilters =
                new RoadmapFiltersDto(productCodes, statuses, dateFrom, dateTo, groupBy, owner);

        return new RoadmapResponseDto(Collections.emptyList(), summary, appliedFilters);
    }
}
