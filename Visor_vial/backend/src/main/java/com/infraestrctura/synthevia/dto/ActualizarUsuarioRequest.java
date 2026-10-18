package com.infraestrctura.synthevia.dto;

/**
 * Cambio de datos editables de una cuenta. El rol NO se cambia por aqui: lo
 * maneja un endpoint aparte porque tiene sus propias validaciones.
 */
public record ActualizarUsuarioRequest(
        String nombre,
        String apellidos,
        String correoElectronico
) {}