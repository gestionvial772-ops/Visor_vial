package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.entidad.RegistroTiempo;
import com.infraestrctura.synthevia.repositorio.RegistroTiempoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistroTiempoService {

    private final RegistroTiempoRepository registroTiempoRepository;

    public RegistroTiempoService(
            RegistroTiempoRepository registroTiempoRepository) {

        this.registroTiempoRepository =
                registroTiempoRepository;
    }

    @Transactional
    public RegistroTiempo registrarInicioCaptura(
            Integer idReporte,
            OffsetDateTime instanteInicio) {

        return registrar(
                idReporte,
                "INICIO_CAPTURA",
                "CLIENTE",
                instanteInicio
        );
    }

    @Transactional
    public RegistroTiempo registrarEnvioServidor(
            Integer idReporte,
            OffsetDateTime instanteServidor) {

        return registrar(
                idReporte,
                "ENVIO_SERVIDOR",
                "SERVIDOR",
                instanteServidor
        );
    }

    /**
     * El analisis con IA ya no se ejecuta al registrar el reporte, solo cuando
     * el administrador pulsa Reanalizar. Por eso la etapa PROCESAMIENTO_IA se
     * registra desde el endpoint de reanalisis, no desde ReporteService.
     */
    @Transactional
    public RegistroTiempo registrarProcesamientoIa(
            Integer idReporte) {

        return registrar(
                idReporte,
                "PROCESAMIENTO_IA",
                "SERVIDOR",
                OffsetDateTime.now()
        );
    }

    @Transactional
    public RegistroTiempo registrarGeoprocesamiento(
            Integer idReporte) {

        return registrar(
                idReporte,
                "GEOPROCESAMIENTO",
                "SERVIDOR",
                OffsetDateTime.now()
        );
    }

    @Transactional
    public RegistroTiempo registrarConsolidacion(
            Integer idReporte) {

        return registrar(
                idReporte,
                "CONSOLIDACION",
                "SERVIDOR",
                OffsetDateTime.now()
        );
    }

    public List<RegistroTiempo> listarPorReporte(
            Integer idReporte) {

        return registroTiempoRepository
                .findByIdReporteOrderByTimestampAsc(
                        idReporte
                );
    }

    private RegistroTiempo registrar(
            Integer idReporte,
            String etapa,
            String origen,
            OffsetDateTime instante) {

        RegistroTiempo registro =
                new RegistroTiempo();

        registro.setIdReporte(idReporte);
        registro.setEtapa(etapa);
        registro.setOrigenEvento(origen);
        registro.setTimestamp(instante);

        return registroTiempoRepository.save(
                registro
        );
    }
}