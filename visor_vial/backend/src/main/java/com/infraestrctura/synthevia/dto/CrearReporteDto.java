package com.infraestrctura.synthevia.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

//este dto representa lo que el frontend puede enviar al crear un paquete
public record CrearReporteDto(

        Integer idVia,

        @Size(max = 1000)
        String descripcion,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitud,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitud,

        @DecimalMin("0.0")
        BigDecimal precisionReportadaMetros,

        @NotNull
        OffsetDateTime instanteInicioCaptura

) {
}