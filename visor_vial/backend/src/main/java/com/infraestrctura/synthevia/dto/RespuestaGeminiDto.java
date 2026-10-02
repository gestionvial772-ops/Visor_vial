package com.infraestrctura.synthevia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Estructura exacta que Gemini debe devolver. Se declara como
 * responseSchema en la peticion, de modo que la API responde con JSON validado
 * y no con texto libre.
 */
public record RespuestaGeminiDto(

        @JsonProperty("tiene_dano")
        Boolean tieneDano,

        @JsonProperty("cantidad_danos")
        Integer cantidadDanos,

        /** 1 = LEVE, 2 = MODERADO, 3 = SEVERO */
        @JsonProperty("severidad")
        Integer severidad,

        @JsonProperty("nivel_confianza")
        Integer nivelConfianza,

        @JsonProperty("tipos_deteccion")
        List<TipoDanoDto> tiposDeteccion,

        @JsonProperty("calidad_imagen")
        CalidadImagenDto calidadImagen,

        @JsonProperty("reporte_breve")
        String reporteBreve
) {
}
