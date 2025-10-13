package com.sivalabs.ft.features.domain.mappers;

import com.sivalabs.ft.features.domain.UsageEvent;
import com.sivalabs.ft.features.domain.dtos.UsageEventDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsageEventMapper {
    UsageEventDto toDto(UsageEvent usageEvent);
}
