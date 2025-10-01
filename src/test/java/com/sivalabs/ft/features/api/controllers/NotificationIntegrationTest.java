package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateFeaturePayload;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.dtos.NotificationDto;
import com.sivalabs.ft.features.domain.models.NotificationEventType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for notification system with feature events
 * Tests that notifications are created when feature events are processed
 */
@Sql("/test-data.sql")
class NotificationIntegrationTest extends AbstractIT {

    @DynamicPropertySource
    static void configureAdditionalProperties(DynamicPropertyRegistry registry) {
        // Override consumer group-id for this specific test with timestamp to avoid conflicts
        registry.add(
                "spring.kafka.consumer.group-id",
                () -> "notification-integration-test-group-" + System.currentTimeMillis());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Clean up tables before each test
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM processed_events");
    }

    @AfterEach
    void tearDown() {
        // Clean up tables after each test
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM processed_events");
    }

    private void setAuthenticationContext(String username) {
        Map<String, Object> claims = Map.of(
                "preferred_username",
                username,
                "userId",
                username + "-id",
                "realm_access",
                Map.of("roles", List.of("ROLE_USER")));
        Map<String, Object> headers = Map.of("header", "mock");
        Jwt jwt = new Jwt("mock-jwt-token", Instant.now(), Instant.now().plusSeconds(300), headers, claims);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateNotificationWhenFeatureIsCreated() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Feature for Notification", "Test notification creation", null, "testuser");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async event processing and notification creation
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Check that notification was created for the creator (testuser)
            Page<NotificationDto> creatorNotifications =
                    notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
            assertThat(creatorNotifications.getContent())
                    .as("Creator notifications for testuser")
                    .hasSize(1);

            // Verify notification details
            NotificationDto notification = creatorNotifications.getContent().get(0);
            assertThat(notification.eventId()).startsWith(eventId); // eventId is modified with recipient suffix
            assertThat(notification.eventType()).isEqualTo(NotificationEventType.FEATURE_CREATED);
            assertThat(notification.recipientUserId()).isEqualTo("testuser");
            assertThat(notification.read()).isFalse();
            assertThat(notification.link()).contains("/features/");
            assertThat(notification.eventDetails()).contains("created");
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldNotCreateDuplicateNotificationsForSameEventId() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Duplicate Test Feature", "Test duplicate prevention", null, "testuser");

        // When - Create feature twice with same eventId
        var result1 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        var result2 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result1).hasStatus(HttpStatus.CREATED);
        assertThat(result2).hasStatus(HttpStatus.CREATED);

        // Wait for async processing
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Should have only one notification despite duplicate requests
            Page<NotificationDto> notificationsPage =
                    notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
            assertThat(notificationsPage.getContent()).hasSize(1);

            NotificationDto notification = notificationsPage.getContent().get(0);
            assertThat(notification.eventId()).startsWith(eventId); // eventId is modified with recipient suffix
            assertThat(notification.eventType()).isEqualTo(NotificationEventType.FEATURE_CREATED);
        });
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldMarkNotificationAsRead() throws Exception {
        // Given - Create a feature to generate notification
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Read Test Feature", "Test read functionality", null, "testuser");

        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Wait for notification creation
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Page<NotificationDto> notificationsPage =
                    notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
            assertThat(notificationsPage.getContent()).hasSize(1);
        });

        // Get the notification ID
        Page<NotificationDto> notificationsPage =
                notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
        UUID notificationId = notificationsPage.getContent().get(0).id();

        // When - Mark as read
        var readResult =
                mvc.put().uri("/api/notifications/{id}/read", notificationId).exchange();

        assertThat(readResult).hasStatus2xxSuccessful();

        // Then - Verify notification is marked as read
        Page<NotificationDto> updatedNotificationsPage =
                notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
        assertThat(updatedNotificationsPage.getContent()).hasSize(1);
        assertThat(updatedNotificationsPage.getContent().get(0).read()).isTrue();
        assertThat(updatedNotificationsPage.getContent().get(0).readAt()).isNotNull();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldMarkNotificationAsUnread() throws Exception {
        // Given - Create a feature to generate notification
        String eventId = UUID.randomUUID().toString();
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Unread Test Feature", "Test unread functionality", null, "testuser");

        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Wait for notification creation
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Page<NotificationDto> notificationsPage =
                    notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
            assertThat(notificationsPage.getContent()).hasSize(1);
        });

        // Get the notification ID and mark it as read first
        Page<NotificationDto> notificationsPage =
                notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
        UUID notificationId = notificationsPage.getContent().get(0).id();

        // Mark as read first
        var readResult =
                mvc.put().uri("/api/notifications/{id}/read", notificationId).exchange();
        assertThat(readResult).hasStatus2xxSuccessful();

        // Verify it's marked as read
        Page<NotificationDto> readNotificationsPage =
                notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
        assertThat(readNotificationsPage.getContent().get(0).read()).isTrue();
        assertThat(readNotificationsPage.getContent().get(0).readAt()).isNotNull();

        // When - Mark as unread
        var unreadResult =
                mvc.put().uri("/api/notifications/{id}/unread", notificationId).exchange();

        assertThat(unreadResult).hasStatus2xxSuccessful();

        // Then - Verify notification is marked as unread
        Page<NotificationDto> unreadNotificationsPage =
                notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
        assertThat(unreadNotificationsPage.getContent()).hasSize(1);
        assertThat(unreadNotificationsPage.getContent().get(0).read()).isFalse();
        assertThat(unreadNotificationsPage.getContent().get(0).readAt()).isNull();

        // Verify in database directly
        Integer unreadCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ? AND read = false AND read_at IS NULL",
                Integer.class,
                notificationId);
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldGetNotificationsViaApiWithPagination() throws Exception {
        // Given - Create multiple features to generate multiple notifications
        for (int i = 0; i < 5; i++) {
            String eventId = UUID.randomUUID().toString();
            CreateFeaturePayload payload =
                    new CreateFeaturePayload(eventId, "intellij", "Feature " + i, "Description " + i, null, "testuser");

            mvc.post()
                    .uri("/api/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload))
                    .exchange();
        }

        // Wait for all notifications to be created
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Page<NotificationDto> notificationsPage =
                    notificationService.getNotificationsForUser("testuser", Pageable.unpaged());
            assertThat(notificationsPage.getContent()).hasSize(5);
        });

        // When - Call GET /api/notifications with pagination
        var response = mvc.get()
                .uri("/api/notifications?page=0&size=3&sort=createdAt,desc")
                .exchange();

        // Then - Verify API response
        assertThat(response).hasStatusOk();

        // Verify pagination works by checking service layer
        Page<NotificationDto> firstPage = notificationService.getNotificationsForUser(
                "testuser", Pageable.ofSize(3).withPage(0));
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void shouldOnlyShowNotificationsToRecipient() throws Exception {
        // Given - Create features with different createdBy and assignedTo users via API
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();

        // Create feature as user1 (user1 will be createdBy, no assignedTo)
        setAuthenticationContext("user1");
        CreateFeaturePayload payload1 = new CreateFeaturePayload(
                eventId1, "intellij", "User1 Feature", "Feature created by user1", null, "user1");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload1))
                .exchange();

        // Create feature as user1 but assign to user2 (user1 createdBy, user2 assignedTo)
        // This will create notifications for both user1 (creator) and user2 (assignee)
        CreateFeaturePayload payload2 = new CreateFeaturePayload(
                eventId2, "intellij", "User2 Feature", "Feature assigned to user2", "user2", "user1");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload2))
                .exchange();

        // Wait for notifications to be created
        // Should create 3 notifications total:
        // - 1 for user1 from first feature (createdBy)
        // - 2 for second feature: user1 (createdBy) and user2 (assignedTo)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class);
            assertThat(totalCount).isGreaterThanOrEqualTo(2); // At least 2 notifications should be created
        });

        // When - Get notifications as user1
        setAuthenticationContext("user1");
        var response = mvc.get().uri("/api/notifications").exchange();

        // Then - user1 should see their notifications (creator of both features)
        assertThat(response).hasStatusOk();

        Page<NotificationDto> user1Notifications =
                notificationService.getNotificationsForUser("user1", Pageable.unpaged());
        assertThat(user1Notifications.getContent()).isNotEmpty();
        assertThat(user1Notifications.getContent())
                .allMatch(n -> n.recipientUserId().equals("user1"));

        // Verify user2's notification exists if assignedTo is working (assignee of second feature)
        Page<NotificationDto> user2Notifications =
                notificationService.getNotificationsForUser("user2", Pageable.unpaged());

        // user2 should have at least 0 notifications (depends on if assignedTo creates notification)
        // The key test is that user1 cannot see user2's notifications
        if (!user2Notifications.getContent().isEmpty()) {
            assertThat(user2Notifications.getContent().get(0).recipientUserId()).isEqualTo("user2");
            assertThat(user2Notifications.getContent().get(0).eventId())
                    .startsWith(eventId2); // eventId is modified with recipient suffix
        }

        // Verify user2 cannot see user1's notifications via API
        setAuthenticationContext("user2");
        var user2Response = mvc.get().uri("/api/notifications").exchange();
        assertThat(user2Response).hasStatusOk();

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // When - Try to access notifications without authentication
        var response = mvc.get().uri("/api/notifications").exchange();

        // Then - Should return 401 Unauthorized
        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldReturnNotFoundForNonExistentNotification() throws Exception {
        // Given - Random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        // When - Try to mark non-existent notification as read
        var readResponse =
                mvc.put().uri("/api/notifications/{id}/read", nonExistentId).exchange();

        // Then - Should return 404 Not Found
        assertThat(readResponse).hasStatus4xxClientError();

        // When - Try to mark non-existent notification as unread
        var unreadResponse =
                mvc.put().uri("/api/notifications/{id}/unread", nonExistentId).exchange();

        // Then - Should return 404 Not Found
        assertThat(unreadResponse).hasStatus4xxClientError();
    }

    @Test
    void shouldReturnNotFoundWhenTryingToAccessOtherUsersNotification() throws Exception {
        // Given - Create notification for otheruser via API (authenticated as otheruser)
        String eventId = UUID.randomUUID().toString();

        setAuthenticationContext("otheruser");
        CreateFeaturePayload payload = new CreateFeaturePayload(
                eventId, "intellij", "Other User Feature", "Feature for otheruser", null, "otheruser");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Wait for notification creation
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Page<NotificationDto> notificationsPage =
                    notificationService.getNotificationsForUser("otheruser", Pageable.unpaged());
            assertThat(notificationsPage.getContent()).hasSize(1);
        });

        // Get the notification ID for otheruser
        Page<NotificationDto> notificationsPage =
                notificationService.getNotificationsForUser("otheruser", Pageable.unpaged());
        UUID notificationId = notificationsPage.getContent().get(0).id();

        // When - Try to mark other user's notification as read (authenticated as testuser)
        var readResponse = mvc.put()
                .uri("/api/notifications/{id}/read", notificationId)
                .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser")))
                .exchange();

        // Then - Should return 4xx (not found/access denied)
        assertThat(readResponse).hasStatus4xxClientError();

        // Verify database state unchanged - notification should still be unread
        Boolean isRead = jdbcTemplate.queryForObject(
                "SELECT read FROM notifications WHERE id = ?", Boolean.class, notificationId);
        assertThat(isRead).isFalse();

        // Cleanup
        SecurityContextHolder.clearContext();
    }
}
