package com.sivalabs.ft.features.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@DisplayName("ApplicationLifecycle Failure Scenario Tests")
@Testcontainers
class ApplicationLifecycleFailureTest {

    @Test
    @DisplayName("Should fail application startup when database is unavailable")
    void shouldFailApplicationStartupWhenDatabaseUnavailable() {
        // Test with invalid database URL
        System.setProperty("spring.datasource.url", "jdbc:postgresql://invalid-host:5432/invalid-db");
        System.setProperty("spring.datasource.username", "invalid");
        System.setProperty("spring.datasource.password", "invalid");
        System.setProperty("spring.kafka.bootstrap-servers", "localhost:9092");

        SpringApplication app = new SpringApplication(TestApplication.class);
        app.setAdditionalProfiles("test");

        Exception exception = assertThrows(Exception.class, () -> {
            app.run();
        });

        // Verify that the failure is related to database connectivity
        String message = exception.getMessage();
        assertThat(message).containsAnyOf("Database unavailable", "Connection", "database");

        // Clean up system properties
        System.clearProperty("spring.datasource.url");
        System.clearProperty("spring.datasource.username");
        System.clearProperty("spring.datasource.password");
        System.clearProperty("spring.kafka.bootstrap-servers");
    }

    @Test
    @DisplayName("Should fail application startup when Kafka is unavailable")
    void shouldFailApplicationStartupWhenKafkaUnavailable() {
        // Use H2 database for this test to isolate Kafka failure
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.kafka.bootstrap-servers", "localhost:9999"); // Invalid Kafka port

        SpringApplication app = new SpringApplication(TestApplication.class);
        app.setAdditionalProfiles("test");

        Exception exception = assertThrows(Exception.class, () -> {
            app.run();
        });

        // Verify that the failure is related to Kafka connectivity
        String message = exception.getMessage();
        assertThat(message).containsAnyOf("Kafka unavailable", "Connection", "kafka");

        // Clean up system properties
        System.clearProperty("spring.datasource.url");
        System.clearProperty("spring.datasource.username");
        System.clearProperty("spring.datasource.password");
        System.clearProperty("spring.jpa.hibernate.ddl-auto");
        System.clearProperty("spring.kafka.bootstrap-servers");
    }

    @SpringBootApplication
    @TestPropertySource(
            properties = {
                "ft.events.new-features=new_features",
                "ft.events.updated-features=updated_features",
                "ft.events.deleted-features=deleted_features",
                "ft.lifecycle.kafka-flush-timeout=5s",
                "ft.lifecycle.total-shutdown-timeout=15s",
                "spring.jpa.hibernate.ddl-auto=validate",
                "logging.level.com.sivalabs.ft.features.config=DEBUG"
            })
    static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}
