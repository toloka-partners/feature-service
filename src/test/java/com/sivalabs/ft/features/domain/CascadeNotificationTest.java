package com.sivalabs.ft.features.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.domain.Commands.CreateFeatureCommand;
import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.entities.Notification;
import com.sivalabs.ft.features.domain.entities.NotificationRecipient;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CascadeNotificationTest extends AbstractIT {

    @Autowired private ReleaseService releaseService;
    @Autowired private FeatureService featureService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationRecipientRepository notificationRecipientRepository;

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldCreateCascadeNotificationsWhenReleaseBecomesReleased() throws InterruptedException {
        // Given - create a release
        String releaseEventId = UUID.randomUUID().toString();
        CreateReleaseCommand releaseCmd = new CreateReleaseCommand(
                "boot-microservices-book", "cascade-v1.0", "Cascade test release", "release.manager", releaseEventId);
        String releaseCode = releaseService.createRelease(releaseCmd);

        // Create multiple features assigned to different users
        CreateFeatureCommand feature1Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 1", "First feature", "dev1", "creator1");
        CreateFeatureCommand feature2Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 2", "Second feature", "dev2", "creator2");
        CreateFeatureCommand feature3Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 3", "Third feature", "dev1", "creator1"); // Same dev as feature1
        CreateFeatureCommand feature4Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 4", "Fourth feature", null, "creator3"); // No assignee

        featureService.createFeature(feature1Cmd);
        featureService.createFeature(feature2Cmd);
        featureService.createFeature(feature3Cmd);
        featureService.createFeature(feature4Cmd);

        // Wait for release creation event to be processed
        Thread.sleep(500);

        // Clear notifications from release creation
        notificationRepository.deleteAll();
        notificationRecipientRepository.deleteAll();

        // When - update release status to RELEASED
        String updateEventId = UUID.randomUUID().toString();
        UpdateReleaseCommand updateCmd = new UpdateReleaseCommand(
                releaseCode, "Ready for production", ReleaseStatus.RELEASED, Instant.now(), "release.manager", updateEventId);
        releaseService.updateRelease(updateCmd);

        // Wait for update event to be processed
        Thread.sleep(1000);

        // Then - should have cascade notifications
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        Notification notification = notifications.get(0);
        assertThat(notification.getEventType()).isEqualTo("RELEASE_UPDATED");
        assertThat(notification.getTitle()).contains("Release Published");
        assertThat(notification.getMessage()).contains("has been published!");

        // Check that all affected users are notified
        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        Set<String> recipientUsers = recipients.stream()
                .map(NotificationRecipient::getRecipient)
                .collect(Collectors.toSet());

        // Should include: release.manager (creator), dev1, dev2, creator1, creator2, creator3
        // Note: dev1 appears in multiple features but should only be notified once
        assertThat(recipientUsers).containsExactlyInAnyOrder(
                "release.manager", "dev1", "dev2", "creator1", "creator2", "creator3");
        assertThat(recipients).hasSize(6); // Unique users only
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldNotCreateCascadeNotificationsForNonReleasedStatus() throws InterruptedException {
        // Given - create a release
        String releaseEventId = UUID.randomUUID().toString();
        CreateReleaseCommand releaseCmd = new CreateReleaseCommand(
                "boot-microservices-book", "no-cascade-v1.0", "No cascade test release", "release.manager", releaseEventId);
        String releaseCode = releaseService.createRelease(releaseCmd);

        // Create a feature
        CreateFeatureCommand featureCmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Test Feature", "Test feature", "dev1", "creator1");
        featureService.createFeature(featureCmd);

        // Wait for events to be processed
        Thread.sleep(500);

        // Clear notifications from creation
        notificationRepository.deleteAll();
        notificationRecipientRepository.deleteAll();

        // When - update release to a status other than RELEASED
        String updateEventId = UUID.randomUUID().toString();
        UpdateReleaseCommand updateCmd = new UpdateReleaseCommand(
                releaseCode, "Still in progress", ReleaseStatus.IN_PROGRESS, null, "release.manager", updateEventId);
        releaseService.updateRelease(updateCmd);

        // Wait for update event to be processed
        Thread.sleep(1000);

        // Then - should have normal update notification, not cascade
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        Notification notification = notifications.get(0);
        assertThat(notification.getTitle()).contains("Release Updated"); // Not "Release Published"
        assertThat(notification.getMessage()).contains("has been updated"); // Not "has been published"

        // Should only notify the release creator, not feature users
        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getRecipient()).isEqualTo("release.manager");
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldHandleEmptyFeatureListGracefully() throws InterruptedException {
        // Given - create a release with no features
        String releaseEventId = UUID.randomUUID().toString();
        CreateReleaseCommand releaseCmd = new CreateReleaseCommand(
                "boot-microservices-book", "empty-v1.0", "Empty release", "release.manager", releaseEventId);
        String releaseCode = releaseService.createRelease(releaseCmd);

        // Wait for creation event
        Thread.sleep(500);

        // Clear notifications from creation
        notificationRepository.deleteAll();
        notificationRecipientRepository.deleteAll();

        // When - update release to RELEASED (with no features)
        String updateEventId = UUID.randomUUID().toString();
        UpdateReleaseCommand updateCmd = new UpdateReleaseCommand(
                releaseCode, "Empty release", ReleaseStatus.RELEASED, Instant.now(), "release.manager", updateEventId);
        releaseService.updateRelease(updateCmd);

        // Wait for update event
        Thread.sleep(1000);

        // Then - should still create notification but only for release manager
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).getRecipient()).isEqualTo("release.manager");
    }

    @Test
    @Sql(scripts = {"/test-data.sql"})
    void shouldDeduplicateUsersInCascadeNotifications() throws InterruptedException {
        // Given - create a release
        String releaseEventId = UUID.randomUUID().toString();
        CreateReleaseCommand releaseCmd = new CreateReleaseCommand(
                "boot-microservices-book", "dedup-v1.0", "Deduplication test", "dev1", releaseEventId);
        String releaseCode = releaseService.createRelease(releaseCmd);

        // Create features where the same user appears in multiple roles
        CreateFeatureCommand feature1Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 1", "First feature", "dev1", "dev1"); // dev1 as both creator and assignee
        CreateFeatureCommand feature2Cmd = new CreateFeatureCommand(
                "boot-microservices-book", releaseCode, "Feature 2", "Second feature", "dev2", "dev1"); // dev1 as creator, dev2 as assignee

        featureService.createFeature(feature1Cmd);
        featureService.createFeature(feature2Cmd);

        // Wait for events
        Thread.sleep(500);

        // Clear notifications from creation
        notificationRepository.deleteAll();
        notificationRecipientRepository.deleteAll();

        // When - update release to RELEASED
        String updateEventId = UUID.randomUUID().toString();
        UpdateReleaseCommand updateCmd = new UpdateReleaseCommand(
                releaseCode, "Ready", ReleaseStatus.RELEASED, Instant.now(), "dev1", updateEventId);
        releaseService.updateRelease(updateCmd);

        // Wait for update event
        Thread.sleep(1000);

        // Then - dev1 should only appear once despite being release creator, feature creator, and feature assignee
        List<NotificationRecipient> recipients = notificationRecipientRepository.findAll();
        Set<String> recipientUsers = recipients.stream()
                .map(NotificationRecipient::getRecipient)
                .collect(Collectors.toSet());

        assertThat(recipientUsers).containsExactlyInAnyOrder("dev1", "dev2");
        assertThat(recipients).hasSize(2); // Only unique users
    }
}