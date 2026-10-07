package com.infraestrctura.synthevia.dto;

import java.math.BigDecimal;

/**
 * DTO que representa una vía cercana a un punto geográfico.
 * Incluye la información básica de la vía y su distancia al punto de referencia.
 */
public record ViaCercanaDto(
    Integer idVia,
    String codigoSegmento,
    String nombreVia,
    BigDecimal distanciaMetros
) {}
