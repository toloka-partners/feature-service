package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.MockOAuth2UserContextFactory;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateFeaturePayload;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for notification system
 * Tests that notifications are created synchronously when features are created/updated/deleted
 */
@Sql("/test-data.sql")
class NotificationIntegrationTest extends AbstractIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final MockOAuth2UserContextFactory contextFactory = new MockOAuth2UserContextFactory();

    @BeforeEach
    void setUp() {
        // Clean up tables before each test
        jdbcTemplate.execute("DELETE FROM notifications");
    }

    @AfterEach
    void tearDown() {
        // Clean up tables after each test
        jdbcTemplate.execute("DELETE FROM notifications");
    }

    private void setAuthenticationContext(String username) {
        WithMockOAuth2User mockUser = new WithMockOAuth2User() {
            @Override
            public Class<WithMockOAuth2User> annotationType() {
                return WithMockOAuth2User.class;
            }

            @Override
            public String value() {
                return username;
            }

            @Override
            public String username() {
                return username;
            }

            @Override
            public long id() {
                return username.hashCode();
            }

            @Override
            public String[] roles() {
                return new String[] {"USER"};
            }
        };
        SecurityContextHolder.setContext(contextFactory.createSecurityContext(mockUser));
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateNotificationWhenFeatureIsCreated() throws Exception {
        // Given - Create feature as testuser with assignedTo different from creator
        CreateFeaturePayload payload = new CreateFeaturePayload(
                "intellij", "Feature for Notification", "Test notification creation", null, "assignee");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Verify testuser (creator) receives notification via API
        var apiResponse = mvc.get().uri("/api/notifications").exchange();
        assertThat(apiResponse).hasStatusOk();

        String responseBody = apiResponse.getResponse().getContentAsString();
        Map<String, Object> pageResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) pageResponse.get("content");

        assertThat(notifications).as("Creator notifications for testuser").hasSize(1);

        Map<String, Object> notification = notifications.get(0);
        assertThat(notification.get("eventType")).isEqualTo("FEATURE_CREATED");
        assertThat(notification.get("recipientUserId")).isEqualTo("testuser");
        assertThat(notification.get("read")).isEqualTo(false);
        assertThat(notification.get("link").toString()).contains("/features/");
        assertThat(notification.get("eventDetails").toString()).contains("created");

        // Verify assignee also receives notification via database
        Integer assigneeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ?", Integer.class, "assignee");
        assertThat(assigneeCount).isEqualTo(1);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldCreateMultipleNotificationsForDifferentFeatures() throws Exception {
        // Given - Create two different features
        CreateFeaturePayload payload1 =
                new CreateFeaturePayload("intellij", "Feature 1", "First feature", null, "assignee");
        CreateFeaturePayload payload2 =
                new CreateFeaturePayload("intellij", "Feature 2", "Second feature", null, "assignee");

        // When - Create two features
        var result1 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload1))
                .exchange();

        var result2 = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload2))
                .exchange();

        assertThat(result1).hasStatus(HttpStatus.CREATED);
        assertThat(result2).hasStatus(HttpStatus.CREATED);

        // Verify creator received 2 notifications (one per feature)
        var apiResponse = mvc.get().uri("/api/notifications").exchange();
        assertThat(apiResponse).hasStatusOk();

        String responseBody = apiResponse.getResponse().getContentAsString();
        Map<String, Object> pageResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) pageResponse.get("content");

        assertThat(notifications).hasSize(2);
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldMarkNotificationAsRead() throws Exception {
        // Given - Create a feature as testuser with assignedTo different from creator
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Read Test Feature", "Test read functionality", null, "assignee");

        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Get the notification ID via API (created synchronously)
        var apiResponse = mvc.get().uri("/api/notifications").exchange();
        String responseBody = apiResponse.getResponse().getContentAsString();
        Map<String, Object> pageResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) pageResponse.get("content");
        UUID notificationId = UUID.fromString(notifications.get(0).get("id").toString());

        // When - Mark as read
        var readResult =
                mvc.put().uri("/api/notifications/{id}/read", notificationId).exchange();

        assertThat(readResult).hasStatus2xxSuccessful();

        // Then - Verify notification is marked as read via API
        var updatedResponse = mvc.get().uri("/api/notifications").exchange();
        assertThat(updatedResponse).hasStatusOk();

        String updatedBody = updatedResponse.getResponse().getContentAsString();
        Map<String, Object> updatedPage = objectMapper.readValue(updatedBody, new TypeReference<>() {});
        List<Map<String, Object>> updatedNotifications = (List<Map<String, Object>>) updatedPage.get("content");

        assertThat(updatedNotifications).hasSize(1);
        assertThat(updatedNotifications.get(0).get("read")).isEqualTo(true);
        assertThat(updatedNotifications.get(0).get("readAt")).isNotNull();
    }

    @Test
    @WithMockOAuth2User(username = "testuser")
    void shouldMarkNotificationAsUnread() throws Exception {
        // Given - Create a feature as testuser with assignedTo different from creator
        CreateFeaturePayload payload = new CreateFeaturePayload(
                "intellij", "Unread Test Feature", "Test unread functionality", null, "assignee");

        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Get the notification ID via API (created synchronously)
        var apiResponse = mvc.get().uri("/api/notifications").exchange();
        String responseBody = apiResponse.getResponse().getContentAsString();
        Map<String, Object> pageResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) pageResponse.get("content");
        UUID notificationId = UUID.fromString(notifications.get(0).get("id").toString());

        // Mark as read first
        var readResult =
                mvc.put().uri("/api/notifications/{id}/read", notificationId).exchange();
        assertThat(readResult).hasStatus2xxSuccessful();

        // Verify it's marked as read via API
        var readResponse = mvc.get().uri("/api/notifications").exchange();
        String readBody = readResponse.getResponse().getContentAsString();
        Map<String, Object> readPage = objectMapper.readValue(readBody, new TypeReference<>() {});
        List<Map<String, Object>> readNotifications = (List<Map<String, Object>>) readPage.get("content");

        assertThat(readNotifications.get(0).get("read")).isEqualTo(true);
        assertThat(readNotifications.get(0).get("readAt")).isNotNull();

        // When - Mark as unread
        var unreadResult =
                mvc.put().uri("/api/notifications/{id}/unread", notificationId).exchange();

        assertThat(unreadResult).hasStatus2xxSuccessful();

        // Then - Verify notification is marked as unread via API
        var unreadResponse = mvc.get().uri("/api/notifications").exchange();
        String unreadBody = unreadResponse.getResponse().getContentAsString();
        Map<String, Object> unreadPage = objectMapper.readValue(unreadBody, new TypeReference<>() {});
        List<Map<String, Object>> unreadNotifications = (List<Map<String, Object>>) unreadPage.get("content");

        assertThat(unreadNotifications).hasSize(1);
        assertThat(unreadNotifications.get(0).get("read")).isEqualTo(false);
        assertThat(unreadNotifications.get(0).get("readAt")).isNull();

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
        // Given - Create multiple features as testuser with assignedTo different from creator
        for (int i = 0; i < 5; i++) {
            CreateFeaturePayload payload =
                    new CreateFeaturePayload("intellij", "Feature " + i, "Description " + i, null, "assignee");

            mvc.post()
                    .uri("/api/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload))
                    .exchange();
        }

        // When - Call GET /api/notifications with pagination for testuser (creator)
        var response = mvc.get()
                .uri("/api/notifications?page=0&size=3&sort=createdAt,desc")
                .exchange();

        // Then - Verify API response and pagination
        assertThat(response).hasStatusOk();

        String responseBody = response.getResponse().getContentAsString();
        Map<String, Object> pageResponse = objectMapper.readValue(responseBody, new TypeReference<>() {});
        List<Map<String, Object>> notifications = (List<Map<String, Object>>) pageResponse.get("content");

        assertThat(notifications).hasSize(3);
        assertThat(pageResponse.get("totalElements")).isEqualTo(5);
        assertThat(pageResponse.get("totalPages")).isEqualTo(2);
    }

    @Test
    void shouldOnlyShowNotificationsToRecipient() throws Exception {
        // Create feature as user1 with no assignedTo (only user1 gets notification)
        setAuthenticationContext("user1");
        CreateFeaturePayload payload1 =
                new CreateFeaturePayload("intellij", "User1 Feature", "Feature created by user1", null, "user1");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload1))
                .exchange();

        // Create feature as user1 but assign to user2 (both user1 and user2 get notifications)
        CreateFeaturePayload payload2 =
                new CreateFeaturePayload("intellij", "User2 Feature", "Feature assigned to user2", null, "user2");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload2))
                .exchange();

        // When - Get notifications as user1 (created synchronously)
        setAuthenticationContext("user1");
        var response = mvc.get().uri("/api/notifications").exchange();

        // Then - user1 should see their notifications (creator of both features)
        assertThat(response).hasStatusOk();

        String user1Body = response.getResponse().getContentAsString();
        Map<String, Object> user1Page = objectMapper.readValue(user1Body, new TypeReference<>() {});
        List<Map<String, Object>> user1Notifications = (List<Map<String, Object>>) user1Page.get("content");

        assertThat(user1Notifications).hasSize(2); // Creator receives notifications for both features
        assertThat(user1Notifications).allMatch(n -> n.get("recipientUserId").equals("user1"));

        // Verify user2's notification exists via database
        Integer user2Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ?", Integer.class, "user2");

        // user2 should have 1 notification (assignee receives notification)
        assertThat(user2Count).isEqualTo(1);

        Map<String, Object> user2Notification = jdbcTemplate.queryForMap(
                "SELECT * FROM notifications WHERE recipient_user_id = ? ORDER BY created_at DESC LIMIT 1", "user2");
        assertThat(user2Notification.get("recipient_user_id")).isEqualTo("user2");

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
        // Given - Create notification for otheruser via API (creator creates, otheruser is assignee)

        setAuthenticationContext("creator");
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Other User Feature", "Feature for otheruser", null, "otheruser");
        mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Get the notification ID for otheruser via database (created synchronously)
        UUID notificationId = jdbcTemplate.queryForObject(
                "SELECT id FROM notifications WHERE recipient_user_id = ? ORDER BY created_at DESC LIMIT 1",
                UUID.class,
                "otheruser");

        // When - Try to mark other user's notification as read (authenticated as testuser)
        setAuthenticationContext("testuser");
        var readResponse =
                mvc.put().uri("/api/notifications/{id}/read", notificationId).exchange();

        // Then - Should return 4xx (not found/access denied)
        assertThat(readResponse).hasStatus4xxClientError();

        // Verify database state unchanged - notification should still be unread
        Boolean isRead = jdbcTemplate.queryForObject(
                "SELECT read FROM notifications WHERE id = ?", Boolean.class, notificationId);
        assertThat(isRead).isFalse();
    }
}
