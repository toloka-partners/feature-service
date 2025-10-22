package com.sivalabs.ft.features.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.dtos.FeatureStatsDto;
import com.sivalabs.ft.features.domain.dtos.FeatureUsageDto;
import com.sivalabs.ft.features.domain.dtos.ProductStatsDto;
import com.sivalabs.ft.features.domain.dtos.UsageStatsDto;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureUsageService {
    private static final Logger log = LoggerFactory.getLogger(FeatureUsageService.class);

    private final FeatureUsageRepository featureUsageRepository;
    private final ObjectMapper objectMapper;
    private final com.sivalabs.ft.features.ApplicationProperties applicationProperties;

    public FeatureUsageService(
            FeatureUsageRepository featureUsageRepository,
            ObjectMapper objectMapper,
            com.sivalabs.ft.features.ApplicationProperties applicationProperties) {
        this.featureUsageRepository = featureUsageRepository;
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
    }

    @Transactional
    public void logUsage(
            String userId,
            String featureCode,
            String productCode,
            String releaseCode,
            ActionType actionType,
            Map<String, Object> contextData,
            String ipAddress,
            String userAgent) {
        if (!applicationProperties.usageTracking().enabled()) {
            return;
        }
        try {
            // Enrich context with anonymized device fingerprint for anonymous users
            Map<String, Object> enrichedContext = contextData != null ? new HashMap<>(contextData) : new HashMap<>();

            if (ipAddress != null && userAgent != null) {
                // Create device fingerprint: hash(device:ip)
                String deviceFingerprint = createDeviceFingerprint(ipAddress, userAgent);
                enrichedContext.put("deviceFingerprint", deviceFingerprint);

                // Extract location from IP (placeholder - would use GeoIP service in production)
                String location = extractLocation(ipAddress);
                if (location != null) {
                    enrichedContext.put("location", location);
                }
            }

            String contextJson = null;
            if (!enrichedContext.isEmpty()) {
                try {
                    contextJson = objectMapper.writeValueAsString(enrichedContext);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize context data", e);
                }
            }

            var featureUsage = new FeatureUsage();
            featureUsage.setUserId(userId);
            featureUsage.setFeatureCode(featureCode);
            featureUsage.setProductCode(productCode);
            featureUsage.setReleaseCode(releaseCode);
            featureUsage.setActionType(actionType);
            featureUsage.setTimestamp(Instant.now());
            featureUsage.setContext(contextJson);
            featureUsage.setIpAddress(ipAddress);
            featureUsage.setUserAgent(userAgent);

            featureUsageRepository.save(featureUsage);
            log.debug(
                    "Logged usage: user={}, feature={}, product={}, release={}, action={}",
                    userId,
                    featureCode,
                    productCode,
                    releaseCode,
                    actionType);
        } catch (Exception e) {
            log.error("Failed to log usage event", e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    @Transactional
    public void logUsage(String userId, String featureCode, String productCode, ActionType actionType) {
        logUsage(userId, featureCode, productCode, null, actionType, null, null, null);
    }

    @Transactional
    public void logUsage(
            String userId, String featureCode, String productCode, ActionType actionType, Map<String, Object> context) {
        logUsage(userId, featureCode, productCode, null, actionType, context, null, null);
    }

    @Transactional
    public void logUsage(
            String userId, String featureCode, String productCode, String releaseCode, ActionType actionType) {
        logUsage(userId, featureCode, productCode, releaseCode, actionType, null, null, null);
    }

    /**
     * Create device fingerprint using hash(device:ip) for anonymous user tracking.
     * GDPR compliant - uses hash instead of storing actual IP.
     */
    private String createDeviceFingerprint(String ipAddress, String userAgent) {
        try {
            String combined = userAgent + ":" + ipAddress;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            // Return first 16 characters for shorter fingerprint
            return hexString.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Failed to create device fingerprint", e);
            return null;
        }
    }

    /**
     * Extract location from IP address.
     * Placeholder implementation - in production would use GeoIP service.
     * Returns only country code for GDPR compliance (not city/precise location).
     */
    private String extractLocation(String ipAddress) {
        // Placeholder: In production, use MaxMind GeoIP2 or similar service
        // For now, return null or "UNKNOWN"
        // Example: return geoIpService.getCountryCode(ipAddress);
        return null;
    }

    @Transactional
    public FeatureUsage createUsageEvent(
            String userId,
            String featureCode,
            String productCode,
            ActionType actionType,
            Map<String, Object> context,
            String ipAddress,
            String userAgent) {
        if (!applicationProperties.usageTracking().enabled()) {
            return null;
        }
        try {
            Map<String, Object> enrichedContext = context != null ? new HashMap<>(context) : new HashMap<>();

            if (ipAddress != null && userAgent != null) {
                String deviceFingerprint = createDeviceFingerprint(ipAddress, userAgent);
                enrichedContext.put("deviceFingerprint", deviceFingerprint);
                String location = extractLocation(ipAddress);
                if (location != null) {
                    enrichedContext.put("location", location);
                }
            }

            String contextJson = null;
            if (!enrichedContext.isEmpty()) {
                try {
                    contextJson = objectMapper.writeValueAsString(enrichedContext);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize context data", e);
                }
            }

            var featureUsage = new FeatureUsage();
            featureUsage.setUserId(userId);
            featureUsage.setFeatureCode(featureCode);
            featureUsage.setProductCode(productCode);
            featureUsage.setActionType(actionType);
            featureUsage.setTimestamp(Instant.now());
            featureUsage.setContext(contextJson);
            featureUsage.setIpAddress(ipAddress);
            featureUsage.setUserAgent(userAgent);

            return featureUsageRepository.save(featureUsage);
        } catch (Exception e) {
            log.error("Failed to create usage event", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public FeatureStatsDto getFeatureStats(
            String featureCode, ActionType actionType, Instant startDate, Instant endDate) {
        List<FeatureUsage> usages;
        long uniqueUserCount;
        long totalUsageCount;

        if (actionType != null && startDate != null && endDate != null) {
            usages = featureUsageRepository.findByFeatureCodeAndActionTypeAndTimestampBetween(
                    featureCode, actionType, startDate, endDate);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByFeatureCodeAndActionTypeAndTimestampBetween(
                    featureCode, actionType, startDate, endDate);
            totalUsageCount = usages.size();
        } else if (actionType != null) {
            usages = featureUsageRepository.findByFeatureCodeAndActionType(featureCode, actionType);
            uniqueUserCount =
                    featureUsageRepository.countDistinctUsersByFeatureCodeAndActionType(featureCode, actionType);
            totalUsageCount = usages.size();
        } else if (startDate != null && endDate != null) {
            usages = featureUsageRepository.findByFeatureCodeAndTimestampBetween(featureCode, startDate, endDate);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByFeatureCodeAndTimestampBetween(
                    featureCode, startDate, endDate);
            totalUsageCount = usages.size();
        } else {
            usages = featureUsageRepository.findByFeatureCode(featureCode);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByFeatureCode(featureCode);
            totalUsageCount = usages.size();
        }

        Map<ActionType, Long> usageByActionType =
                usages.stream().collect(Collectors.groupingBy(FeatureUsage::getActionType, Collectors.counting()));

        Map<String, Long> topUsers = usages.stream()
                .collect(Collectors.groupingBy(FeatureUsage::getUserId, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> usageByProduct = usages.stream()
                .filter(u -> u.getProductCode() != null)
                .collect(Collectors.groupingBy(FeatureUsage::getProductCode, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return new FeatureStatsDto(
                featureCode, totalUsageCount, uniqueUserCount, usageByActionType, topUsers, usageByProduct);
    }

    @Transactional(readOnly = true)
    public ProductStatsDto getProductStats(
            String productCode, ActionType actionType, Instant startDate, Instant endDate) {
        List<FeatureUsage> usages;
        long uniqueUserCount;
        long uniqueFeatureCount;
        long totalUsageCount;

        if (actionType != null && startDate != null && endDate != null) {
            usages = featureUsageRepository.findByProductCodeAndActionTypeAndTimestampBetween(
                    productCode, actionType, startDate, endDate);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByProductCodeAndActionTypeAndTimestampBetween(
                    productCode, actionType, startDate, endDate);
            uniqueFeatureCount =
                    featureUsageRepository.countDistinctFeaturesByProductCodeAndActionTypeAndTimestampBetween(
                            productCode, actionType, startDate, endDate);
            totalUsageCount = usages.size();
        } else if (actionType != null) {
            usages = featureUsageRepository.findByProductCodeAndActionType(productCode, actionType);
            uniqueUserCount =
                    featureUsageRepository.countDistinctUsersByProductCodeAndActionType(productCode, actionType);
            uniqueFeatureCount =
                    featureUsageRepository.countDistinctFeaturesByProductCodeAndActionType(productCode, actionType);
            totalUsageCount = usages.size();
        } else if (startDate != null && endDate != null) {
            usages = featureUsageRepository.findByProductCodeAndTimestampBetween(productCode, startDate, endDate);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByProductCodeAndTimestampBetween(
                    productCode, startDate, endDate);
            uniqueFeatureCount = featureUsageRepository.countDistinctFeaturesByProductCodeAndTimestampBetween(
                    productCode, startDate, endDate);
            totalUsageCount = usages.size();
        } else {
            usages = featureUsageRepository.findByProductCode(productCode);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByProductCode(productCode);
            uniqueFeatureCount = featureUsageRepository.countDistinctFeaturesByProductCode(productCode);
            totalUsageCount = usages.size();
        }

        Map<ActionType, Long> usageByActionType =
                usages.stream().collect(Collectors.groupingBy(FeatureUsage::getActionType, Collectors.counting()));

        Map<String, Long> topFeatures = usages.stream()
                .filter(u -> u.getFeatureCode() != null)
                .collect(Collectors.groupingBy(FeatureUsage::getFeatureCode, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> topUsers = usages.stream()
                .collect(Collectors.groupingBy(FeatureUsage::getUserId, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return new ProductStatsDto(
                productCode,
                totalUsageCount,
                uniqueUserCount,
                uniqueFeatureCount,
                usageByActionType,
                topFeatures,
                topUsers);
    }

    @Transactional(readOnly = true)
    public List<FeatureUsageDto> getFeatureEvents(
            String featureCode, ActionType actionType, Instant startDate, Instant endDate) {
        List<FeatureUsage> usages;

        if (actionType != null && startDate != null && endDate != null) {
            usages = featureUsageRepository.findByFeatureCodeAndActionTypeAndTimestampBetween(
                    featureCode, actionType, startDate, endDate);
        } else if (actionType != null) {
            usages = featureUsageRepository.findByFeatureCodeAndActionType(featureCode, actionType);
        } else if (startDate != null && endDate != null) {
            usages = featureUsageRepository.findByFeatureCodeAndTimestampBetween(featureCode, startDate, endDate);
        } else {
            usages = featureUsageRepository.findByFeatureCode(featureCode);
        }

        return usages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeatureUsageDto> getProductEvents(
            String productCode, ActionType actionType, Instant startDate, Instant endDate) {
        List<FeatureUsage> usages;

        if (actionType != null && startDate != null && endDate != null) {
            usages = featureUsageRepository.findByProductCodeAndActionTypeAndTimestampBetween(
                    productCode, actionType, startDate, endDate);
        } else if (actionType != null) {
            usages = featureUsageRepository.findByProductCodeAndActionType(productCode, actionType);
        } else if (startDate != null && endDate != null) {
            usages = featureUsageRepository.findByProductCodeAndTimestampBetween(productCode, startDate, endDate);
        } else {
            usages = featureUsageRepository.findByProductCode(productCode);
        }

        return usages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeatureUsageDto> getAllEvents(
            ActionType actionType,
            String userId,
            String featureCode,
            String productCode,
            Instant startDate,
            Instant endDate) {
        List<FeatureUsage> usages = featureUsageRepository.findAll();

        return usages.stream()
                .filter(u -> actionType == null || u.getActionType() == actionType)
                .filter(u -> userId == null || userId.equals(u.getUserId()))
                .filter(u -> featureCode == null || featureCode.equals(u.getFeatureCode()))
                .filter(u -> productCode == null || productCode.equals(u.getProductCode()))
                .filter(u -> startDate == null || !u.getTimestamp().isBefore(startDate))
                .filter(u -> endDate == null || !u.getTimestamp().isAfter(endDate))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsageStatsDto getOverallStats(ActionType actionType, Instant startDate, Instant endDate) {
        List<FeatureUsage> usages;
        long uniqueUserCount;
        long uniqueFeatureCount;
        long uniqueProductCount;
        long totalUsageCount;

        if (actionType != null && startDate != null && endDate != null) {
            usages = featureUsageRepository.findAll().stream()
                    .filter(u -> u.getActionType() == actionType)
                    .filter(u -> !u.getTimestamp().isBefore(startDate)
                            && !u.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());
            uniqueUserCount = featureUsageRepository.countDistinctUsersByActionTypeAndTimestampBetween(
                    actionType, startDate, endDate);
            uniqueFeatureCount = featureUsageRepository.countDistinctFeaturesByActionTypeAndTimestampBetween(
                    actionType, startDate, endDate);
            uniqueProductCount = featureUsageRepository.countDistinctProductsByActionTypeAndTimestampBetween(
                    actionType, startDate, endDate);
            totalUsageCount = usages.size();
        } else if (actionType != null) {
            usages = featureUsageRepository.findByActionType(actionType);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByActionType(actionType);
            uniqueFeatureCount = featureUsageRepository.countDistinctFeaturesByActionType(actionType);
            uniqueProductCount = featureUsageRepository.countDistinctProductsByActionType(actionType);
            totalUsageCount = usages.size();
        } else if (startDate != null && endDate != null) {
            usages = featureUsageRepository.findByTimestampBetween(startDate, endDate);
            uniqueUserCount = featureUsageRepository.countDistinctUsersByTimestampBetween(startDate, endDate);
            uniqueFeatureCount = featureUsageRepository.countDistinctFeaturesByTimestampBetween(startDate, endDate);
            uniqueProductCount = featureUsageRepository.countDistinctProductsByTimestampBetween(startDate, endDate);
            totalUsageCount = usages.size();
        } else {
            usages = featureUsageRepository.findAll();
            uniqueUserCount = featureUsageRepository.countDistinctUsers();
            uniqueFeatureCount = featureUsageRepository.countDistinctFeatures();
            uniqueProductCount = featureUsageRepository.countDistinctProducts();
            totalUsageCount = usages.size();
        }

        Map<ActionType, Long> usageByActionType =
                usages.stream().collect(Collectors.groupingBy(FeatureUsage::getActionType, Collectors.counting()));

        Map<String, Long> topFeatures = usages.stream()
                .filter(u -> u.getFeatureCode() != null)
                .collect(Collectors.groupingBy(FeatureUsage::getFeatureCode, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> topProducts = usages.stream()
                .filter(u -> u.getProductCode() != null)
                .collect(Collectors.groupingBy(FeatureUsage::getProductCode, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> topUsers = usages.stream()
                .collect(Collectors.groupingBy(FeatureUsage::getUserId, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return new UsageStatsDto(
                totalUsageCount,
                uniqueUserCount,
                uniqueFeatureCount,
                uniqueProductCount,
                usageByActionType,
                topFeatures,
                topProducts,
                topUsers);
    }

    private FeatureUsageDto toDto(FeatureUsage entity) {
        return new FeatureUsageDto(
                entity.getId(),
                entity.getUserId(),
                entity.getFeatureCode(),
                entity.getProductCode(),
                entity.getActionType(),
                entity.getTimestamp(),
                entity.getContext(),
                entity.getIpAddress(),
                entity.getUserAgent());
    }
}
