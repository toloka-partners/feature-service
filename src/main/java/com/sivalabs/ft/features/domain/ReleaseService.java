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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseService {
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

        // Validate planning dates
        validatePlanningDates(cmd.plannedStartDate(), cmd.plannedReleaseDate(), null);

        Release release = new Release();
        release.setProduct(product);
        release.setCode(code);
        release.setDescription(cmd.description());
        release.setStatus(ReleaseStatus.DRAFT);
        release.setPlannedStartDate(cmd.plannedStartDate());
        release.setPlannedReleaseDate(cmd.plannedReleaseDate());
        release.setOwner(cmd.owner());
        release.setNotes(cmd.notes());
        release.setCreatedBy(cmd.createdBy());
        release.setCreatedAt(Instant.now());
        releaseRepository.save(release);
        return code;
    }

    @Transactional
    public void updateRelease(UpdateReleaseCommand cmd) {
        Release release = releaseRepository.findByCode(cmd.code()).orElseThrow();

        // Validate status transition if status is being changed
        if (cmd.status() != null && !release.getStatus().canTransitionTo(cmd.status())) {
            throw new BadRequestException(
                    "Invalid status transition from " + release.getStatus() + " to " + cmd.status());
        }

        // Validate planning dates
        validatePlanningDates(
                cmd.plannedStartDate() != null ? cmd.plannedStartDate() : release.getPlannedStartDate(),
                cmd.plannedReleaseDate() != null ? cmd.plannedReleaseDate() : release.getPlannedReleaseDate(),
                cmd.actualReleaseDate());

        release.setDescription(cmd.description());
        if (cmd.status() != null) {
            release.setStatus(cmd.status());
        }
        release.setReleasedAt(cmd.releasedAt());
        release.setPlannedStartDate(cmd.plannedStartDate());
        release.setPlannedReleaseDate(cmd.plannedReleaseDate());
        release.setActualReleaseDate(cmd.actualReleaseDate());
        release.setOwner(cmd.owner());
        release.setNotes(cmd.notes());
        release.setUpdatedBy(cmd.updatedBy());
        release.setUpdatedAt(Instant.now());
        releaseRepository.save(release);
    }

    @Transactional
    public void deleteRelease(String code) {
        if (!releaseRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("Release with code " + code + " not found");
        }
        featureRepository.unsetRelease(code);
        releaseRepository.deleteByCode(code);
    }

    private void validatePlanningDates(
            Instant plannedStartDate, Instant plannedReleaseDate, Instant actualReleaseDate) {
        if (plannedStartDate != null && plannedReleaseDate != null) {
            if (plannedStartDate.isAfter(plannedReleaseDate)) {
                throw new BadRequestException("Planned start date cannot be after planned release date");
            }
        }

        if (actualReleaseDate != null && plannedStartDate != null) {
            if (actualReleaseDate.isBefore(plannedStartDate)) {
                throw new BadRequestException("Actual release date cannot be before planned start date");
            }
        }
    }
}
