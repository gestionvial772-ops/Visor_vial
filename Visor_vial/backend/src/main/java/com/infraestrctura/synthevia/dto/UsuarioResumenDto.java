package com.infraestrctura.synthevia.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Fila del listado de usuarios del panel. No expone el hash de contrasena:
 * ese campo nunca debe salir del backend.
 */
public record UsuarioResumenDto(
        Integer idUsuario,
        String dni,
        String nombre,
        String apellidos,
        String correoElectronico,
        String rol,
        short reportesHoy,
        LocalDate fechaUltimoReporte,
        OffsetDateTime fechaCreacion,
        long totalReportes
) {}