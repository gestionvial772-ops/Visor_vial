package com.infraestrctura.synthevia.dto;

import java.util.List;

public record ViaGeoJsonDto(
    String type,
    List<ViaFeatureDto> features
) {}
