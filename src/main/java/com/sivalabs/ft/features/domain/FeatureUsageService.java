package com.sivalabs.ft.features.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.entities.FeatureUsage;
import com.sivalabs.ft.features.domain.models.ActionType;
import java.time.Instant;
import java.util.Map;
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
            Map<String, Object> contextData) {
        if (!applicationProperties.usageTracking().enabled()) {
            return;
        }
        try {
            String contextJson = null;
            if (contextData != null && !contextData.isEmpty()) {
                try {
                    contextJson = objectMapper.writeValueAsString(contextData);
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
        logUsage(userId, featureCode, productCode, null, actionType, null);
    }

    @Transactional
    public void logUsage(
            String userId, String featureCode, String productCode, ActionType actionType, Map<String, Object> context) {
        logUsage(userId, featureCode, productCode, null, actionType, context);
    }

    @Transactional
    public void logUsage(
            String userId, String featureCode, String productCode, String releaseCode, ActionType actionType) {
        logUsage(userId, featureCode, productCode, releaseCode, actionType, null);
    }
}
