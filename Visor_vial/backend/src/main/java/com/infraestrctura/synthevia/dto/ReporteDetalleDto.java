package com.infraestrctura.synthevia.dto;

/**
 * Detalle completo de un reporte: la informacion base, la coordenada
 * geolocalizada y el analisis de IA. Lo consume unicamente el panel del
 * administrador.
 */
public record ReporteDetalleDto(
        ReporteResumenDto reporte,
        CoordenadaDto coordenada,
        AnalisisIaDto analisis
) {
}
