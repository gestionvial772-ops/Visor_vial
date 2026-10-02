package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.AnalisisIaDto;
import com.infraestrctura.synthevia.dto.CoordenadaDto;
import com.infraestrctura.synthevia.dto.ReporteDetalleDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.entidad.Coordenada;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.servicio.AnalisisDanoService;
import com.infraestrctura.synthevia.servicio.CoordenadaService;
import com.infraestrctura.synthevia.servicio.ReporteService;
import com.infraestrctura.synthevia.servicio.RegistroTiempoService;
import com.infraestrctura.synthevia.servicio.SupabaseStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints del analisis de danos con IA. Toda esta superficie es
 * exclusivamente del panel del administrador: el ciudadano nunca ve el
 * resultado del modelo.
 */
@RestController
@RequestMapping("/admin/reportes")
public class AnalisisController {

    private static final Logger log = LoggerFactory.getLogger(AnalisisController.class);

    private final ReporteService reporteService;
    private final CoordenadaService coordenadaService;
    private final AnalisisDanoService analisisDanoService;
    private final SupabaseStorageService supabaseStorageService;
    private final RegistroTiempoService registroTiempoService;

    public AnalisisController(
            ReporteService reporteService,
            CoordenadaService coordenadaService,
            AnalisisDanoService analisisDanoService,
            SupabaseStorageService supabaseStorageService,
            RegistroTiempoService registroTiempoService) {

        this.reporteService = reporteService;
        this.coordenadaService = coordenadaService;
        this.analisisDanoService = analisisDanoService;
        this.supabaseStorageService = supabaseStorageService;
        this.registroTiempoService = registroTiempoService;
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<ReporteDetalleDto> detalle(@PathVariable Integer id) {

        return reporteService.obtener(id)
                .map(reporte -> ResponseEntity.ok(
                        new ReporteDetalleDto(
                                toResumenDto(reporte),
                                coordenadaService.obtenerPorReporte(id)
                                        .map(this::toCoordenadaDto)
                                        .orElse(null),
                                analisisDanoService.obtenerAnalisis(id).orElse(null))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/analisis")
    public ResponseEntity<AnalisisIaDto> analisis(@PathVariable Integer id) {

        return analisisDanoService.obtenerAnalisis(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Vuelve a ejecutar el analisis sobre la imagen ya almacenada, reemplazando
     * la clasificacion anterior.
     */
    @PostMapping("/{id}/reanalizar")
    public ResponseEntity<Map<String, String>> reanalizar(@PathVariable Integer id) {

        Reporte reporte = reporteService.obtener(id).orElse(null);

        if (reporte == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "El reporte #" + id + " no existe."));
        }

        String imagenUrl = reporte.getImagenUrl();

        if (imagenUrl == null || imagenUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El reporte no tiene imagen registrada."));
        }

        log.info("Reanalisis de IA solicitado. idReporte={}", id);

        try {
            SupabaseStorageService.ImagenDescargada imagen =
                    supabaseStorageService.descargar(imagenUrl);

            registroTiempoService.registrarProcesamientoIa(id);

            analisisDanoService.analizarYGuardar(
                    id,
                    imagen.contenido(),
                    imagen.mimeType(),
                    reporte.getDescripcion());

            registroTiempoService.registrarConsolidacion(id);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Análisis de IA actualizado correctamente."));

        } catch (IllegalArgumentException e) {
            log.warn("Reanalisis rechazado. idReporte={}. Causa: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", e.getMessage()));

        } catch (Exception e) {
            log.error("Fallo el reanálisis con IA. idReporte={}", id, e);

            String causa = e.getMessage() == null
                    ? "error desconocido"
                    : e.getMessage();

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "mensaje", "No se pudo completar el análisis con IA.",
                            "detalle", causa));
        }
    }

    private ReporteResumenDto toResumenDto(Reporte r) {
        return new ReporteResumenDto(
                r.getIdReporte(),
                r.getIdUsuario(),
                r.getIdVia(),
                r.getImagenUrl(),
                r.getDescripcion(),
                r.getEstado(),
                r.getFechaCreacion());
    }

    private CoordenadaDto toCoordenadaDto(Coordenada c) {

        Double latitud = c.getPuntoCapturado() == null ? null : c.getPuntoCapturado().getY();
        Double longitud = c.getPuntoCapturado() == null ? null : c.getPuntoCapturado().getX();

        return new CoordenadaDto(
                latitud,
                longitud,
                c.getPrecisionReportadaMetros(),
                c.getFechaCaptura());
    }
}
