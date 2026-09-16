package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.EstadisticasDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.repositorio.ReporteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service

@Transactional(readOnly = true)
public class ReporteService {

    private final ReporteRepository reporteRepository;

    public ReporteService(ReporteRepository reporteRepository){
        this.reporteRepository = reporteRepository;
    }

    public Page<Reporte> listar(
        int pagina,
        int tamanio,
        String estado,
        String busqueda
    ){
        Pageable pageable = PageRequest.of(pagina, tamanio);

        return reporteRepository.buscar(
            estado,
            busqueda,
            pageable
        );
    }

    public Optional <Reporte> obtener(Integer id){
        return reporteRepository.findById(id);
    }

    @Transactional
    public Optional<Reporte> actualizarEstado(
        Integer id,
        String nuevoEstado
    ){
        Optional<Reporte> reporteOpt = reporteRepository.findById(id);
        if(reporteOpt.isEmpty()){
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
                        : 1
        );

        reporte.setIdVia(body.getIdVia());

        reporte.setImagenUrl(
                body.getImagenUrl() != null
                        ? body.getImagenUrl()
                        : ""
        );

        reporte.setDescripcion(
                body.getDescripcion()
        );

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

    public EstadisticasDto obtenerEstadisticas() {

        long total =
                reporteRepository.contarTotal();

        long pendientes =
                reporteRepository.countByEstado(
                        "PENDIENTE"
                );

        long enProceso =
                reporteRepository.countByEstado(
                        "EN_PROCESO"
                );

        long resueltos =
                reporteRepository.countByEstado(
                        "RESUELTO"
                );

        long duplicados =
                reporteRepository.countByEstado(
                        "DUPLICADO"
                );

        return new EstadisticasDto(
                total,
                pendientes,
                enProceso,
                resueltos,
                duplicados
        );
    }
    
}
