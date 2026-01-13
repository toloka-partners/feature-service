package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.NotificationDto;
import com.sivalabs.ft.features.domain.entities.Notification;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.NotificationMapper;
import com.sivalabs.ft.features.domain.models.DeliveryStatus;
import com.sivalabs.ft.features.domain.models.NotificationEventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            UserRepository userRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Create a new notification
     */
    @Transactional
    public NotificationDto createNotification(
            String recipientUserId, NotificationEventType eventType, String eventDetails, String link) {

        // Fetch user email from users table
        String recipientEmail = userRepository
                .findByUsername(recipientUserId)
                .map(user -> user.getEmail())
                .orElse(null);

        var notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setRecipientEmail(recipientEmail);
        notification.setEventType(eventType);
        notification.setEventDetails(eventDetails);
        notification.setLink(link);
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);
        notification.setDeliveryStatus(DeliveryStatus.PENDING);

        notification = notificationRepository.save(notification);

        log.info("Created notification {} for user {}", notification.getId(), recipientUserId);

        // Send email asynchronously (failure will not prevent notification creation)
        NotificationDto notificationDto = notificationMapper.toDto(notification);
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            emailService.sendNotificationEmail(notificationDto);
        } else {
            log.warn("No email address found for user {}, skipping email notification", recipientUserId);
        }

        return notificationDto;
    }

    /**
     * Create multiple notifications in a single batch operation.
     */
    @Transactional
    public List<NotificationDto> createNotificationsBatch(List<NotificationData> notificationsData) {
        if (notificationsData == null || notificationsData.isEmpty()) {
            return List.of();
        }

        List<Notification> notifications = new ArrayList<>();
        Instant now = Instant.now();

        for (NotificationData data : notificationsData) {
            // Fetch user email from users table
            String recipientEmail = userRepository
                    .findByUsername(data.recipientUserId())
                    .map(user -> user.getEmail())
                    .orElse(null);

            var notification = new Notification();
            notification.setRecipientUserId(data.recipientUserId());
            notification.setRecipientEmail(recipientEmail);
            notification.setEventType(data.eventType());
            notification.setEventDetails(data.eventDetails());
            notification.setLink(data.link());
            notification.setCreatedAt(now);
            notification.setRead(false);
            notification.setDeliveryStatus(DeliveryStatus.PENDING);
            notifications.add(notification);
        }

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        log.info("Created {} notifications in batch", savedNotifications.size());

        // Send emails asynchronously for all notifications
        List<NotificationDto> notificationDtos =
                savedNotifications.stream().map(notificationMapper::toDto).toList();

        notificationDtos.forEach(dto -> {
            if (dto.recipientEmail() != null && !dto.recipientEmail().isEmpty()) {
                emailService.sendNotificationEmail(dto);
            }
        });

        return notificationDtos;
    }

    /**
     * Data class for batch notification creation
     */
    public record NotificationData(
            String recipientUserId, NotificationEventType eventType, String eventDetails, String link) {}

    /**
     * Get notifications for a user with pagination
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForUser(String recipientUserId, Pageable pageable) {
        Page<Notification> notifications =
                notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, pageable);
        return notifications.map(notificationMapper::toDto);
    }

    /**
     * Mark notification as read
     * Only the recipient can mark their own notifications as read
     */
    @Transactional
    public NotificationDto markAsRead(UUID notificationId, String recipientUserId) {
        Instant readAt = Instant.now();
        int updated = notificationRepository.markAsRead(notificationId, recipientUserId, readAt);

        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found or access denied");
        }

        // Fetch updated notification
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        log.info("Marked notification {} as read for user {}", notificationId, recipientUserId);

        return notificationMapper.toDto(notification);
    }

    /**
     * Mark notification as unread
     * Only the recipient can mark their own notifications as unread
     */
    @Transactional
    public NotificationDto markAsUnread(UUID notificationId, String recipientUserId) {
        int updated = notificationRepository.markAsUnread(notificationId, recipientUserId);

        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found or access denied");
        }

        // Fetch updated notification
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        log.info("Marked notification {} as unread for user {}", notificationId, recipientUserId);

        return notificationMapper.toDto(notification);
    }

    /**
     * Mark notification as read via tracking pixel
     * This method is called when the email tracking pixel is loaded
     * It's idempotent - multiple calls won't update the readAt timestamp
     */
    @Transactional
    public void markAsReadViaTrackingPixel(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (!notification.getRead()) {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
                notificationRepository.save(notification);
                log.info(
                        "Notification {} marked as read via tracking pixel for user {}",
                        notificationId,
                        notification.getRecipientUserId());
            } else {
                log.debug("Notification {} already marked as read, skipping update", notificationId);
            }
        });
    }

    /**
     * Check if notification exists by ID
     */
    @Transactional(readOnly = true)
    public boolean notificationExists(UUID notificationId) {
        return notificationRepository.existsById(notificationId);
    }
}
