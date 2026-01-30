package com.sivalabs.ft.features.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sivalabs.ft.features.ApplicationProperties;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationLifecycleManager Tests")
class ApplicationLifecycleManagerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ProducerFactory<String, Object> producerFactory;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.EventsProperties eventsProperties;

    @Mock
    private ApplicationProperties.LifecycleProperties lifecycleProperties;

    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;

    @Mock
    private ContextClosedEvent contextClosedEvent;

    private ApplicationLifecycleManager lifecycleManager;

    @BeforeEach
    void setUp() {
        when(applicationProperties.events()).thenReturn(eventsProperties);
        when(applicationProperties.lifecycle()).thenReturn(lifecycleProperties);
        when(eventsProperties.newFeatures()).thenReturn("new_features");
        when(eventsProperties.updatedFeatures()).thenReturn("updated_features");
        when(eventsProperties.deletedFeatures()).thenReturn("deleted_features");
        when(lifecycleProperties.kafkaFlushTimeout()).thenReturn(Duration.ofSeconds(10));
        when(lifecycleProperties.totalShutdownTimeout()).thenReturn(Duration.ofSeconds(30));

        lifecycleManager =
                new ApplicationLifecycleManager(dataSource, kafkaTemplate, producerFactory, applicationProperties);
    }

    @Test
    @DisplayName("Should successfully complete startup when database and Kafka are available")
    void shouldCompleteStartupSuccessfully() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockTopicsFuture = mock(KafkaFuture.class);

        Set<String> availableTopics = Set.of("new_features", "updated_features", "deleted_features");
        when(mockTopicsFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(availableTopics);
        when(mockTopicsResult.names()).thenReturn(mockTopicsFuture);
        when(mockAdminClient.listTopics()).thenReturn(mockTopicsResult);

        Map<String, Object> adminConfig = new HashMap<>();
        when(producerFactory.getConfigurationProperties()).thenReturn(adminConfig);

        try (MockedStatic<org.apache.kafka.clients.admin.KafkaAdminClient> mockedStatic =
                Mockito.mockStatic(org.apache.kafka.clients.admin.KafkaAdminClient.class)) {
            mockedStatic
                    .when(() -> org.apache.kafka.clients.admin.KafkaAdminClient.create(adminConfig))
                    .thenReturn(mockAdminClient);

            // Act & Assert
            assertDoesNotThrow(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent));
        }

        // Verify
        verify(dataSource).getConnection();
        verify(mockConnection).isValid(5);
        verify(mockConnection).close();
        verify(kafkaTemplate).partitionsFor("new_features");
    }

    @Test
    @DisplayName("Should fail startup when database is unavailable")
    void shouldFailStartupWhenDatabaseUnavailable() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database unavailable");
    }

    @Test
    @DisplayName("Should fail startup when database connection is invalid")
    void shouldFailStartupWhenDatabaseConnectionInvalid() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection validation failed");
    }

    @Test
    @DisplayName("Should fail startup when Kafka is unavailable")
    void shouldFailStartupWhenKafkaUnavailable() throws Exception {
        // Arrange - Setup successful database connection
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // Arrange - Setup Kafka failure
        Map<String, Object> adminConfig = new HashMap<>();
        when(producerFactory.getConfigurationProperties()).thenReturn(adminConfig);

        try (MockedStatic<org.apache.kafka.clients.admin.KafkaAdminClient> mockedStatic =
                Mockito.mockStatic(org.apache.kafka.clients.admin.KafkaAdminClient.class)) {
            mockedStatic
                    .when(() -> org.apache.kafka.clients.admin.KafkaAdminClient.create(adminConfig))
                    .thenThrow(new RuntimeException("Kafka connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kafka unavailable");
        }
    }

    @Test
    @DisplayName("Should fail startup when required Kafka topic is missing")
    void shouldFailStartupWhenRequiredTopicMissing() throws Exception {
        // Arrange - Setup successful database connection
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // Arrange - Setup Kafka with missing topic
        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockTopicsFuture = mock(KafkaFuture.class);

        // Missing 'deleted_features' topic
        Set<String> availableTopics = Set.of("new_features", "updated_features");
        when(mockTopicsFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(availableTopics);
        when(mockTopicsResult.names()).thenReturn(mockTopicsFuture);
        when(mockAdminClient.listTopics()).thenReturn(mockTopicsResult);

        Map<String, Object> adminConfig = new HashMap<>();
        when(producerFactory.getConfigurationProperties()).thenReturn(adminConfig);

        try (MockedStatic<org.apache.kafka.clients.admin.KafkaAdminClient> mockedStatic =
                Mockito.mockStatic(org.apache.kafka.clients.admin.KafkaAdminClient.class)) {
            mockedStatic
                    .when(() -> org.apache.kafka.clients.admin.KafkaAdminClient.create(adminConfig))
                    .thenReturn(mockAdminClient);

            // Act & Assert
            assertThatThrownBy(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Required Kafka topic not found with topic name: deleted_features");
        }
    }

    @Test
    @DisplayName("Should fail startup when Kafka producer warm-up fails")
    void shouldFailStartupWhenKafkaProducerWarmupFails() throws Exception {
        // Arrange - Setup successful database connection
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // Arrange - Setup successful Kafka admin but failed producer warm-up
        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockTopicsFuture = mock(KafkaFuture.class);

        Set<String> availableTopics = Set.of("new_features", "updated_features", "deleted_features");
        when(mockTopicsFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(availableTopics);
        when(mockTopicsResult.names()).thenReturn(mockTopicsFuture);
        when(mockAdminClient.listTopics()).thenReturn(mockTopicsResult);

        Map<String, Object> adminConfig = new HashMap<>();
        when(producerFactory.getConfigurationProperties()).thenReturn(adminConfig);

        // Make producer warm-up fail
        when(kafkaTemplate.partitionsFor(anyString()))
                .thenThrow(new RuntimeException("Producer initialization failed"));

        try (MockedStatic<org.apache.kafka.clients.admin.KafkaAdminClient> mockedStatic =
                Mockito.mockStatic(org.apache.kafka.clients.admin.KafkaAdminClient.class)) {
            mockedStatic
                    .when(() -> org.apache.kafka.clients.admin.KafkaAdminClient.create(adminConfig))
                    .thenReturn(mockAdminClient);

            // Act & Assert
            assertThatThrownBy(() -> lifecycleManager.onApplicationStartup(contextRefreshedEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kafka producer warm-up failed");
        }
    }

    @Test
    @DisplayName("Should successfully complete shutdown and flush Kafka messages")
    void shouldCompleteShutdownSuccessfully() {
        // Act & Assert
        assertDoesNotThrow(() -> lifecycleManager.onApplicationShutdown(contextClosedEvent));

        // Verify
        verify(kafkaTemplate).flush();
    }

    @Test
    @DisplayName("Should continue shutdown even when Kafka flush fails")
    void shouldContinueShutdownWhenKafkaFlushFails() {
        // Arrange
        doThrow(new RuntimeException("Flush failed")).when(kafkaTemplate).flush();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> lifecycleManager.onApplicationShutdown(contextClosedEvent));

        // Verify flush was attempted
        verify(kafkaTemplate).flush();
    }

    @Test
    @DisplayName("Should handle shutdown with null lifecycle properties")
    void shouldHandleShutdownWithNullLifecycleProperties() {
        // Arrange
        when(applicationProperties.lifecycle()).thenReturn(null);
        lifecycleManager =
                new ApplicationLifecycleManager(dataSource, kafkaTemplate, producerFactory, applicationProperties);

        // Act & Assert
        assertDoesNotThrow(() -> lifecycleManager.onApplicationShutdown(contextClosedEvent));

        // Verify flush was attempted with default timeout
        verify(kafkaTemplate).flush();
    }
}
