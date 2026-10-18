package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.CrearReporteDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.dto.ViaCercanaDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import com.infraestrctura.synthevia.servicio.ReporteService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;
import java.util.Map;
import com.infraestrctura.synthevia.dto.PaginaDto;
import com.infraestrctura.synthevia.servicio.SupabaseStorageService;

@RestController
@RequestMapping("/reportes")
public class ReporteCiudadanoController {

   private static final Logger log = LoggerFactory.getLogger(
         ReporteCiudadanoController.class);

   private final ReporteService reporteService;
   private final ViaSegmentoRepository viaRepository;
   private final SupabaseStorageService storage;

   public ReporteCiudadanoController(
         ReporteService reporteService,
         ViaSegmentoRepository viaRepository, SupabaseStorageService storage) {

      this.reporteService = reporteService;
      this.viaRepository = viaRepository;
      this.storage = storage;

   }

   @GetMapping("/mios")
   public PaginaDto<ReporteResumenDto> propios(Principal principal,
         @RequestParam(defaultValue = "0") int pagina,
         @RequestParam(defaultValue = "10") int tamanio) {
      var resultados = reporteService.listarPropios(Integer.valueOf(principal.getName()), pagina, tamanio)
            .map(this::toDto);
      return new PaginaDto<>(resultados.getContent(), resultados.getNumber(),
            resultados.getTotalPages(), resultados.getTotalElements());
   }

   @GetMapping("/{id}/imagen")
   public ResponseEntity<Map<String, String>> imagenPropia(@PathVariable Integer id, Principal principal) {
      var reporte = reporteService.obtener(id)
            .filter(r -> r.getIdUsuario().equals(Integer.valueOf(principal.getName())));
      if (reporte.isEmpty()) return ResponseEntity.notFound().build();
      String url = storage.urlPublica(reporte.get().getImagenUrl());
      return url == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(Map.of("url", url));
   }

   @PostMapping
   public ResponseEntity<ReporteResumenDto> crear(
      @Valid 
      @RequestPart 
      CrearReporteDto dto,

      @RequestPart 
      MultipartFile imagen, java.security.Principal principal
   ) 
   {   
      Reporte reporte = reporteService.crearDesdeSistema(dto, imagen, Integer.valueOf(principal.getName()));

      log.info("Reporte ciudadano creado: #{}", reporte.getIdReporte());

      // El reporte se registra SIN analisis de IA. El ciudadano solo reporta:
      // la llamada a Gemini tarda varios segundos y hacia que el usuario
      // abandona el formulario. La clasificacion la genera el administrador
      // desde el panel con el boton Reanalizar.
      return ResponseEntity
            .status(201)
            .body(toDto(reporte));
   }

   /**
    * Obtiene las vías más cercanas a una coordenada geográfica.
    * Útil para que el usuario seleccione manualmente la vía correcta al crear un reporte.
    * 
    * @param latitud Latitud del punto (WGS84, rango -90 a 90)
    * @param longitud Longitud del punto (WGS84, rango -180 a 180)
    * @param radio Radio de búsqueda en metros (por defecto 20m)
    * @param limite Número máximo de resultados (por defecto 5)
    * @return Lista de vías ordenadas por distancia (más cercana primero)
    */
   @GetMapping("/vias-cercanas")
   public ResponseEntity<List<ViaCercanaDto>> obtenerViasCercanas(
         @RequestParam Double latitud,
         @RequestParam Double longitud,
         @RequestParam(defaultValue = "20.0") Double radio,
         @RequestParam(defaultValue = "5") Integer limite) {

      log.info("Buscando vías cercanas a: lat={}, lon={}, radio={}m, limite={}", 
               latitud, longitud, radio, limite);

      List<Object[]> resultados = viaRepository.findViasCercanasConDistancia(
            longitud, latitud, radio, limite);

      List<ViaCercanaDto> viasCercanas = resultados.stream()
            .map(row -> new ViaCercanaDto(
                  (Integer) row[0],        // id_via
                  (String) row[1],          // codigo_segmento
                  (String) row[2],          // nombre_via
                  (BigDecimal) row[3]       // distancia_metros
            ))
            .collect(Collectors.toList());

      log.info("Se encontraron {} vías cercanas", viasCercanas.size());

      return ResponseEntity.ok(viasCercanas);
   }

   private ReporteResumenDto toDto(
         Reporte reporte) {

      return new ReporteResumenDto(
            reporte.getIdReporte(),
            reporte.getIdUsuario(),
            reporte.getIdVia(),
            reporte.getImagenUrl(),
            reporte.getDescripcion(),
            reporte.getEstado(),
            reporte.getFechaCreacion(), reporte.getNombreVia(), reporte.getCodigoSegmento());
   }

}
