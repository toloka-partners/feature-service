package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.Commands.CreateReleaseCommand;
import com.sivalabs.ft.features.domain.Commands.UpdateReleaseCommand;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.entities.Product;
import com.sivalabs.ft.features.domain.entities.Release;
import com.sivalabs.ft.features.domain.exceptions.BadRequestException;
import com.sivalabs.ft.features.domain.exceptions.ResourceNotFoundException;
import com.sivalabs.ft.features.domain.mappers.ReleaseMapper;
import com.sivalabs.ft.features.domain.models.ReleaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    ReleaseService(
            ReleaseRepository releaseRepository,
            ProductRepository productRepository,
            FeatureRepository featureRepository,
            ReleaseMapper releaseMapper) {
        this.releaseRepository = releaseRepository;
        this.productRepository = productRepository;
        this.featureRepository = featureRepository;
        this.releaseMapper = releaseMapper;
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
        Product product = productRepository.findByCode(cmd.productCode()).orElseThrow();
        String code = cmd.code();
        if (!cmd.code().startsWith(product.getPrefix() + RELEASE_SEPARATOR)) {
            code = product.getPrefix() + RELEASE_SEPARATOR + cmd.code();
        }
        Release release = new Release();
        release.setProduct(product);
        release.setCode(code);
        release.setDescription(cmd.description());
        release.setPlannedReleaseDate(cmd.plannedReleaseDate());
        release.setStatus(ReleaseStatus.DRAFT);
        release.setCreatedBy(cmd.createdBy());
        release.setCreatedAt(Instant.now());
        releaseRepository.save(release);
        log.info("Created release with code: {} by user: {}", code, cmd.createdBy());
        return code;
    }

    @Transactional
    public void updateRelease(UpdateReleaseCommand cmd) {
        Release release = releaseRepository.findByCode(cmd.code()).orElseThrow();

        // Validate status transition if status is being changed
        if (cmd.status() != null && !release.getStatus().equals(cmd.status())) {
            validateStatusTransition(release.getStatus(), cmd.status());
            log.info(
                    "Status transition for release {}: {} -> {} by user: {}",
                    cmd.code(),
                    release.getStatus(),
                    cmd.status(),
                    cmd.updatedBy());
        }

        release.setDescription(cmd.description());
        if (cmd.status() != null) {
            release.setStatus(cmd.status());
        }
        release.setPlannedReleaseDate(cmd.plannedReleaseDate());
        release.setReleasedAt(cmd.releasedAt());
        release.setUpdatedBy(cmd.updatedBy());
        release.setUpdatedAt(Instant.now());
        releaseRepository.save(release);
        log.info("Updated release: {} by user: {}", cmd.code(), cmd.updatedBy());
    }

    @Transactional
    public void deleteRelease(String code) {
        if (!releaseRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("Release with code " + code + " not found");
        }
        featureRepository.unsetRelease(code);
        releaseRepository.deleteByCode(code);
        log.info("Deleted release: {}", code);
    }

    /**
     * Validates if status transition is allowed according to state machine rules
     */
    private void validateStatusTransition(ReleaseStatus currentStatus, ReleaseStatus targetStatus) {
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
        }
    }

    /**
     * Find releases that are overdue (past planned release date but not completed)
     */
    @Transactional(readOnly = true)
    public List<ReleaseDto> findOverdueReleases() {
        return releaseRepository.findOverdueReleases(Instant.now()).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    /**
     * Find releases that are overdue (past planned release date but not completed) with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReleaseDto> findOverdueReleases(Pageable pageable) {
        return releaseRepository.findOverdueReleases(Instant.now(), pageable).map(releaseMapper::toDto);
    }

    /**
     * Find releases that are at risk (approaching deadline within threshold days)
     */
    @Transactional(readOnly = true)
    public List<ReleaseDto> findAtRiskReleases(int daysThreshold) {
        Instant thresholdDate = Instant.now().plusSeconds(daysThreshold * 24L * 60L * 60L);
        return releaseRepository.findAtRiskReleases(Instant.now(), thresholdDate).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    /**
     * Find releases that are at risk (approaching deadline within threshold days) with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReleaseDto> findAtRiskReleases(int daysThreshold, Pageable pageable) {
        Instant thresholdDate = Instant.now().plusSeconds(daysThreshold * 24L * 60L * 60L);
        return releaseRepository
                .findAtRiskReleases(Instant.now(), thresholdDate, pageable)
                .map(releaseMapper::toDto);
    }

    /**
     * Find releases by status
     */
    @Transactional(readOnly = true)
    public List<ReleaseDto> findReleasesByStatus(ReleaseStatus status) {
        return releaseRepository.findByStatus(status).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    /**
     * Find releases by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReleaseDto> findReleasesByStatus(ReleaseStatus status, Pageable pageable) {
        return releaseRepository.findByStatus(status, pageable).map(releaseMapper::toDto);
    }

    /**
     * Find releases by owner (created by)
     */
    @Transactional(readOnly = true)
    public List<ReleaseDto> findReleasesByOwner(String owner) {
        return releaseRepository.findByCreatedBy(owner).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    /**
     * Find releases by owner (created by) with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReleaseDto> findReleasesByOwner(String owner, Pageable pageable) {
        return releaseRepository.findByCreatedBy(owner, pageable).map(releaseMapper::toDto);
    }

    /**
     * Find releases by date range
     */
    @Transactional(readOnly = true)
    public List<ReleaseDto> findReleasesByDateRange(Instant startDate, Instant endDate) {
        return releaseRepository.findByPlannedReleaseDateBetween(startDate, endDate).stream()
                .map(releaseMapper::toDto)
                .toList();
    }

    /**
     * Find releases by date range with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReleaseDto> findReleasesByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return releaseRepository
                .findByPlannedReleaseDateBetween(startDate, endDate, pageable)
                .map(releaseMapper::toDto);
    }

    /**
     * Find releases with filters and pagination
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReleaseDto> findReleasesWithFilters(
            ReleaseStatus status,
            String owner,
            Instant startDate,
            Instant endDate,
            org.springframework.data.domain.Pageable pageable) {
        return releaseRepository
                .findAllWithFilters(status, owner, startDate, endDate, pageable)
                .map(releaseMapper::toDto);
    }
}
