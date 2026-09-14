package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.ActualizarEstadoDto;
import com.infraestrctura.synthevia.dto.EstadisticasDto;
import com.infraestrctura.synthevia.dto.PaginaDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.repositorio.ReporteRepository;
import jakarta.validation.Valid;
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
    private final ReporteRepository reporteRepository;

    public ReporteController(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @GetMapping
    public ResponseEntity<PaginaDto<ReporteResumenDto>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda) {

        Pageable pageable = PageRequest.of(pagina, tamanio);
        Page<Reporte> page = reporteRepository.buscar(estado, busqueda, pageable);

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
        return reporteRepository.findById(id)
                .map(r -> ResponseEntity.ok(toDto(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReporteResumenDto> actualizarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarEstadoDto dto) {

        Optional<Reporte> opt = reporteRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();

        Reporte r = opt.get();
        String anterior = r.getEstado();
        r.setEstado(dto.estado());
        reporteRepository.save(r);

        log.info("Reporte #{}: {} → {}", id, anterior, dto.estado());
        return ResponseEntity.ok(toDto(r));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasDto> estadisticas() {
        long total = reporteRepository.contarTotal();
        long pendientes = reporteRepository.countByEstado("PENDIENTE");
        long enProceso = reporteRepository.countByEstado("EN_PROCESO");
        long resueltos = reporteRepository.countByEstado("RESUELTO");
        long duplicados = reporteRepository.countByEstado("DUPLICADO");

        log.info("Estadísticas: total={}, pendientes={}, enProceso={}, resueltos={}", total, pendientes, enProceso,
                resueltos);
        return ResponseEntity.ok(new EstadisticasDto(total, pendientes, enProceso, resueltos, duplicados));
    }

    @PostMapping
    public ResponseEntity<ReporteResumenDto> crear(@RequestBody Reporte body) {
        Reporte r = new Reporte();
        r.setIdUsuario(body.getIdUsuario() != null ? body.getIdUsuario() : 1);
        r.setIdVia(body.getIdVia());
        r.setImagenUrl(body.getImagenUrl() != null ? body.getImagenUrl() : "");
        r.setDescripcion(body.getDescripcion());
        r.setEstado("PENDIENTE");
        Reporte saved = reporteRepository.save(r);
        log.info("Reporte creado: #{}", saved.getIdReporte());
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!reporteRepository.existsById(id))
            return ResponseEntity.notFound().build();
        reporteRepository.deleteById(id);
        log.info("Reporte #{} eliminado", id);
        return ResponseEntity.noContent().build();
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
