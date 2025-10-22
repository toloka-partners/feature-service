package com.sivalabs.ft.features.domain.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sivalabs.ft.features.domain.FeatureRepository;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private FeatureRepository featureRepository;

    private ReleaseEventListener releaseEventListener;

    @BeforeEach
    void setUp() {
        releaseEventListener = new ReleaseEventListener(notificationService, featureRepository);
    }

    @Test
    void shouldCreateNotificationForReleaseCreatedEvent() {
        // Given
        String eventId = "event-123";
        ReleaseCreatedEvent event = new ReleaseCreatedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Initial release",
                ReleaseStatus.DRAFT,
                "PROD",
                "john.doe",
                Instant.now()
        );

        when(notificationService.isEventProcessed(eventId)).thenReturn(false);

        // When
        releaseEventListener.handleReleaseCreatedEvent(event);

        // Then
        verify(notificationService).createNotification(
                eq(eventId),
                eq("RELEASE_CREATED"),
                eq("New Release Created: PROD-v1.0"),
                eq("A new release 'PROD-v1.0' has been created in product PROD"),
                eq("/releases/PROD-v1.0"),
                eq(List.of("john.doe"))
        );
    }

    @Test
    void shouldSkipProcessingIfReleaseCreatedEventAlreadyProcessed() {
        // Given
        String eventId = "event-123";
        ReleaseCreatedEvent event = new ReleaseCreatedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Initial release",
                ReleaseStatus.DRAFT,
                "PROD",
                "john.doe",
                Instant.now()
        );

        when(notificationService.isEventProcessed(eventId)).thenReturn(true);

        // When
        releaseEventListener.handleReleaseCreatedEvent(event);

        // Then
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateNotificationForReleaseUpdatedEvent() {
        // Given
        String eventId = "event-456";
        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Updated description",
                ReleaseStatus.DRAFT,
                null,
                "PROD",
                "john.doe",
                Instant.now().minusSeconds(3600),
                "jane.doe",
                Instant.now()
        );

        when(notificationService.isEventProcessed(eventId)).thenReturn(false);

        // When
        releaseEventListener.handleReleaseUpdatedEvent(event);

        // Then
        verify(notificationService).createNotification(
                eq(eventId),
                eq("RELEASE_UPDATED"),
                eq("Release Updated: PROD-v1.0"),
                eq("Release 'PROD-v1.0' has been updated"),
                eq("/releases/PROD-v1.0"),
                eq(List.of("john.doe"))
        );
    }

    @Test
    void shouldCreateCascadeNotificationsWhenReleaseStatusChangesToReleased() {
        // Given
        String eventId = "event-789";
        ReleaseUpdatedEvent event = new ReleaseUpdatedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Release is ready",
                ReleaseStatus.RELEASED,
                Instant.now(),
                "PROD",
                "john.doe",
                Instant.now().minusSeconds(3600),
                "jane.doe",
                Instant.now()
        );

        // Create mock features with different users
        Feature feature1 = createMockFeature("john.doe", "alice.dev");
        Feature feature2 = createMockFeature("bob.dev", "charlie.dev");
        Feature feature3 = createMockFeature("john.doe", null); // No assignee

        when(notificationService.isEventProcessed(eventId)).thenReturn(false);
        when(featureRepository.findByReleaseCode("PROD-v1.0"))
                .thenReturn(List.of(feature1, feature2, feature3));

        // When
        releaseEventListener.handleReleaseUpdatedEvent(event);

        // Then
        verify(notificationService).createNotification(
                eq(eventId),
                eq("RELEASE_UPDATED"),
                eq("Release Published: PROD-v1.0"),
                eq("Release 'PROD-v1.0' has been published!"),
                eq("/releases/PROD-v1.0"),
                eq(List.of("john.doe", "alice.dev", "bob.dev", "charlie.dev"))
        );
    }

    @Test
    void shouldCreateNotificationForReleaseDeletedEvent() {
        // Given
        String eventId = "event-delete";
        ReleaseDeletedEvent event = new ReleaseDeletedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Deleted release",
                ReleaseStatus.DRAFT,
                null,
                "PROD",
                "john.doe",
                Instant.now().minusSeconds(7200),
                "jane.doe",
                Instant.now().minusSeconds(3600),
                "admin.user",
                Instant.now()
        );

        when(notificationService.isEventProcessed(eventId)).thenReturn(false);

        // When
        releaseEventListener.handleReleaseDeletedEvent(event);

        // Then
        verify(notificationService).createNotification(
                eq(eventId),
                eq("RELEASE_DELETED"),
                eq("Release Deleted: PROD-v1.0"),
                eq("Release 'PROD-v1.0' has been deleted"),
                eq("/releases"),
                eq(List.of("john.doe"))
        );
    }

    @Test
    void shouldSkipProcessingIfReleaseDeletedEventAlreadyProcessed() {
        // Given
        String eventId = "event-delete";
        ReleaseDeletedEvent event = new ReleaseDeletedEvent(
                eventId,
                1L,
                "PROD-v1.0",
                "Deleted release",
                ReleaseStatus.DRAFT,
                null,
                "PROD",
                "john.doe",
                Instant.now().minusSeconds(7200),
                "jane.doe",
                Instant.now().minusSeconds(3600),
                "admin.user",
                Instant.now()
        );

        when(notificationService.isEventProcessed(eventId)).thenReturn(true);

        // When
        releaseEventListener.handleReleaseDeletedEvent(event);

        // Then
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    private Feature createMockFeature(String createdBy, String assignedTo) {
        Feature feature = new Feature();
        feature.setCreatedBy(createdBy);
        feature.setAssignedTo(assignedTo);
        
        // Create mock release and product to avoid null pointer issues
        Release release = new Release();
        release.setCode("PROD-v1.0");
        
        Product product = new Product();
        product.setCode("PROD");
        release.setProduct(product);
        
        feature.setRelease(release);
        feature.setStatus(FeatureStatus.NEW);
        
        return feature;
    }
}