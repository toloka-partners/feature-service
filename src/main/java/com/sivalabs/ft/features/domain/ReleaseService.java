package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.DeleteReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.events.EventDeduplicationService;
import com.sivalabs.ft.features.domain.events.EventPublisher;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.EventType;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseService {
    private static final Logger log = LoggerFactory.getLogger(ReleaseService.class);
    public static final String RELEASE_SEPARATOR = "-";
    private final ReleaseRepository releaseRepository;
    private final ProductRepository productRepository;
    private final FeatureRepository featureRepository;
    private final ReleaseMapper releaseMapper;
    private final EventPublisher eventPublisher;
    private final EventDeduplicationService eventDeduplicationService;

    ReleaseService(
            ReleaseRepository releaseRepository,
            ProductRepository productRepository,
            FeatureRepository featureRepository,
            ReleaseMapper releaseMapper,
            EventPublisher eventPublisher,
            EventDeduplicationService eventDeduplicationService) {
        this.releaseRepository = releaseRepository;
        this.productRepository = productRepository;
        this.featureRepository = featureRepository;
        this.releaseMapper = releaseMapper;
        this.eventPublisher = eventPublisher;
        this.eventDeduplicationService = eventDeduplicationService;
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
        // Use EventDeduplicationService for API-level idempotency with result storage
        return eventDeduplicationService.executeIdempotent(cmd.eventId(), EventType.API, () -> {
            Product product = productRepository.findByCode(cmd.productCode()).orElseThrow();
            String code = cmd.code();
            if (!cmd.code().startsWith(product.getPrefix() + RELEASE_SEPARATOR)) {
                code = product.getPrefix() + RELEASE_SEPARATOR + cmd.code();
            }

            // Check if release with this code already exists
            if (releaseRepository.existsByCode(code)) {
                throw new BadRequestException("Release with code " + code + " already exists");
            }

            Release release = new Release();
            release.setProduct(product);
            release.setCode(code);
            release.setDescription(cmd.description());
            release.setStatus(ReleaseStatus.DRAFT);
            release.setCreatedBy(cmd.createdBy());
            release.setCreatedAt(Instant.now());
            release = releaseRepository.save(release);

            // Publish event for Kafka listeners
            eventPublisher.publishReleaseCreatedEvent(cmd.eventId(), release);

            log.info(
                    "Successfully created release with code: {} for API eventId: {}, event eventId: {}",
                    code,
                    cmd.eventId(),
                    cmd.eventId());

            return code;
        });
    }

    @Transactional
    public void updateRelease(UpdateReleaseCommand cmd) {
        // Use EventDeduplicationService for API-level idempotency
        eventDeduplicationService.executeIdempotent(cmd.eventId(), EventType.API, () -> {
            Release release = releaseRepository.findByCode(cmd.code()).orElseThrow();

            // Store previous status for cascade notification logic
            ReleaseStatus previousStatus = release.getStatus();

            release.setDescription(cmd.description());
            release.setStatus(cmd.status());
            release.setReleasedAt(cmd.releasedAt());
            release.setUpdatedBy(cmd.updatedBy());
            release.setUpdatedAt(Instant.now());
            release = releaseRepository.save(release);

            // Publish event for Kafka listeners with previous status
            eventPublisher.publishReleaseUpdatedEvent(cmd.eventId(), release, previousStatus);

            log.info(
                    "Successfully updated release with code: {} for API eventId: {}, event eventId: {}",
                    cmd.code(),
                    cmd.eventId(),
                    cmd.eventId());

            return "updated";
        });
    }

    @Transactional
    public void deleteRelease(DeleteReleaseCommand cmd) {
        // Use EventDeduplicationService for API-level idempotency
        eventDeduplicationService.executeIdempotent(cmd.eventId(), EventType.API, () -> {
            Release release = releaseRepository
                    .findByCode(cmd.code())
                    .orElseThrow(() -> new ResourceNotFoundException("Release with code " + cmd.code() + " not found"));

            // Publish event before deleting the release (while release still exists)
            eventPublisher.publishReleaseDeletedEvent(cmd.eventId(), release, cmd.deletedBy(), Instant.now());

            // Unset release from features and delete the release
            featureRepository.unsetRelease(cmd.code());
            releaseRepository.deleteByCode(cmd.code());

            log.info(
                    "Successfully deleted release with code: {} for API eventId: {}, event eventId: {}",
                    cmd.code(),
                    cmd.eventId(),
                    cmd.eventId());

            return "deleted";
        });
    }
}
