package com.infraestrctura.synthevia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudInicioSesion(
    @NotBlank @Pattern(regexp = "^\\d{8}$") String dni,
    @NotBlank String contrasena
) {}
