package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import com.sivalabs.ft.features.domain.entities.UsageEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsageEventMapper {
    UsageEventDto toDto(UsageEvent usageEvent);
}
