package com.sivalabs.ft.features.domain.events;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.domain.FeatureService;
import com.sivalabs.ft.features.domain.NotificationRecipientService;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.ReleaseService;
import com.sivalabs.ft.features.domain.dtos.FeatureDto;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import com.sivalabs.ft.features.domain.models.NotificationEventType;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseEventListenerTest {

    @Mock
    private EventDeduplicationService eventDeduplicationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRecipientService recipientService;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private FeatureService featureService;

    private ObjectMapper objectMapper;
    private ReleaseEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new ReleaseEventListener(
                eventDeduplicationService,
                notificationService,
                recipientService,
                releaseService,
                featureService,
                objectMapper);
    }

    @Test
    void shouldCreateNotificationForReleaseCreatedEvent() {
        // Given
        String eventId = "event-123";
        String releaseCode = "PROD-R1";
        String createdBy = "user1";

        ReleaseCreatedEvent event = new ReleaseCreatedEvent(
                eventId, 1L, releaseCode, "Release 1.0", ReleaseStatus.DRAFT, "PROD", createdBy, Instant.now());

        ReleaseDto releaseDto = new ReleaseDto(
                1L, releaseCode, "Release 1.0", ReleaseStatus.DRAFT, null, createdBy, Instant.now(), null, null);

        // Mock the deduplication service to execute the lambda
        doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                })
                .when(eventDeduplicationService)
                .executeIdempotent(eq(eventId), eq(EventType.EVENT), any());

        when(releaseService.findReleaseByCode(releaseCode)).thenReturn(Optional.of(releaseDto));

        // When
        listener.handleReleaseCreatedEvent(event, "topic", 0, 0L);

        // Then
        verify(notificationService)
                .createNotification(
                        eq(createdBy),
                        startsWith(eventId + "-recipient-"),
                        eq(NotificationEventType.RELEASE_CREATED),
                        anyString(),
                        eq("/releases/" + releaseCode));
    }

    @Test
    void shouldCreateNotificationForReleaseUpdatedEvent() {
        // Given
        String eventId = "event-456";
        String releaseCode = "PROD-R1";
        String createdBy = "user1";

        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                1L,
                releaseCode,
                "Release 1.0 Updated",
                ReleaseStatus.DRAFT,
                ReleaseStatus.DRAFT,
                null,
                "PROD",
                createdBy,
                Instant.now(),
                "user2",
                Instant.now());

        ReleaseDto releaseDto = new ReleaseDto(
                1L,
                releaseCode,
                "Release 1.0 Updated",
                ReleaseStatus.DRAFT,
                null,
                createdBy,
                Instant.now(),
                "user2",
                Instant.now());

        // Mock the deduplication service to execute the lambda
        doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                })
                .when(eventDeduplicationService)
                .executeIdempotent(eq(eventId), eq(EventType.EVENT), any());

        when(releaseService.findReleaseByCode(releaseCode)).thenReturn(Optional.of(releaseDto));

        // When
        listener.handleReleaseUpdatedEvent(event, "topic", 0, 0L);

        // Then
        verify(notificationService)
                .createNotification(
                        eq(createdBy),
                        startsWith(eventId + "-recipient-"),
                        eq(NotificationEventType.RELEASE_UPDATED),
                        anyString(),
                        eq("/releases/" + releaseCode));
    }

    @Test
    void shouldCreateCascadeNotificationsWhenReleaseStatusChangesToReleased() {
        // Given
        String eventId = "event-789";
        String releaseCode = "PROD-R1";
        String createdBy = "user1";
        String assignedTo = "user2";

        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                1L,
                releaseCode,
                "Release 1.0",
                ReleaseStatus.RELEASED,
                ReleaseStatus.DRAFT, // Previous status was DRAFT
                Instant.now(),
                "PROD",
                createdBy,
                Instant.now(),
                "user3",
                Instant.now());

        ReleaseDto releaseDto = new ReleaseDto(
                1L,
                releaseCode,
                "Release 1.0",
                ReleaseStatus.RELEASED,
                Instant.now(),
                createdBy,
                Instant.now(),
                "user3",
                Instant.now());

        FeatureDto feature1 = new FeatureDto(
                1L,
                "PROD-1",
                "Feature 1",
                "Description",
                FeatureStatus.RELEASED,
                releaseCode,
                false,
                assignedTo,
                createdBy,
                Instant.now(),
                null,
                null);

        FeatureDto feature2 = new FeatureDto(
                2L,
                "PROD-2",
                "Feature 2",
                "Description",
                FeatureStatus.RELEASED,
                releaseCode,
                false,
                "user3",
                createdBy,
                Instant.now(),
                null,
                null);

        // Mock the deduplication service to execute the lambda
        doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                })
                .when(eventDeduplicationService)
                .executeIdempotent(eq(eventId), eq(EventType.EVENT), any());

        when(releaseService.findReleaseByCode(releaseCode)).thenReturn(Optional.of(releaseDto));
        when(featureService.findFeaturesByRelease(null, releaseCode)).thenReturn(List.of(feature1, feature2));

        // When
        listener.handleReleaseUpdatedEvent(event, "topic", 0, 0L);

        // Then
        // Verify notification for release creator
        verify(notificationService)
                .createNotification(
                        eq(createdBy),
                        startsWith(eventId + "-recipient-"),
                        eq(NotificationEventType.RELEASE_UPDATED),
                        anyString(),
                        eq("/releases/" + releaseCode));

        // Verify cascade notifications for affected users (createdBy, assignedTo, user3)
        verify(notificationService, atLeast(3))
                .createNotification(
                        anyString(),
                        startsWith(eventId + "-cascade-"),
                        eq(NotificationEventType.RELEASE_UPDATED),
                        anyString(),
                        eq("/releases/" + releaseCode));
    }

    @Test
    void shouldCreateNotificationForReleaseDeletedEvent() {
        // Given
        String eventId = "event-999";
        String releaseCode = "PROD-R1";
        String deletedBy = "user1";

        ReleaseDeletedEvent event = new ReleaseDeletedEvent(
                eventId,
                1L,
                releaseCode,
                "Release 1.0",
                ReleaseStatus.DRAFT,
                null,
                "PROD",
                "user1",
                Instant.now(),
                null,
                null,
                deletedBy,
                Instant.now());

        // Mock the deduplication service to execute the lambda
        doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                })
                .when(eventDeduplicationService)
                .executeIdempotent(eq(eventId), eq(EventType.EVENT), any());

        // When
        listener.handleReleaseDeletedEvent(event, "topic", 0, 0L);

        // Then
        verify(notificationService)
                .createNotification(
                        eq(deletedBy),
                        eq(eventId),
                        eq(NotificationEventType.RELEASE_DELETED),
                        anyString(),
                        eq("/releases/" + releaseCode));
    }

    @Test
    void shouldNotCreateCascadeNotificationsWhenNoFeaturesInRelease() {
        // Given
        String eventId = "event-empty";
        String releaseCode = "PROD-R1";
        String createdBy = "user1";

        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                1L,
                releaseCode,
                "Release 1.0",
                ReleaseStatus.RELEASED,
                ReleaseStatus.DRAFT,
                Instant.now(),
                "PROD",
                createdBy,
                Instant.now(),
                "user2",
                Instant.now());

        ReleaseDto releaseDto = new ReleaseDto(
                1L,
                releaseCode,
                "Release 1.0",
                ReleaseStatus.RELEASED,
                Instant.now(),
                createdBy,
                Instant.now(),
                "user2",
                Instant.now());

        // Mock the deduplication service to execute the lambda
        doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                })
                .when(eventDeduplicationService)
                .executeIdempotent(eq(eventId), eq(EventType.EVENT), any());

        when(releaseService.findReleaseByCode(releaseCode)).thenReturn(Optional.of(releaseDto));
        when(featureService.findFeaturesByRelease(null, releaseCode)).thenReturn(List.of());

        // When
        listener.handleReleaseUpdatedEvent(event, "topic", 0, 0L);

        // Then
        // Only one notification for the release creator, no cascade notifications
        verify(notificationService, times(1))
                .createNotification(
                        anyString(), anyString(), eq(NotificationEventType.RELEASE_UPDATED), anyString(), anyString());
    }
}
