package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.ViaSegmento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ViaSegmentoRepository extends JpaRepository<ViaSegmento, Integer> {

    List<ViaSegmento> findAllByOrderByNombreViaAsc();

    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', json_agg(
                json_build_object(
                    'type', 'Feature',
                    'geometry', ST_AsGeoJSON(geom)::json,
                    'properties', json_build_object(
                        'idVia', id_via,
                        'codigoSegmento', codigo_segmento,
                        'nombreVia', nombre_via
                    )
                )
            )
        )::text
        FROM via_segmento
        WHERE geom IS NOT NULL
    """, nativeQuery = true)
    String obtenerViasComoGeoJson();
}
