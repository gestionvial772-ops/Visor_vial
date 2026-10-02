package com.infraestrctura.synthevia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tipo de deterioro devuelto por el modelo de IA junto con la cantidad de
 * instancias observadas y la confianza estimada de la deteccion.
 */
public record TipoDanoDto(

        @JsonProperty("tipo")
        String tipo,

        @JsonProperty("cantidad")
        Integer cantidad,

        @JsonProperty("confianza")
        Integer confianza
) {
}
