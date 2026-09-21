package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.CrearReporteDto;
import com.infraestrctura.synthevia.dto.ReporteResumenDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.servicio.ReporteService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reportes")
public class ReporteCiudadanoController {

   private static final Logger log = LoggerFactory.getLogger(
         ReporteCiudadanoController.class);

   private final ReporteService reporteService;

   public ReporteCiudadanoController(ReporteService reporteService) {

      this.reporteService = reporteService;

   }

   @PostMapping
   public ResponseEntity<ReporteResumenDto> crear(
      @Valid 
      @RequestPart 
      CrearReporteDto dto,

      @RequestPart 
      MultipartFile imagen
   ) 
   {   
      Reporte reporte = reporteService.crearDesdeSistema(dto, imagen);

      log.info("Reporte ciudadano creado: #{}", reporte.getIdReporte());

      return ResponseEntity
            .status(201)
            .body(toDto(reporte));
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
            reporte.getFechaCreacion());
   }

}
