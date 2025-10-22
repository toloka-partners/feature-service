package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);
}