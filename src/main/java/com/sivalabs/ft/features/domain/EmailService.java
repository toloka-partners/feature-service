package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.dtos.NotificationDto;

/**
 * Service interface for sending email notifications
 */
public interface EmailService {
    /**
     * Send notification email to recipient
     * This method is asynchronous and failures will not prevent notification creation
     *
     * @param notification The notification details to send
     */
    void sendNotificationEmail(NotificationDto notification);
}
