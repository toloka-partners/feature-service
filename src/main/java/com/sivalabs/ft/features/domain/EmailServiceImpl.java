package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.ApplicationProperties;
import com.sivalabs.ft.features.domain.dtos.NotificationDto;
import com.sivalabs.ft.features.domain.models.DeliveryStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of EmailService for sending notification emails
 * Sends emails asynchronously and logs failures without blocking notification creation
 */
@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final ApplicationProperties applicationProperties;
    private final NotificationRepository notificationRepository;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            ApplicationProperties applicationProperties,
            NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.applicationProperties = applicationProperties;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Async
    @Transactional
    public void sendNotificationEmail(NotificationDto notification) {
        if (notification.recipientEmail() == null
                || notification.recipientEmail().isEmpty()) {
            log.warn("Cannot send email for notification {}: recipient email is null or empty", notification.id());
            updateDeliveryStatus(notification.id(), DeliveryStatus.FAILED);
            return;
        }

        try {
            MimeMessage message = createEmailMessage(notification);
            mailSender.send(message);

            log.info(
                    "Email sent successfully to {} for notification {} (event: {})",
                    notification.recipientEmail(),
                    notification.id(),
                    notification.eventType());

            updateDeliveryStatus(notification.id(), DeliveryStatus.DELIVERED);

        } catch (Exception e) {
            // Log failure with full context but don't throw exception
            // Email delivery failure must not prevent notification creation
            log.error(
                    "Email delivery failed for notification {}: recipient={}, eventType={}, timestamp={}, error={}",
                    notification.id(),
                    notification.recipientEmail(),
                    notification.eventType(),
                    notification.createdAt(),
                    e.getMessage(),
                    e);

            updateDeliveryStatus(notification.id(), DeliveryStatus.FAILED);
        }
    }

    /**
     * Create MIME email message with HTML content and tracking pixel
     */
    private MimeMessage createEmailMessage(NotificationDto notification) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(notification.recipientEmail());
        helper.setSubject(EmailTemplateBuilder.buildSubject(notification));
        helper.setFrom("noreply@feature-tracker.com");

        // Build tracking pixel URL
        String trackingPixelUrl =
                applicationProperties.publicBaseUrl() + "/notifications/" + notification.id() + "/read";

        // Set HTML content
        String htmlContent = EmailTemplateBuilder.buildEmailHtml(notification, trackingPixelUrl);
        helper.setText(EmailTemplateBuilder.buildEmailText(notification), htmlContent);

        return message;
    }

    /**
     * Update notification delivery status
     */
    private void updateDeliveryStatus(java.util.UUID notificationId, DeliveryStatus status) {
        try {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                notification.setDeliveryStatus(status);
                notificationRepository.save(notification);
            });
        } catch (Exception e) {
            log.error("Failed to update delivery status for notification {}: {}", notificationId, e.getMessage());
        }
    }
}
