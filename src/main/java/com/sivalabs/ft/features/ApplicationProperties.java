package com.sivalabs.ft.features;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ft")
public record ApplicationProperties(EventsProperties events, LifecycleProperties lifecycle) {

    public record EventsProperties(String newFeatures, String updatedFeatures, String deletedFeatures) {}

    public record LifecycleProperties(Duration kafkaFlushTimeout, Duration totalShutdownTimeout) {

        public LifecycleProperties() {
            this(Duration.ofSeconds(10), Duration.ofSeconds(30));
        }
    }
}
