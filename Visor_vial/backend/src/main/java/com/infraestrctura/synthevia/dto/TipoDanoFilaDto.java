package com.infraestrctura.synthevia.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fila de la tabla de deteriorateos que ve el administrador. El backend
 * devuelve siempre los ocho conceptos, con el booleano ya resuelto, para que el
 * frontend no tenga que reconstruir el catalogo.
 */
public record TipoDanoFilaDto(
        String codigo,
        String etiqueta,
        boolean detectado,
        Integer cantidad,
        Integer confianza
) {
}
