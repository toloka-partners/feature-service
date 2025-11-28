package com.sivalabs.ft.features.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        String host,
        int port,
        String username,
        String password,
        String virtualHost,
        Exchange exchange,
        Retry retry,
        DeadLetter deadLetter) {

    public record Exchange(String name, String type, boolean durable, RoutingKeys routingKeys) {

        public record RoutingKeys(String featureCreated, String featureUpdated, String featureDeleted) {}
    }

    public record Retry(int maxAttempts, long initialInterval, double multiplier, long maxInterval) {}

    public record DeadLetter(String exchangeName, String queueName, String routingKey) {}
}
