package com.infraestrctura.synthevia.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Coordenada capturada por el dispositivo, expuesta en el detalle del reporte.
 */
public record CoordenadaDto(
        Double latitud,
        Double longitud,
        BigDecimal precisionReportadaMetros,
        OffsetDateTime fechaCaptura
) {
}
