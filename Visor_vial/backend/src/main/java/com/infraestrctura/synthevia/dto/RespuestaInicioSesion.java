package com.infraestrctura.synthevia.dto;

public record RespuestaInicioSesion(
    boolean exito,
    String mensaje,
    String nombre,
    String rol
) {}
