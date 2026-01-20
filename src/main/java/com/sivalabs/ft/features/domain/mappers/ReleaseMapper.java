package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.api.models.CreateReleasePayload;
import com.sivalabs.ft.features.api.models.UpdateReleasePayload;
import com.sivalabs.ft.features.domain.dtos.ReleaseDto;
import com.sivalabs.ft.features.domain.entities.Release;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReleaseMapper {
    ReleaseDto toDto(Release release);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "releasedAt", ignore = true)
    @Mapping(target = "actualReleaseDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "features", ignore = true)
    Release toEntity(CreateReleasePayload payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "features", ignore = true)
    void updateEntity(@MappingTarget Release release, UpdateReleasePayload payload);
}
