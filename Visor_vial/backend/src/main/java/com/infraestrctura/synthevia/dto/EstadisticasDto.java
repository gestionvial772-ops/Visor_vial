package com.infraestrctura.synthevia.dto;

public record EstadisticasDto(
    long total,
    long pendientes,
    long enProceso,
    long resueltos,
    long duplicados
) {}
