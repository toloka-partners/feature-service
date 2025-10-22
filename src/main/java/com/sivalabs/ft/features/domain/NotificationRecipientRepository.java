package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
}