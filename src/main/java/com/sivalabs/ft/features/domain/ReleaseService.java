package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.DeleteReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseService {
    public static final String RELEASE_SEPARATOR = "-";
    private final ReleaseRepository releaseRepository;
    private final ProductRepository productRepository;
    private final FeatureRepository featureRepository;
    private final ReleaseMapper releaseMapper;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;

    ReleaseService(
            ReleaseRepository releaseRepository,
            ProductRepository productRepository,
            FeatureRepository featureRepository,
            ReleaseMapper releaseMapper,
            EventPublisher eventPublisher,
            NotificationService notificationService) {
        this.releaseRepository = releaseRepository;
        this.productRepository = productRepository;
        this.featureRepository = featureRepository;
        this.releaseMapper = releaseMapper;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReleaseDto> findReleasesByProductCode(String productCode) {
        return releaseRepository.findByProductCode(productCode).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReleaseDto> findReleaseByCode(String code) {
        return releaseRepository.findByCode(code).map(releaseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public boolean isReleaseExists(String code) {
        return releaseRepository.existsByCode(code);
    }

    @Transactional
    public String createRelease(CreateReleaseCommand cmd) {
        // Check for duplicate event
        if (cmd.eventId() != null && notificationService.isEventProcessed(cmd.eventId())) {
            // Return existing release code if found
            return releaseRepository.findByCode(cmd.code())
                    .map(Release::getCode)
                    .orElse(cmd.code());
        }

        Product product = productRepository.findByCode(cmd.productCode()).orElseThrow();
        String code = cmd.code();
        if (!cmd.code().startsWith(product.getPrefix() + RELEASE_SEPARATOR)) {
            code = product.getPrefix() + RELEASE_SEPARATOR + cmd.code();
        }
        Release release = new Release();
        release.setProduct(product);
        release.setCode(code);
        release.setDescription(cmd.description());
        release.setStatus(ReleaseStatus.DRAFT);
        release.setCreatedBy(cmd.createdBy());
        release.setCreatedAt(Instant.now());
        Release savedRelease = releaseRepository.save(release);
        
        // Publish event if eventId is provided (API operations)
        if (cmd.eventId() != null) {
            eventPublisher.publishReleaseCreatedEvent(savedRelease, cmd.eventId());
        }
        
        return code;
    }

    @Transactional
    public void updateRelease(UpdateReleaseCommand cmd) {
        // Check for duplicate event
        if (cmd.eventId() != null && notificationService.isEventProcessed(cmd.eventId())) {
            return;
        }

        Release release = releaseRepository.findByCode(cmd.code()).orElseThrow();
        release.setDescription(cmd.description());
        release.setStatus(cmd.status());
        release.setReleasedAt(cmd.releasedAt());
        release.setUpdatedBy(cmd.updatedBy());
        release.setUpdatedAt(Instant.now());
        Release savedRelease = releaseRepository.save(release);
        
        // Publish event if eventId is provided (API operations)
        if (cmd.eventId() != null) {
            eventPublisher.publishReleaseUpdatedEvent(savedRelease, cmd.eventId());
        }
    }

    @Transactional
    public void deleteRelease(DeleteReleaseCommand cmd) {
        // Check for duplicate event
        if (cmd.eventId() != null && notificationService.isEventProcessed(cmd.eventId())) {
            return;
        }

        Release release = releaseRepository.findByCode(cmd.code())
                .orElseThrow(() -> new ResourceNotFoundException("Release with code " + cmd.code() + " not found"));
        
        featureRepository.unsetRelease(cmd.code());
        releaseRepository.deleteByCode(cmd.code());
        
        // Publish event if eventId is provided (API operations)
        if (cmd.eventId() != null) {
            eventPublisher.publishReleaseDeletedEvent(release, cmd.deletedBy(), Instant.now(), cmd.eventId());
        }
    }

    // Keep backward compatibility for existing callers
    @Transactional
    public void deleteRelease(String code) {
        DeleteReleaseCommand cmd = new DeleteReleaseCommand(code, "system", null);
        deleteRelease(cmd);
    }
}
