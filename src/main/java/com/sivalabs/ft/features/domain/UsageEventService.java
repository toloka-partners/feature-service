package com.sivalabs.ft.features.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageEventService {
    private final UsageEventRepository usageEventRepository;

    public UsageEventService(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    @Transactional
    public UsageEvent saveUsageEvent(UsageEvent event) {
        event.setTimestamp(Instant.now());
        return usageEventRepository.save(event);
    }

    public List<UsageEvent> getFeatureUsage(String featureCode, Instant start, Instant end) {
        if (start != null && end != null) {
            return usageEventRepository.findByFeatureCodeAndTimestampBetween(featureCode, start, end);
        }
        return usageEventRepository.findByFeatureCode(featureCode);
    }

    public List<UsageEvent> getProductUsage(String productCode, Instant start, Instant end) {
        if (start != null && end != null) {
            return usageEventRepository.findByProductCodeAndTimestampBetween(productCode, start, end);
        }
        return usageEventRepository.findByProductCode(productCode);
    }
}
