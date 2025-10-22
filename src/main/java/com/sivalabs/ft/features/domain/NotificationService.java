package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Notification;
import com.sivalabs.ft.features.domain.entities.NotificationRecipient;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationRecipientRepository notificationRecipientRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationRecipientRepository = notificationRecipientRepository;
    }

    @Transactional
    public void createNotification(
            String eventId,
            String eventType,
            String title,
            String message,
            String link,
            List<String> recipients) {
        
        // Check for duplicate event
        if (notificationRepository.existsByEventId(eventId)) {
            return; // Skip if already processed
        }

        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setEventType(eventType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setCreatedAt(Instant.now());
        
        Notification savedNotification = notificationRepository.save(notification);

        // Create notification recipients
        for (String recipient : recipients) {
            NotificationRecipient notificationRecipient = new NotificationRecipient();
            notificationRecipient.setNotification(savedNotification);
            notificationRecipient.setRecipient(recipient);
            notificationRecipientRepository.save(notificationRecipient);
        }
    }

    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId) {
        return notificationRepository.existsByEventId(eventId);
    }
}