package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;
import java.util.List;

public record MultiProductRoadmapResponseDto(List<ProductRoadmapDto> products) implements Serializable {}
