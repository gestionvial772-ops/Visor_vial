package com.infraestrctura.synthevia.dto;

/** Conteos del panel de usuarios, para las tarjetas de resumen. */
public record EstadisticasUsuariosDto(
        long total,
        long ciudadanos,
        long administradores,
        long activosHoy
) {}