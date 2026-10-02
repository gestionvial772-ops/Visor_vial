package com.infraestrctura.synthevia.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resultado del analisis de IA tal como lo consume el panel del administrador.
 */
public record AnalisisIaDto(
        Integer idReporte,
        boolean disponible,
        boolean tieneDano,
        Integer cantidadDanos,
        Short severidad,
        String severidadEtiqueta,
        List<TipoDanoFilaDto> tiposDano,
        BigDecimal nivelConfianza,
        String modeloIaUtilizado,
        String versionModelo,
        String reporteBreve,
        OffsetDateTime fechaProcesamiento
) {
}
