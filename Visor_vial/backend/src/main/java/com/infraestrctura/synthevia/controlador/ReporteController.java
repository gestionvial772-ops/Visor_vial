package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.ActualizarEstadoDto;
import com.infraestrctura.synthevia.dto.EstadisticasDto;
import com.infraestrctura.synthevia.dto.PaginaDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.servicio.ReporteService;
import com.infraestrctura.synthevia.servicio.SupabaseStorageService;
import com.infraestrctura.synthevia.dto.AsignarViaDto;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);
    private final ReporteService reporteService;
    private final SupabaseStorageService supabaseStorageService;

    public ReporteController(
            ReporteService reporteService,
            SupabaseStorageService supabaseStorageService) {
        this.reporteService = reporteService;
        this.supabaseStorageService = supabaseStorageService;
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

    @GetMapping("/{id}/imagen")
    public ResponseEntity<Map<String, String>> imagen(@PathVariable Integer id) {

        Reporte reporte = reporteService.obtener(id).orElse(null);

        if (reporte == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "El reporte #" + id + " no existe."));
        }

        String url = supabaseStorageService.urlPublica(reporte.getImagenUrl());

        if (url == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El reporte no tiene imagen registrada."));
        }

        return ResponseEntity.ok(Map.of("url", url));
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

    @PatchMapping("/{id}/via")
    public ResponseEntity<Void> asignarVia( @PathVariable Integer id,
            @Valid @RequestBody AsignarViaDto dto) {

        boolean asignado = reporteService.asignarVia(id, dto.idVia());

        if (!asignado) {
            return ResponseEntity.notFound().build();
        }

        log.info("Reporte #{} vinculado a la vía #{}", id, dto.idVia());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasDto> estadisticas() {

        return ResponseEntity.ok(reporteService.obtenerEstadisticas());
    }

    @PostMapping
    public ResponseEntity<ReporteResumenDto> crear(@RequestBody Reporte body, java.security.Principal principal) {

        Reporte saved = reporteService.crear(body, Integer.valueOf(principal.getName()));

        log.info(
                "Reporte creado #{}",
                saved.getIdReporte());

        return ResponseEntity.status(201).body(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable Integer id) {

        boolean anulado = reporteService.eliminar(id);

        if (!anulado) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "mensaje", "El reporte #" + id
                                    + " no existe o ya estaba anulado."));
        }

        log.info(
                "Reporte #{} anulado logicamente",
                id);

        return ResponseEntity
                .ok()
                .body(Map.of("mensaje", "Reporte #" + id + " anulado."));
    }

    private ReporteResumenDto toDto(Reporte r) {
        return new ReporteResumenDto(
                r.getIdReporte(),
                r.getIdUsuario(),
                r.getIdVia(),
                r.getImagenUrl(),
                r.getDescripcion(),
                r.getEstado(),
                r.getFechaCreacion(), r.getNombreVia(), r.getCodigoSegmento());
    }
}
