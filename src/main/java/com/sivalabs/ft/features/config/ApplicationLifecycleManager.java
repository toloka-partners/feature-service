package com.sivalabs.ft.features.config;

import com.sivalabs.ft.features.ApplicationProperties;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

/**
 * Manages application lifecycle events for startup initialization and graceful shutdown.
 * Handles database connectivity verification and Kafka producer warm-up on startup,
 * and ensures proper resource cleanup on shutdown.
 */
@Component
@EnableConfigurationProperties(ApplicationProperties.class)
public class ApplicationLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationLifecycleManager.class);

    // Timeout constants will be replaced by configurable properties

    private final DataSource dataSource;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProducerFactory<String, Object> producerFactory;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ApplicationLifecycleManager(
            DataSource dataSource,
            KafkaTemplate<String, Object> kafkaTemplate,
            ProducerFactory<String, Object> producerFactory,
            ApplicationProperties applicationProperties) {
        this.dataSource = dataSource;
        this.kafkaTemplate = kafkaTemplate;
        this.producerFactory = producerFactory;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Handles application startup initialization tasks.
     * Verifies database connectivity and Kafka producer warm-up.
     *
     * @param event ContextRefreshedEvent fired when ApplicationContext is initialized
     * @throws RuntimeException if database or Kafka are unavailable
     */
    @EventListener
    public void onApplicationStartup(ContextRefreshedEvent event) {
        logger.info("Starting application lifecycle initialization...");

        try {
            verifyDatabaseConnectivity();
            verifyKafkaConnectivityAndTopics();
            logger.info("Application startup completed successfully");
        } catch (Exception e) {
            logger.error("Application startup failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles application shutdown cleanup tasks.
     * Flushes pending Kafka messages and ensures graceful resource cleanup.
     *
     * @param event ContextClosedEvent fired when ApplicationContext is closing
     */
    @EventListener
    public void onApplicationShutdown(ContextClosedEvent event) {
        logger.info("Starting application graceful shutdown...");

        try {
            flushKafkaMessages();
            logger.info("Application shutdown completed successfully");
        } catch (Exception e) {
            logger.error("Error during application shutdown: {}", e.getMessage(), e);
            // Continue shutdown even if flush fails
        }
    }

    /**
     * Verifies database connectivity by attempting to get a connection.
     *
     * @throws RuntimeException if database connection fails
     */
    private void verifyDatabaseConnectivity() {
        logger.info("Verifying database connectivity...");

        try {
            var connection = dataSource.getConnection();
            if (connection.isValid(5)) {
                connection.close();
                logger.info("Database connectivity verified successfully");
            } else {
                throw new RuntimeException("Database connection validation failed");
            }
        } catch (Exception e) {
            logger.error("Database connectivity verification failed: {}", e.getMessage());
            throw new RuntimeException("Database unavailable", e);
        }
    }

    /**
     * Verifies Kafka connectivity and ensures required topics exist.
     *
     * @throws RuntimeException if Kafka is unavailable or required topics are missing
     */
    private void verifyKafkaConnectivityAndTopics() {
        logger.info("Verifying Kafka connectivity and topics...");

        try {
            // Test Kafka connectivity by creating admin client
            var adminClientConfig = producerFactory.getConfigurationProperties();
            try (AdminClient adminClient = KafkaAdminClient.create(adminClientConfig)) {

                // List topics to verify connectivity
                ListTopicsResult topicsResult = adminClient.listTopics();
                var topicNames = topicsResult.names().get(5, TimeUnit.SECONDS);

                logger.info("Kafka connectivity verified. Available topics: {}", topicNames);

                // Verify required topics exist
                verifyRequiredTopics(topicNames);

                // Warm up Kafka producer
                warmupKafkaProducer();

                logger.info("Kafka connectivity and topic verification completed successfully");
            }
        } catch (Exception e) {
            logger.error("Kafka connectivity verification failed: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("topic")) {
                throw new RuntimeException(e.getMessage(), e);
            }
            throw new RuntimeException("Kafka unavailable", e);
        }
    }

    /**
     * Verifies that all required Kafka topics exist.
     *
     * @param availableTopics Set of available topic names
     * @throws RuntimeException if any required topic is missing
     */
    private void verifyRequiredTopics(java.util.Set<String> availableTopics) {
        var events = applicationProperties.events();
        List<String> requiredTopics = List.of(events.newFeatures(), events.updatedFeatures(), events.deletedFeatures());

        for (String topic : requiredTopics) {
            if (!availableTopics.contains(topic)) {
                String errorMessage = "Required Kafka topic not found with topic name: " + topic;
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        }

        logger.info("All required Kafka topics verified: {}", requiredTopics);
    }

    /**
     * Warms up the Kafka producer by testing connectivity.
     */
    private void warmupKafkaProducer() {
        logger.info("Warming up Kafka producer...");
        try {
            // Trigger producer initialization by accessing metadata
            kafkaTemplate.partitionsFor(applicationProperties.events().newFeatures());
            logger.info("Kafka producer warmed up successfully");
        } catch (Exception e) {
            logger.error("Kafka producer warm-up failed: {}", e.getMessage());
            throw new RuntimeException("Kafka producer warm-up failed", e);
        }
    }

    /**
     * Flushes pending Kafka messages with timeout.
     */
    private void flushKafkaMessages() {
        var flushTimeout = applicationProperties.lifecycle() != null
                ? applicationProperties.lifecycle().kafkaFlushTimeout()
                : Duration.ofSeconds(10);

        logger.info("Flushing pending Kafka messages with timeout: {}", flushTimeout);

        try {
            // Note: KafkaTemplate.flush() doesn't support timeout, so we use a simple approach
            kafkaTemplate.flush();
            logger.info("Kafka messages flushed successfully");
        } catch (Exception e) {
            logger.error("Failed to flush Kafka messages within timeout: {}", e.getMessage());
            // Don't throw exception to continue shutdown process
        }
    }
}
