package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.domain.dtos.DependencyDto;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DependencyMapper {

    @Mapping(source = "feature.code", target = "featureCode")
    @Mapping(source = "dependsOnFeature.code", target = "dependsOnFeatureCode")
    DependencyDto toDto(FeatureDependency dependency);
}
