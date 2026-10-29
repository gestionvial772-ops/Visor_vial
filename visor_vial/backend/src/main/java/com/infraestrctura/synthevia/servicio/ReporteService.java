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
import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service

@Transactional(readOnly = true)
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final CoordenadaService coordenadaService;
    private final RegistroTiempoService registroTiempoService;
    private final SupabaseStorageService supabaseStorageService;
    private final ViaSegmentoRepository viaRepository;
    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    public ReporteService(
            ReporteRepository reporteRepository,
            CoordenadaService coordenadaService,
            RegistroTiempoService registroTiempoService,
            SupabaseStorageService supabaseStorageService, ViaSegmentoRepository viaRepository) {
        this.reporteRepository = reporteRepository;
        this.coordenadaService = coordenadaService;
        this.registroTiempoService = registroTiempoService;
        this.supabaseStorageService = supabaseStorageService;
        this.viaRepository = viaRepository;
    }

    public Page<Reporte> listar(
            int pagina,
            int tamanio,
            String estado,
            String busqueda) {
        Pageable pageable = paginacion(pagina, tamanio);

        return reporteRepository.buscar(
                estado == null || estado.isBlank() ? null : estado.trim(),
                busqueda == null ? null : busqueda.trim(),
                pageable);
    }

    public long contarAnulados() {
        return reporteRepository.contarAnulados();
    }

    public Optional<Reporte> obtener(Integer id) {
        return reporteRepository.findById(id).filter(r -> !r.estaAnulado());
    }

    @Transactional
    public Optional<Reporte> actualizarEstado(
            Integer id,
            String nuevoEstado) {
        Optional<Reporte> reporteOpt = obtener(id);
        if (reporteOpt.isEmpty()) {
            return Optional.empty();
        }

        Reporte reporte = reporteOpt.get();

        reporte.setEstado(nuevoEstado);

        Reporte actualizado = reporteRepository.save(reporte);

        return Optional.of(actualizado);
    }

    @Transactional
    public boolean asignarVia(Integer idReporte, Integer idVia) {

        validarVia(idVia);

        Optional<Reporte> reporteOpt = obtener(idReporte);

        if (reporteOpt.isEmpty()) {
            return false;
        }

        Reporte reporte = reporteOpt.get();

        reporte.setIdVia(idVia);

        reporteRepository.save(reporte);

        log.info("Reporte #{} vinculado a la vía #{}", idReporte, idVia);

        return true;
    }

    @Transactional
    public Reporte crear(Reporte body, Integer idUsuario) {
        validarVia(body.getIdVia());

        Reporte reporte = new Reporte();

        reporte.setIdUsuario(idUsuario);

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

        Optional<Reporte> reporteOpt = reporteRepository.findById(id);

        if (reporteOpt.isEmpty()) {
            return false;
        }

        Reporte reporte = reporteOpt.get();

        if (reporte.estaAnulado()) {
            return false;
        }

        reporte.setEliminadoEn(OffsetDateTime.now());

        reporteRepository.save(reporte);

        log.info("Reporte #{} anulado logicamente", id);

        return true;
    }

    @Transactional
    public Reporte crearDesdeSistema(CrearReporteDto dto, MultipartFile imagen, Integer idUsuario) {
        validarVia(dto.idVia());

        OffsetDateTime instanteServidor = OffsetDateTime.now();

        String imagenPath = supabaseStorageService.generarRuta(imagen);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK)
                    supabaseStorageService.eliminarSilenciosamente(imagenPath);
            }
        });

        Reporte reporte = new Reporte();

        reporte.setIdUsuario(idUsuario);

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

        registroTiempoService.registrarConsolidacion(guardado.getIdReporte());

        return guardado;

    }

    public Page<Reporte> listarPropios(Integer idUsuario, int pagina, int tamanio) {
        return reporteRepository.findByIdUsuarioAndEliminadoEnIsNullOrderByFechaCreacionDesc(
                idUsuario, paginacion(pagina, tamanio));
    }

    private Pageable paginacion(int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginacion invalida (tamanio: 1 a 100).");
        }
        return PageRequest.of(pagina, tamanio);
    }

    private void validarVia(Integer idVia) {
        if (idVia != null && !viaRepository.existsById(idVia)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La via seleccionada no existe.");
        }
    }

    public EstadisticasDto obtenerEstadisticas() {

        long total = reporteRepository.contarTotal();

        long pendientes = reporteRepository.contarActivosPorEstado(
                "PENDIENTE");

        long enProceso = reporteRepository.contarActivosPorEstado(
                "EN_PROCESO");

        long resueltos = reporteRepository.contarActivosPorEstado(
                "RESUELTO");

        long duplicados = reporteRepository.contarActivosPorEstado(
                "DUPLICADO");

        return new EstadisticasDto(
                total,
                pendientes,
                enProceso,
                resueltos,
                duplicados);
    }
}
