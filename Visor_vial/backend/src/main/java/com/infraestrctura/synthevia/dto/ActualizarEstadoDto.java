package com.infraestrctura.synthevia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoDto(
    @NotBlank
    @Pattern(regexp = "PENDIENTE|EN_PROCESO|RESUELTO|DUPLICADO",
             message = "Estado inválido. Use: PENDIENTE, EN_PROCESO, RESUELTO o DUPLICADO")
    String estado
) {}
