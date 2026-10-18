package com.infraestrctura.synthevia.dto;

public record ViaFeatureDto(
    String type,
    ViaGeometryDto geometry,
    ViaPropertiesDto properties
) {}
