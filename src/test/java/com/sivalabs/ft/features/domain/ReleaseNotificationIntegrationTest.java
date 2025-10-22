package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.entities.Notification;
import com.sivalabs.ft.features.domain.entities.NotificationRecipient;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ReleaseNotificationIntegrationTest extends AbstractIT {

    @Autowired private ReleaseService releaseService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationRecipientRepository notificationRecipientRepository;

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldCreateNotificationWhenReleaseIsCreated() throws InterruptedException {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateReleaseCommand cmd = new CreateReleaseCommand(
                "boot-microservices-book", "v2.0", "Second version", "john.doe", eventId);

        // When
        String releaseCode = releaseService.createRelease(cmd);

        // Wait a bit for async event processing
        Thread.sleep(1000);

        // Then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification notification = notifications.get(0);
        assertThat(notification.getEventId()).isEqualTo(eventId);
        assertThat(notification.getEventType()).isEqualTo("RELEASE_CREATED");
        assertThat(notification.getTitle()).contains(releaseCode);
        assertThat(notification.getLink()).isEqualTo("/releases/" + releaseCode);

        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getRecipient()).isEqualTo("john.doe");
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldNotCreateDuplicateNotificationForSameEventId() throws InterruptedException {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateReleaseCommand cmd = new CreateReleaseCommand(
                "boot-microservices-book", "v2.1", "Second version patch", "john.doe", eventId);

        // When - create release twice with same eventId
        releaseService.createRelease(cmd);
        releaseService.createRelease(cmd);

        // Wait a bit for async event processing
        Thread.sleep(1000);

        // Then - should only have one notification
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        assertThat(recipients).hasSize(1);
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldCreateCascadeNotificationsWhenReleaseIsPublished() throws InterruptedException {
        // Given - create a release first
        String createEventId = UUID.randomUUID().toString();
        CreateReleaseCommand createCmd = new CreateReleaseCommand(
                "boot-microservices-book", "v3.0", "Major version", "john.doe", createEventId);
        String releaseCode = releaseService.createRelease(createCmd);

        // Wait for creation event to be processed
        Thread.sleep(500);

        // When - update release to RELEASED status
        String updateEventId = UUID.randomUUID().toString();
        UpdateReleaseCommand updateCmd = new UpdateReleaseCommand(
                releaseCode, "Ready for production", ReleaseStatus.RELEASED, Instant.now(), "jane.doe", updateEventId);
        releaseService.updateRelease(updateCmd);

        // Wait for update event to be processed
        Thread.sleep(1000);

        // Then - should have notifications for both create and update events
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(2);
        
        // Check that we have both types of notifications
        boolean hasCreateNotification = notifications.stream()
                .anyMatch(n -> "RELEASE_CREATED".equals(n.getEventType()));
        boolean hasUpdateNotification = notifications.stream()
                .anyMatch(n -> "RELEASE_UPDATED".equals(n.getEventType()));
        
        assertThat(hasCreateNotification).isTrue();
        assertThat(hasUpdateNotification).isTrue();

        // Check update notification details
        Notification updateNotification = notifications.stream()
                .filter(n -> "RELEASE_UPDATED".equals(n.getEventType()))
                .findFirst()
                .orElseThrow();
        
        assertThat(updateNotification.getTitle()).contains("Release Published");
        assertThat(updateNotification.getMessage()).contains("has been published");
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldHandleEventDeduplicationCorrectly() throws InterruptedException {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateReleaseCommand cmd = new CreateReleaseCommand(
                "boot-microservices-book", "v4.0", "Another version", "alice.dev", eventId);

        // When - simulate multiple attempts to process same event
        releaseService.createRelease(cmd);
        
        // Wait for first event to be processed
        Thread.sleep(500);
        
        // Try to create again with same eventId
        releaseService.createRelease(cmd);
        
        // Wait for potential second event processing
        Thread.sleep(500);

        // Then - should only have one notification despite multiple attempts
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        
        Notification notification = notifications.get(0);
        assertThat(notification.getEventId()).isEqualTo(eventId);
        
        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getRecipient()).isEqualTo("alice.dev");
    }

    @Test
    void shouldHandleNullEventIdGracefully() {
        // Given - command without eventId (backward compatibility)
        CreateReleaseCommand cmd = new CreateReleaseCommand(
                "boot-microservices-book", "v5.0", "Version without events", "bob.dev", null);

        // When
        String releaseCode = releaseService.createRelease(cmd);

        // Then - should succeed but not create notifications
        assertThat(releaseCode).isNotNull();
        
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).isEmpty();
    }
}