package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.FeatureUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.ProductUsageStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import com.sivalabs.ft.features.domain.entities.UsageEvent;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.UsageEventMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageEventService {
    private final UsageEventRepository usageEventRepository;
    private final FeatureService featureService;
    private final ProductService productService;
    private final UsageEventMapper usageEventMapper;

    UsageEventService(
            UsageEventRepository usageEventRepository,
            FeatureService featureService,
            ProductService productService,
            UsageEventMapper usageEventMapper) {
        this.usageEventRepository = usageEventRepository;
        this.featureService = featureService;
        this.productService = productService;
        this.usageEventMapper = usageEventMapper;
    }

    @Transactional
    public UsageEventDto createUsageEvent(
            String featureCode, String productCode, String eventType, String metadata, String userId) {
        // Validate feature exists
        if (!featureService.isFeatureExists(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        // Validate product exists
        if (!productService.existsByCode(productCode)) {
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
            String featureCode, String eventType, Instant startDate, Instant endDate) {
        // Validate feature exists
        if (!featureService.isFeatureExists(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        validateDateRange(startDate, endDate);

        // Get events for calculating statistics
        List<UsageEvent> events =
                usageEventRepository.findByFeatureCodeWithFilters(featureCode, eventType, startDate, endDate);

        // Calculate statistics from filtered events
        Long totalEvents = (long) events.size();
        Long uniqueUsers = events.stream().map(UsageEvent::getUserId).distinct().count();
        Map<String, Long> eventsByType = calculateEventsByType(events);
        Instant firstEventAt = events.stream()
                .map(UsageEvent::getCreatedAt)
                .min(Instant::compareTo)
                .orElse(null);
        Instant lastEventAt = events.stream()
                .map(UsageEvent::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null);

        return new FeatureUsageStatsDto(featureCode, totalEvents, uniqueUsers, eventsByType, firstEventAt, lastEventAt);
    }

    @Transactional(readOnly = true)
    public ProductUsageStatsDto getProductUsageStats(
            String productCode, String eventType, Instant startDate, Instant endDate) {
        // Validate product exists
        if (!productService.existsByCode(productCode)) {
            throw new ResourceNotFoundException("Product with code " + productCode + " not found");
        }

        validateDateRange(startDate, endDate);

        // Get events for calculating statistics
        List<UsageEvent> events =
                usageEventRepository.findByProductCodeWithFilters(productCode, eventType, startDate, endDate);

        // Calculate statistics from filtered events
        Long totalEvents = (long) events.size();
        Long uniqueUsers = events.stream().map(UsageEvent::getUserId).distinct().count();
        Long uniqueFeatures =
                events.stream().map(UsageEvent::getFeatureCode).distinct().count();
        Map<String, Long> eventsByType = calculateEventsByType(events);
        Instant firstEventAt = events.stream()
                .map(UsageEvent::getCreatedAt)
                .min(Instant::compareTo)
                .orElse(null);
        Instant lastEventAt = events.stream()
                .map(UsageEvent::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null);

        return new ProductUsageStatsDto(
                productCode, totalEvents, uniqueUsers, uniqueFeatures, eventsByType, firstEventAt, lastEventAt);
    }

    @Transactional(readOnly = true)
    public List<UsageEventDto> getFeatureUsageEvents(
            String featureCode, String eventType, Instant startDate, Instant endDate) {
        // Validate feature exists
        if (!featureService.isFeatureExists(featureCode)) {
            throw new ResourceNotFoundException("Feature with code " + featureCode + " not found");
        }

        validateDateRange(startDate, endDate);

        List<UsageEvent> events =
                usageEventRepository.findByFeatureCodeWithFilters(featureCode, eventType, startDate, endDate);
        return events.stream().map(usageEventMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UsageEventDto> getProductUsageEvents(
            String productCode, String eventType, Instant startDate, Instant endDate) {
        // Validate product exists
        if (!productService.existsByCode(productCode)) {
            throw new ResourceNotFoundException("Product with code " + productCode + " not found");
        }

        validateDateRange(startDate, endDate);

        List<UsageEvent> events =
                usageEventRepository.findByProductCodeWithFilters(productCode, eventType, startDate, endDate);
        return events.stream().map(usageEventMapper::toDto).toList();
    }

    private void validateDateRange(Instant startDate, Instant endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
    }

    private Map<String, Long> calculateEventsByType(List<UsageEvent> events) {
        Map<String, Long> eventsByType = new HashMap<>();
        for (UsageEvent event : events) {
            String type = event.getEventType();
            eventsByType.put(type, eventsByType.getOrDefault(type, 0L) + 1);
        }
        return eventsByType;
    }
}
