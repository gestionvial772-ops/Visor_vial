package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.ActualizarEstadoDto;
import com.infraestrctura.synthevia.dto.EstadisticasDto;
import com.infraestrctura.synthevia.dto.PaginaDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.servicio.ReporteService;
import jakarta.validation.Valid;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/admin/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public ResponseEntity<PaginaDto<ReporteResumenDto>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda) {

        Page<Reporte> page = reporteService.listar(
                pagina,
                tamanio,
                estado,
                busqueda);

        Page<ReporteResumenDto> dtoPage = page.map(this::toDto);

        log.info("Listar reportes: pagina={}, estado={}, busqueda={}, total={}", pagina, estado, busqueda,
                dtoPage.getTotalElements());
        return ResponseEntity.ok(new PaginaDto<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages(),
                dtoPage.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReporteResumenDto> obtener(@PathVariable Integer id) {
        return reporteService.obtener(id)
                .map(reporte -> ResponseEntity.ok(toDto(reporte)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReporteResumenDto> actualizarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarEstadoDto dto) {

        Optional<Reporte> actualizado = reporteService.actualizarEstado(id, dto.estado());
        if (actualizado.isEmpty())
            return ResponseEntity.notFound().build();

        log.info("Reporte #{}: actualizado a {}", id, dto.estado());
        return ResponseEntity.ok(toDto(actualizado.get()));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasDto> estadisticas() {

        return ResponseEntity.ok(reporteService.obtenerEstadisticas());
    }

    @PostMapping
    public ResponseEntity<ReporteResumenDto> crear(@RequestBody Reporte body) {

        Reporte saved = reporteService.crear(body);

        log.info(
                "Reporte creado #{}",
                saved.getIdReporte());

        return ResponseEntity.status(201).body(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id) {

        boolean eliminado = reporteService.eliminar(id);

        if (!eliminado) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        log.info(
                "Reporte #{} eliminado",
                id);

        return ResponseEntity
                .noContent()
                .build();
    }

    private ReporteResumenDto toDto(Reporte r) {
        return new ReporteResumenDto(
                r.getIdReporte(),
                r.getIdUsuario(),
                r.getIdVia(),
                r.getImagenUrl(),
                r.getDescripcion(),
                r.getEstado(),
                r.getFechaCreacion());
    }
}
