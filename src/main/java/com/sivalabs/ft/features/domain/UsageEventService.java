package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.FeatureUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.ProductUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import com.sivalabs.ft.features.domain.entities.UsageEvent;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.UsageEventMapper;
import com.sivalabs.ft.features.domain.models.UsageEventType;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageEventService {
    private final UsageEventRepository usageEventRepository;
    private final FeatureRepository featureRepository;
    private final ProductRepository productRepository;
    private final UsageEventMapper usageEventMapper;

    UsageEventService(
            UsageEventRepository usageEventRepository,
            FeatureRepository featureRepository,
            ProductRepository productRepository,
            UsageEventMapper usageEventMapper) {
        this.usageEventRepository = usageEventRepository;
        this.featureRepository = featureRepository;
        this.productRepository = productRepository;
        this.usageEventMapper = usageEventMapper;
    }

    @Transactional
    public UsageEventDto createUsageEvent(
            String featureCode, String productCode, UsageEventType eventType, String metadata, String userId) {
        // Validate feature exists
        if (!featureRepository.existsByCode(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        // Validate product exists
        if (!productRepository.existsByCode(productCode)) {
            throw new ResourceNotFoundException("Product with code " + productCode + " not found");
        }

        UsageEvent usageEvent = new UsageEvent();
        usageEvent.setFeatureCode(featureCode);
        usageEvent.setProductCode(productCode);
        usageEvent.setUserId(userId);
        usageEvent.setEventType(eventType);
        usageEvent.setMetadata(metadata);
        usageEvent.setCreatedAt(Instant.now());

        UsageEvent saved = usageEventRepository.save(usageEvent);
        return usageEventMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public FeatureUsageStatsDto getFeatureUsageStats(
            String featureCode, UsageEventType eventType, Instant startDate, Instant endDate) {
        // Validate feature exists
        if (!featureRepository.existsByCode(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        // Get events for calculating event type distribution
        List<UsageEvent> events =
                usageEventRepository.findByFeatureCodeWithFilters(featureCode, null, startDate, endDate);

        // Calculate statistics
        Long totalEvents = usageEventRepository.countByFeatureCode(featureCode, startDate, endDate);
        Long uniqueUsers = usageEventRepository.countDistinctUsersByFeatureCode(featureCode, startDate, endDate);
        Map<String, Long> eventsByType = calculateEventsByType(events);
        Instant firstEventAt = usageEventRepository.findFirstEventTimeByFeatureCode(featureCode, startDate, endDate);
        Instant lastEventAt = usageEventRepository.findLastEventTimeByFeatureCode(featureCode, startDate, endDate);

        return new FeatureUsageStatsDto(featureCode, totalEvents, uniqueUsers, eventsByType, firstEventAt, lastEventAt);
    }

    @Transactional(readOnly = true)
    public ProductUsageStatsDto getProductUsageStats(
            String productCode, UsageEventType eventType, Instant startDate, Instant endDate) {
        // Validate product exists
        if (!productRepository.existsByCode(productCode)) {
            throw new ResourceNotFoundException("Product with code " + productCode + " not found");
        }

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        // Get events for calculating event type distribution
        List<UsageEvent> events =
                usageEventRepository.findByProductCodeWithFilters(productCode, null, startDate, endDate);

        // Calculate statistics
        Long totalEvents = usageEventRepository.countByProductCode(productCode, startDate, endDate);
        Long uniqueUsers = usageEventRepository.countDistinctUsersByProductCode(productCode, startDate, endDate);
        Long uniqueFeatures = usageEventRepository.countDistinctFeaturesByProductCode(productCode, startDate, endDate);
        Map<String, Long> eventsByType = calculateEventsByType(events);
        Instant firstEventAt = usageEventRepository.findFirstEventTimeByProductCode(productCode, startDate, endDate);
        Instant lastEventAt = usageEventRepository.findLastEventTimeByProductCode(productCode, startDate, endDate);

        return new ProductUsageStatsDto(
                productCode, totalEvents, uniqueUsers, uniqueFeatures, eventsByType, firstEventAt, lastEventAt);
    }

    @Transactional(readOnly = true)
    public List<UsageEventDto> getFeatureUsageEvents(
            String featureCode, UsageEventType eventType, Instant startDate, Instant endDate) {
        // Validate feature exists
        if (!featureRepository.existsByCode(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        List<UsageEvent> events =
                usageEventRepository.findByFeatureCodeWithFilters(featureCode, eventType, startDate, endDate);
        return events.stream().map(usageEventMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UsageEventDto> getProductUsageEvents(
            String productCode, UsageEventType eventType, Instant startDate, Instant endDate) {
        // Validate product exists
        if (!productRepository.existsByCode(productCode)) {
            throw new ResourceNotFoundException("Product with code " + productCode + " not found");
        }

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }

        List<UsageEvent> events =
                usageEventRepository.findByProductCodeWithFilters(productCode, eventType, startDate, endDate);
        return events.stream().map(usageEventMapper::toDto).toList();
    }

    private Map<String, Long> calculateEventsByType(List<UsageEvent> events) {
        Map<String, Long> eventsByType = new HashMap<>();
        for (UsageEvent event : events) {
            String type = event.getEventType().name();
            eventsByType.put(type, eventsByType.getOrDefault(type, 0L) + 1);
        }
        return eventsByType;
    }
}
