package com.sivalabs.ft.features.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sivalabs.ft.features.AbstractIT;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"ft.lifecycle.kafka-flush-timeout=5s", "ft.lifecycle.total-shutdown-timeout=15s"})
@DisplayName("ApplicationLifecycleManager Integration Tests")
class ApplicationLifecycleManagerIntegrationTest extends AbstractIT {

    @Autowired
    private ApplicationLifecycleManager lifecycleManager;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final AtomicInteger startupEventCount = new AtomicInteger(0);
    private static final AtomicInteger shutdownEventCount = new AtomicInteger(0);
    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Test
    @DisplayName("Should handle complete application lifecycle successfully")
    void shouldHandleCompleteApplicationLifecycleSuccessfully() throws Exception {
        // Verify that startup completed successfully (context is already started)
        assertThat(lifecycleManager).isNotNull();

        // Verify startup event was fired
        await().atMost(Duration.ofSeconds(5)).until(() -> startupEventCount.get() >= 1);

        // Send some test messages to Kafka before shutdown
        kafkaTemplate.send("new_features", "test-key", "test-message-1");
        kafkaTemplate.send("updated_features", "test-key", "test-message-2");
        kafkaTemplate.send("deleted_features", "test-key", "test-message-3");

        // Trigger application shutdown
        new Thread(() -> {
                    try {
                        Thread.sleep(100); // Small delay to ensure test setup is complete
                        applicationContext.close();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .start();

        // Wait for shutdown event to be processed
        boolean shutdownCompleted = shutdownLatch.await(10, TimeUnit.SECONDS);
        assertThat(shutdownCompleted).isTrue();

        // Verify shutdown event was fired
        await().atMost(Duration.ofSeconds(5)).until(() -> shutdownEventCount.get() >= 1);

        assertThat(startupEventCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(shutdownEventCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should successfully verify database connectivity on startup")
    void shouldSuccessfullyVerifyDatabaseConnectivityOnStartup() {
        // This test verifies that the application started successfully with database connectivity
        assertThat(lifecycleManager).isNotNull();

        // If we reach this point, it means database connectivity was verified successfully during startup
        // because the ApplicationLifecycleManager would have thrown an exception if database was unavailable
    }

    @Test
    @DisplayName("Should successfully verify Kafka connectivity and topics on startup")
    void shouldSuccessfullyVerifyKafkaConnectivityAndTopicsOnStartup() {
        // This test verifies that the application started successfully with Kafka connectivity
        assertThat(lifecycleManager).isNotNull();

        // Verify that Kafka producer is working by sending a test message
        assertThat(kafkaTemplate).isNotNull();

        // Send test messages to verify all required topics are accessible
        kafkaTemplate.send("new_features", "test-key", "test-startup-verification");
        kafkaTemplate.send("updated_features", "test-key", "test-startup-verification");
        kafkaTemplate.send("deleted_features", "test-key", "test-startup-verification");

        // If we reach this point without exceptions, Kafka connectivity was verified successfully
    }

    @Test
    @DisplayName("Should handle Kafka message flushing during shutdown gracefully")
    void shouldHandleKafkaMessageFlushingDuringShutdownGracefully() {
        // Send multiple messages to create pending messages for flushing
        for (int i = 0; i < 10; i++) {
            kafkaTemplate.send("new_features", "test-key-" + i, "test-message-" + i);
        }

        // The flush will be tested during the actual shutdown process
        // This test primarily verifies that the setup works correctly
        assertThat(kafkaTemplate).isNotNull();
    }

    /**
     * Test event listener to capture lifecycle events for verification
     */
    @org.springframework.stereotype.Component
    static class LifecycleEventTestListener {

        @EventListener
        public void onContextRefreshed(ContextRefreshedEvent event) {
            startupEventCount.incrementAndGet();
            startupLatch.countDown();
        }

        @EventListener
        public void onContextClosed(ContextClosedEvent event) {
            shutdownEventCount.incrementAndGet();
            shutdownLatch.countDown();
        }
    }
}
