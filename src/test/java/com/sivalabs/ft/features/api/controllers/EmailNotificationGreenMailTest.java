package com.sivalabs.ft.features.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sivalabs.ft.features.AbstractIT;
import com.sivalabs.ft.features.WithMockOAuth2User;
import com.sivalabs.ft.features.api.models.CreateFeaturePayload;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for email notification system using GreenMail.
 * More stable than mock-based tests as it uses real SMTP server.
 */
@Sql("/test-data.sql")
class EmailNotificationGreenMailTest extends AbstractIT {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add(
                "spring.mail.port", () -> String.valueOf(greenMail.getSmtp().getPort()));
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM notifications");
        greenMail.reset();
    }

    // ========== Test 1: Email is sent after notification creation ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldSendEmailWhenNotificationIsCreated() throws Exception {
        // Given - alice creates a feature assigned to bob
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Email Test Feature", "Test email notification", null, "bob");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Then - Wait for async email to be sent
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Verify notification was created with recipient_email
        String recipientEmail = jdbcTemplate.queryForObject(
                "SELECT recipient_email FROM notifications WHERE recipient_user_id = ?", String.class, "bob");
        assertThat(recipientEmail).isEqualTo("bob@company.com");

        // Get notification ID from database
        UUID notificationId = jdbcTemplate.queryForObject(
                "SELECT id FROM notifications WHERE recipient_user_id = ?", UUID.class, "bob");

        // Verify email was sent
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        MimeMessage sentMessage = messages[0];
        assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo("bob@company.com");
        assertThat(sentMessage.getSubject()).contains("Feature Created");

        // Verify email body contains tracking pixel link
        String emailContent = extractEmailContent(sentMessage);
        assertThat(emailContent)
                .as("Email should contain tracking pixel link with notification ID")
                .contains("/notifications/" + notificationId + "/read");
    }

    private String extractEmailContent(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof jakarta.mail.internet.MimeMultipart multipart) {
            return extractFromMultipart(multipart);
        }
        return content.toString();
    }

    private String extractFromMultipart(jakarta.mail.internet.MimeMultipart multipart) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            jakarta.mail.BodyPart part = multipart.getBodyPart(i);
            Object partContent = part.getContent();
            if (partContent instanceof String) {
                sb.append(partContent);
            } else if (partContent instanceof jakarta.mail.internet.MimeMultipart nested) {
                sb.append(extractFromMultipart(nested));
            }
        }
        return sb.toString();
    }

    // ========== Test 2: Tracking endpoint marks notification as read ==========

    @Test
    void shouldMarkNotificationAsReadWhenTrackingPixelIsLoaded() throws Exception {
        // Given - Create an unread notification directly in database
        UUID notificationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, recipient_user_id, recipient_email, event_type, event_details, created_at, read, delivery_status)
                VALUES (?, 'testuser', 'testuser@example.com', 'FEATURE_CREATED', '{}', NOW(), false, 'PENDING')
                """,
                notificationId);

        // When - Load tracking pixel
        var result = mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        // Then - Should return 200 OK with GIF image
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getContentType()).isEqualTo("image/gif");

        // Verify notification is marked as read in database
        Boolean isRead = jdbcTemplate.queryForObject(
                "SELECT read FROM notifications WHERE id = ?", Boolean.class, notificationId);
        assertThat(isRead).isTrue();

        // Verify read_at is populated
        java.sql.Timestamp readAt = jdbcTemplate.queryForObject(
                "SELECT read_at FROM notifications WHERE id = ?", java.sql.Timestamp.class, notificationId);
        assertThat(readAt).isNotNull();
    }

    // ========== Test 3: Tracking endpoint is accessible without authentication ==========

    @Test
    void shouldAllowTrackingEndpointWithoutAuthentication() throws Exception {
        // Given - Create notification (no @WithMockOAuth2User = unauthenticated request)
        UUID notificationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, recipient_user_id, recipient_email, event_type, event_details, created_at, read, delivery_status)
                VALUES (?, 'testuser', 'testuser@example.com', 'FEATURE_CREATED', '{}', NOW(), false, 'PENDING')
                """,
                notificationId);

        // When - Access tracking endpoint without authentication
        var result = mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        // Then - Should return 200 OK (not 401 Unauthorized)
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getContentType()).isEqualTo("image/gif");
    }

    // ========== Test 4: Email failure handling ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldCreateNotificationEvenWhenEmailSendingFails() throws Exception {
        // Given - Stop GreenMail to simulate email failure
        greenMail.stop();

        CreateFeaturePayload payload = new CreateFeaturePayload(
                "intellij", "Email Failure Test", "Test notification despite email failure", null, "bob");

        // When - Create feature (email sending will fail)
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Then - Feature should still be created
        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Notification should be saved in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ?", Integer.class, "bob");
        assertThat(count).isEqualTo(1);

        // Restart GreenMail for other tests
        greenMail.start();
    }

    // ========== Test 5: Tracking endpoint returns 404 for non-existent notification ==========

    @Test
    void shouldReturn404ForNonExistentNotificationId() throws Exception {
        // Given - Random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        // When - Try to load tracking pixel for non-existent notification
        var result = mvc.get().uri("/notifications/{id}/read", nonExistentId).exchange();

        // Then - Should return 404 Not Found
        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    // ========== Test 6: Tracking endpoint returns 400 for invalid UUID format ==========

    @Test
    void shouldReturn400ForInvalidUuidFormat() throws Exception {
        // When - Try to load tracking pixel with invalid UUID
        var result =
                mvc.get().uri("/notifications/{id}/read", "not-a-valid-uuid").exchange();

        // Then - Should return 400 Bad Request
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);

        // Response should not expose internal details (stack traces)
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("IllegalArgumentException");
        assertThat(responseBody).doesNotContain("java.util.UUID");
        assertThat(responseBody).doesNotContain("Exception");
    }

    // ========== Test 7: Tracking endpoint is idempotent ==========

    @Test
    void shouldBeIdempotentWhenCalledMultipleTimes() throws Exception {
        // Given - Create an unread notification
        UUID notificationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, recipient_user_id, recipient_email, event_type, event_details, created_at, read, delivery_status)
                VALUES (?, 'testuser', 'testuser@example.com', 'FEATURE_CREATED', '{}', NOW(), false, 'PENDING')
                """,
                notificationId);

        // When - Call tracking endpoint first time
        var firstResult =
                mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        assertThat(firstResult).hasStatus(HttpStatus.OK);

        // Get the read_at timestamp after first call
        java.sql.Timestamp firstReadAt = jdbcTemplate.queryForObject(
                "SELECT read_at FROM notifications WHERE id = ?", java.sql.Timestamp.class, notificationId);

        // When - Call tracking endpoint second time
        var secondResult =
                mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        // Then - Should still return 200 OK
        assertThat(secondResult).hasStatus(HttpStatus.OK);

        // read_at should NOT be updated (idempotent)
        java.sql.Timestamp secondReadAt = jdbcTemplate.queryForObject(
                "SELECT read_at FROM notifications WHERE id = ?", java.sql.Timestamp.class, notificationId);

        assertThat(secondReadAt).isEqualTo(firstReadAt);
    }

    // ========== Test 8: Notification contains recipient_email in database ==========

    @Test
    @WithMockOAuth2User(username = "creator")
    void shouldStoreRecipientEmailInNotification() throws Exception {
        // Given
        CreateFeaturePayload payload = new CreateFeaturePayload(
                "intellij", "Recipient Email Test", "Test recipient email storage", null, "recipient");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async email
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Then - Verify recipient_email column is populated
        String recipientEmail = jdbcTemplate.queryForObject(
                "SELECT recipient_email FROM notifications WHERE recipient_user_id = ?", String.class, "recipient");

        assertThat(recipientEmail)
                .as("recipient_email should match email from users table")
                .isEqualTo("recipient@company.com");

        // Verify email was actually sent
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("recipient@company.com");
    }

    // ========== Test 9: Tracking endpoint returns valid GIF image ==========

    @Test
    void shouldReturnValidGifImage() throws Exception {
        // Given - Create notification
        UUID notificationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, recipient_user_id, recipient_email, event_type, event_details, created_at, read, delivery_status)
                VALUES (?, 'testuser', 'testuser@example.com', 'FEATURE_CREATED', '{}', NOW(), false, 'PENDING')
                """,
                notificationId);

        // When - Load tracking pixel
        var result = mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        // Then - Should return valid GIF
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getContentType()).isEqualTo("image/gif");

        // Verify response body is not empty (actual GIF content)
        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();
    }

    // ========== Test 10: XSS prevention - HTML should be escaped in email content ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldEscapeHtmlInEmailContent() throws Exception {
        // Given - Create feature with potentially malicious HTML content
        String maliciousTitle = "<script>alert('XSS')</script>Malicious Feature";
        String maliciousDescription = "<img src=x onerror=alert('XSS')>Description";
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", maliciousTitle, maliciousDescription, null, "bob");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async email
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Then - Capture email and verify HTML is escaped
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        String emailContent = extractEmailContent(messages[0]);

        // Verify malicious tags are escaped (not present as raw HTML)
        assertThat(emailContent).doesNotContain("<script>");
        assertThat(emailContent).doesNotContain("onerror=");
        assertThat(emailContent).doesNotContain("<img src=x");
    }

    // ========== Test 11: Email should include all required fields ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldIncludeAllRequiredFieldsInEmail() throws Exception {
        // Given
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Complete Email Test", "Full email validation", null, "bob");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async email
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Capture email
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        String emailContent = extractEmailContent(messages[0]);

        // Verify required fields are present (per Task Description)
        // 1. Link to affected entity
        assertThat(emailContent).as("Email should contain link to feature").contains("/features/");
        // 2. Event summary (feature title or code should be present)
        assertThat(emailContent)
                .as("Email should contain event summary with feature info")
                .satisfiesAnyOf(
                        content -> assertThat(content).contains("Complete Email Test"),
                        content -> assertThat(content).contains("IDEA-"));
        // 3. Actor (who triggered the event)
        assertThat(emailContent)
                .as("Email should contain actor who triggered the event")
                .contains("alice");
    }

    // ========== Test 12: No email sent when user not found in users table ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldNotSendEmailWhenUserNotFoundInUsersTable() throws Exception {
        // Given - Create feature assigned to user that does NOT exist in users table
        CreateFeaturePayload payload = new CreateFeaturePayload(
                "intellij", "Feature for Unknown User", "Test unknown user handling", null, "nonexistent_user");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        // Then - Feature should be created successfully
        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait a bit to ensure no email is sent
        Thread.sleep(1000);

        // No email should be sent for user not in users table
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(0);
    }

    // ========== Test 13: Tracking pixel returns Cache-Control header ==========

    @Test
    void shouldReturnCacheControlHeaderForTrackingPixel() throws Exception {
        // Given - Create notification
        UUID notificationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, recipient_user_id, recipient_email, event_type, event_details, created_at, read, delivery_status)
                VALUES (?, 'testuser', 'testuser@example.com', 'FEATURE_CREATED', '{}', NOW(), false, 'PENDING')
                """,
                notificationId);

        // When - Load tracking pixel
        var result = mvc.get().uri("/notifications/{id}/read", notificationId).exchange();

        // Then - Cache-Control must prevent caching (required for tracking to work correctly)
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertThat(cacheControl)
                .as("Cache-Control header must be present to prevent pixel caching")
                .isNotNull()
                .satisfiesAnyOf(s -> assertThat(s.toLowerCase()).contains("no-cache"), s -> assertThat(s.toLowerCase())
                        .contains("no-store"));
    }

    // ========== Test 14: Email delivery status tracking ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldTrackEmailDeliveryStatus() throws Exception {
        // Given
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Delivery Status Test", "Test delivery tracking", null, "bob");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async email processing
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Then - Verify delivery status is updated to DELIVERED
        String deliveryStatus = jdbcTemplate.queryForObject(
                "SELECT delivery_status FROM notifications WHERE recipient_user_id = ?", String.class, "bob");
        assertThat(deliveryStatus).isEqualTo("DELIVERED");

        // Verify email was actually received
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
    }

    // ========== Test 15: Email contains tracking pixel ==========

    @Test
    @WithMockOAuth2User(username = "alice")
    void shouldEmbedTrackingPixelInEmail() throws Exception {
        // Given
        CreateFeaturePayload payload =
                new CreateFeaturePayload("intellij", "Tracking Pixel Test", "Test tracking pixel", null, "bob");

        // When - Create feature
        var result = mvc.post()
                .uri("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        // Wait for async email
        await().atMost(Duration.ofSeconds(5)).until(() -> greenMail.getReceivedMessages().length > 0);

        // Get notification ID
        UUID notificationId = jdbcTemplate.queryForObject(
                "SELECT id FROM notifications WHERE recipient_user_id = ?", UUID.class, "bob");

        // Then - Verify email contains tracking pixel
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        String emailContent = extractEmailContent(messages[0]);
        assertThat(emailContent)
                .as("Email should contain tracking pixel with notification ID")
                .contains("http://localhost:8081/notifications/" + notificationId + "/read");
    }
}
