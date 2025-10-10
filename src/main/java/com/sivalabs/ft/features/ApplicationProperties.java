package com.sivalabs.ft.features;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ft")
public record ApplicationProperties(EventsProperties events, AsyncProperties async) {

    public record EventsProperties(String newFeatures, String updatedFeatures, String deletedFeatures) {}

    public record AsyncProperties(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {}
}
