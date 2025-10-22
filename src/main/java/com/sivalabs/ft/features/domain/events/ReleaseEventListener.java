package com.sivalabs.ft.features.domain.events;

import com.sivalabs.ft.features.domain.FeatureRepository;
import com.sivalabs.ft.features.domain.NotificationService;
import com.sivalabs.ft.features.domain.entities.Feature;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReleaseEventListener {
    private static final Logger log = LoggerFactory.getLogger(ReleaseEventListener.class);
    
    private final NotificationService notificationService;
    private final FeatureRepository featureRepository;

    public ReleaseEventListener(NotificationService notificationService, FeatureRepository featureRepository) {
        this.notificationService = notificationService;
        this.featureRepository = featureRepository;
    }

    @KafkaListener(topics = "${ft.events.new-releases}")
    @Transactional
    public void handleReleaseCreatedEvent(ReleaseCreatedEvent event) {
        log.info("Processing ReleaseCreatedEvent: {}", event.eventId());
        
        if (notificationService.isEventProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        String title = "New Release Created: " + event.code();
        String message = "A new release '" + event.code() + "' has been created in product " + event.productCode();
        String link = "/releases/" + event.code();

        notificationService.createNotification(
                event.eventId(),
                "RELEASE_CREATED",
                title,
                message,
                link,
                List.of(event.createdBy())
        );
        
        log.info("Successfully processed ReleaseCreatedEvent: {}", event.eventId());
    }

    @KafkaListener(topics = "${ft.events.updated-releases}")
    @Transactional
    public void handleReleaseUpdatedEvent(ReleaseUpdatedEvent event) {
        log.info("Processing ReleaseUpdatedEvent: {}", event.eventId());
        
        if (notificationService.isEventProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        String title = "Release Updated: " + event.code();
        String message = "Release '" + event.code() + "' has been updated";
        String link = "/releases/" + event.code();

        // Get recipients: release.createdBy
        List<String> recipients = List.of(event.createdBy());

        // Handle cascade notifications when release status changes to RELEASED
        if (event.status() == ReleaseStatus.RELEASED) {
            title = "Release Published: " + event.code();
            message = "Release '" + event.code() + "' has been published!";
            
            // Get all users with features in this release for cascade notifications
            List<Feature> featuresInRelease = featureRepository.findByReleaseCode(event.code());
            Set<String> featureUsers = featuresInRelease.stream()
                    .flatMap(feature -> {
                        return List.of(
                                feature.getCreatedBy(),
                                feature.getAssignedTo()
                        ).stream().filter(user -> user != null && !user.isEmpty());
                    })
                    .collect(Collectors.toSet());
            
            // Add release creator to the set
            featureUsers.add(event.createdBy());
            recipients = featureUsers.stream().toList();
        }

        notificationService.createNotification(
                event.eventId(),
                "RELEASE_UPDATED",
                title,
                message,
                link,
                recipients
        );
        
        log.info("Successfully processed ReleaseUpdatedEvent: {}", event.eventId());
    }

    @KafkaListener(topics = "${ft.events.deleted-releases}")
    @Transactional
    public void handleReleaseDeletedEvent(ReleaseDeletedEvent event) {
        log.info("Processing ReleaseDeletedEvent: {}", event.eventId());
        
        if (notificationService.isEventProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        String title = "Release Deleted: " + event.code();
        String message = "Release '" + event.code() + "' has been deleted";
        String link = "/releases"; // No specific link since release is deleted

        notificationService.createNotification(
                event.eventId(),
                "RELEASE_DELETED",
                title,
                message,
                link,
                List.of(event.createdBy())
        );
        
        log.info("Successfully processed ReleaseDeletedEvent: {}", event.eventId());
    }
}