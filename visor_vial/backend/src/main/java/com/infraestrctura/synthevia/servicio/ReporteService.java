package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.EstadisticasDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.repositorio.ReporteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.infraestrctura.synthevia.dto.CrearReporteDto;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service

@Transactional(readOnly = true)
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final CoordenadaService coordenadaService;
    private final RegistroTiempoService registroTiempoService;
    private final SupabaseStorageService supabaseStorageService;
    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    public ReporteService(
            ReporteRepository reporteRepository,
            CoordenadaService coordenadaService,
            RegistroTiempoService registroTiempoService,
            SupabaseStorageService supabaseStorageService) {
        this.reporteRepository = reporteRepository;
        this.coordenadaService = coordenadaService;
        this.registroTiempoService = registroTiempoService;
        this.supabaseStorageService = supabaseStorageService;
    }

    public Page<Reporte> listar(
            int pagina,
            int tamanio,
            String estado,
            String busqueda) {
        Pageable pageable = PageRequest.of(pagina, tamanio);

        return reporteRepository.buscar(
                estado,
                busqueda,
                pageable);
    }

    public Optional<Reporte> obtener(Integer id) {
        return reporteRepository.findById(id);
    }

    @Transactional
    public Optional<Reporte> actualizarEstado(
            Integer id,
            String nuevoEstado) {
        Optional<Reporte> reporteOpt = reporteRepository.findById(id);
        if (reporteOpt.isEmpty()) {
            return Optional.empty();
        }

        Reporte reporte = reporteOpt.get();

        reporte.setEstado(nuevoEstado);

        Reporte actualizado = reporteRepository.save(reporte);

        return Optional.of(actualizado);
    }

    @Transactional
    public Reporte crear(Reporte body) {

        Reporte reporte = new Reporte();

        reporte.setIdUsuario(
                body.getIdUsuario() != null
                        ? body.getIdUsuario()
                        : 1);

        reporte.setIdVia(body.getIdVia());

        reporte.setImagenUrl(
                body.getImagenUrl() != null
                        ? body.getImagenUrl()
                        : "");

        reporte.setDescripcion(
                body.getDescripcion());

        reporte.setEstado("PENDIENTE");

        return reporteRepository.save(reporte);
    }

    @Transactional
    public boolean eliminar(Integer id) {

        if (!reporteRepository.existsById(id)) {
            return false;
        }

        reporteRepository.deleteById(id);

        return true;
    }

    @Transactional
    public Reporte crearDesdeSistema(CrearReporteDto dto, MultipartFile imagen) {

        OffsetDateTime instanteServidor = OffsetDateTime.now();

        String imagenPath = supabaseStorageService.generarRuta(imagen);

        boolean imagenSubida = false;

        try {

            Reporte reporte = new Reporte();

            reporte.setIdUsuario(1);

            reporte.setIdVia(dto.idVia());

            reporte.setImagenUrl(imagenPath);

            reporte.setDescripcion(dto.descripcion());

            reporte.setEstado("PENDIENTE");

            Reporte guardado = reporteRepository.save(reporte);
            log.info(
                    "PASO 1 - Reporte guardado. idReporte={}",
                    guardado.getIdReporte());

            registroTiempoService.registrarInicioCaptura(guardado.getIdReporte(), dto.instanteInicioCaptura());
            log.info(
                    "PASO 2 - INICIO_CAPTURA registrado. idReporte={}",
                    guardado.getIdReporte());

            registroTiempoService.registrarEnvioServidor(guardado.getIdReporte(), instanteServidor);
            log.info(
                    "PASO 3 - ENVIO_SERVIDOR registrado. idReporte={}",
                    guardado.getIdReporte());

            log.info(
                    "PASO 4 - Antes de guardar coordenada. idReporte={}",
                    guardado.getIdReporte());
            coordenadaService.guardar(
                    guardado.getIdReporte(),
                    dto.latitud(),
                    dto.longitud(),
                    dto.precisionReportadaMetros());

            log.info(
                    "PASO 5 - Coordenada guardada. idReporte={}",
                    guardado.getIdReporte());

            registroTiempoService.registrarGeoprocesamiento(guardado.getIdReporte());

            supabaseStorageService.subir(imagenPath, imagen);

            imagenSubida = true;

            return guardado;

        } catch (RuntimeException e) {
            if (imagenSubida) {
                supabaseStorageService.eliminarSilenciosamente(imagenPath);
            }
            throw e;
        }
    }

    public EstadisticasDto obtenerEstadisticas() {

        long total = reporteRepository.contarTotal();

        long pendientes = reporteRepository.countByEstado(
                "PENDIENTE");

        long enProceso = reporteRepository.countByEstado(
                "EN_PROCESO");

        long resueltos = reporteRepository.countByEstado(
                "RESUELTO");

        long duplicados = reporteRepository.countByEstado(
                "DUPLICADO");

        return new EstadisticasDto(
                total,
                pendientes,
                enProceso,
                resueltos,
                duplicados);
    }

}
