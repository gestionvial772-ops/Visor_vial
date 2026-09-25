package com.infraestrctura.synthevia.dto;

import java.time.OffsetDateTime;

public record ReporteResumenDto(
    Integer idReporte,
    Integer idUsuario,
    Integer idVia,
    String  imagenUrl,
    String  descripcion,
    String  estado,
    OffsetDateTime fechaCreacion
) {}
