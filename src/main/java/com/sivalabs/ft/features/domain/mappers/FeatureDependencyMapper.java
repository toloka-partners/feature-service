package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = FeatureMapper.class)
public interface FeatureDependencyMapper {
    FeatureDependencyDto toDto(FeatureDependency featureDependency);
}
