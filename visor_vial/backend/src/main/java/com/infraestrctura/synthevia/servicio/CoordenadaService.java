package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.entidad.Coordenada;
import com.infraestrctura.synthevia.repositorio.CoordenadaRepository;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CoordenadaService {

    private static final int SRID_WGS84 = 4326;

    private final CoordenadaRepository coordenadaRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(
            new PrecisionModel(),
            SRID_WGS84);

    public CoordenadaService(CoordenadaRepository coordenadaRepository) {
        this.coordenadaRepository = coordenadaRepository;
    }

    @Transactional
    public Coordenada guardar(Integer idReporte, double latitud, double longitud, BigDecimal precisionReportadaMetros) 
    {
        validarCoordenadas(latitud, longitud);

        validarPrecision(precisionReportadaMetros);

        if (coordenadaRepository
                .findByIdReporte(idReporte)
                .isPresent()) 
        {
            throw new IllegalStateException("El reporte ya posee una coordenada registrada.");
        }

        Point punto = geometryFactory.createPoint(new Coordinate(longitud, latitud));

        punto.setSRID(SRID_WGS84);

        Coordenada coordenada = new Coordenada();

        coordenada.setIdReporte(idReporte);
        coordenada.setPuntoCapturado(punto);
        coordenada.setPrecisionReportadaMetros(
                precisionReportadaMetros);
        coordenada.setFechaCaptura(
                OffsetDateTime.now());

        return coordenadaRepository.save(
                coordenada);
    }

    public Optional<Coordenada> obtenerPorReporte(Integer idReporte) {
        return coordenadaRepository.findByIdReporte(idReporte);
    }

    private void validarCoordenadas(double latitud, double longitud) {
        if (latitud < -90 || latitud > 90) {
            throw new IllegalArgumentException("La latitud debe encontrarse entre -90 y 90.");
        }
        if (longitud < -180 || longitud > 180) 
        {
            throw new IllegalArgumentException("La longitud debe encontrarse entre -180 y 180.");
        }
    }
    private void validarPrecision(BigDecimal precision)
     {
        if (precision != null && precision.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new IllegalArgumentException("La precisión reportada no puede ser negativa.");
        }
    }
}