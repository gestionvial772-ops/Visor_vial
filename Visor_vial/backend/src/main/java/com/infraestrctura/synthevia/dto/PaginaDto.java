package com.infraestrctura.synthevia.dto;

import java.util.List;

public record PaginaDto<T>(
    List<T> contenido,
    int paginaActual,
    int totalPaginas,
    long totalElementos
) {}
