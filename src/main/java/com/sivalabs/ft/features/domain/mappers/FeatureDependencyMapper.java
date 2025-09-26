package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.domain.dtos.FeatureDependencyDto;
import com.sivalabs.ft.features.domain.entities.FeatureDependency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FeatureDependencyMapper {

    @Mapping(source = "feature.code", target = "featureCode")
    @Mapping(source = "feature.title", target = "featureTitle")
    @Mapping(source = "dependsOnFeature.code", target = "dependsOnFeatureCode")
    @Mapping(source = "dependsOnFeature.title", target = "dependsOnFeatureTitle")
    FeatureDependencyDto toDto(FeatureDependency featureDependency);
}
