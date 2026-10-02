package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.entidad.Clasificacion;
import com.infraestrctura.synthevia.repositorio.ClasificacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClasificacionService {

    private final ClasificacionRepository clasificacionRepository;

    public ClasificacionService(ClasificacionRepository clasificacionRepository) {
        this.clasificacionRepository = clasificacionRepository;
    }

    @Transactional
    public Clasificacion guardarResultadoIa(Integer idReporte, Short severidad, String etiquetaDetectada,
            BigDecimal nivelConfianza,
            String modeloIaUtilizado,
            String versionModelo) {
        validarSeveridad(severidad);
        validarConfianza(nivelConfianza);
        validarModelo(modeloIaUtilizado);

        if (clasificacionRepository
                .findByIdReporte(idReporte)
                .isPresent()) {

            throw new IllegalStateException("El reporte ya posee una clasificación de IA.");
        }

        Clasificacion clasificacion = new Clasificacion();

        clasificacion.setIdReporte(idReporte);
        clasificacion.setSeveridad(severidad);
        clasificacion.setEtiquetaDetectada(etiquetaDetectada);
        clasificacion.setNivelConfianza(nivelConfianza);
        clasificacion.setModeloIaUtilizado(modeloIaUtilizado);
        clasificacion.setVersionModelo(versionModelo);
        clasificacion.setFechaProcesamiento(OffsetDateTime.now());

        return clasificacionRepository.save(
                clasificacion);
    }

    public Optional<Clasificacion> obtenerPorReporte(Integer idReporte) {
        return clasificacionRepository.findByIdReporte(idReporte);
    }

    private void validarSeveridad(Short severidad) {
        if (severidad == null || severidad < 1 || severidad > 3) {
            throw new IllegalArgumentException("La severidad debe ser 1, 2 o 3.");
        }
    }

    private void validarConfianza(BigDecimal confianza) {

        if (confianza == null) {
            return;
        }

        if (confianza.compareTo(BigDecimal.ZERO) < 0 ||
                confianza.compareTo(
                        BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("La confianza debe encontrarse entre 0 y 100.");
        }
    }

    private void validarModelo(String modelo) {
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException("Debe indicarse el modelo de IA utilizado.");
        }
    }
}