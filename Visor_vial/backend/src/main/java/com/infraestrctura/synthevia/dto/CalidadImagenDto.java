package com.infraestrctura.synthevia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Puntuacion de los factores de calidad fotografica. El modelo debe rellenarla
 * y la confianza se deriva de ella, para que nivel_confianza no sea un numero
 * invented sin respaldo.
 */
public record CalidadImagenDto(

        @JsonProperty("encuadre")
        Integer encuadre,

        @JsonProperty("escala")
        Integer escala,

        @JsonProperty("iluminacion")
        Integer iluminacion,

        @JsonProperty("nitidez")
        Integer nitidez,

        @JsonProperty("oclusion")
        Integer oclusion
) {
}
