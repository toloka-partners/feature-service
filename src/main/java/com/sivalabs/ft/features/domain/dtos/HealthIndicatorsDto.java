package com.sivalabs.ft.features.domain.dtos;

import java.io.Serializable;

public record HealthIndicatorsDto(String riskLevel, String timelineAdherence) implements Serializable {}
