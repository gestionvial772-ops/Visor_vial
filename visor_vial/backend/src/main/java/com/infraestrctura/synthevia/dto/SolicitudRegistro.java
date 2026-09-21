package com.infraestrctura.synthevia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRegistro(

        @NotBlank
        @Size(max = 100)
        String nombre,

        @NotBlank
        @Size(max = 100)
        String apellidos,

        @NotBlank
        @Email
        @Size(max = 150)
        String correoElectronico,

        @NotBlank
        @Pattern(regexp = "^\\d{8}$")
        String dni,

        @NotBlank
        @Size(min = 8, max = 72)
        String contrasena,

        @NotBlank
        String captchaToken

) {
}