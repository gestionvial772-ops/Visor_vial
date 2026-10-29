package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.ViaSegmento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.infraestrctura.synthevia.dto.ViaResumenDto;
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

    /**
     * Busca las vías más cercanas a un punto geográfico dado.
     * Devuelve un array de objetos con: id_via, codigo_segmento, nombre_via,
     * distancia_metros
     *
     * @param longitud    Longitud del punto de referencia (WGS84)
     * @param latitud     Latitud del punto de referencia (WGS84)
     * @param radioMetros Radio de búsqueda en metros
     * @param limite      Número máximo de resultados
     * @return Lista de objetos con información de vía y distancia, ordenadas por
     *         distancia
     */
    @Query(value = """
                SELECT
                    v.id_via,
                    v.codigo_segmento,
                    v.nombre_via,
                    ROUND(
                        ST_Distance(
                            v.geom::geography,
                            ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography
                        )::numeric,
                        2
                    ) as distancia_metros
                FROM via_segmento v
                WHERE v.geom IS NOT NULL
                  AND ST_DWithin(
                        v.geom::geography,
                        ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography,
                        :radioMetros
                      )
                ORDER BY distancia_metros
                LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> findViasCercanasConDistancia(
            @Param("longitud") Double longitud,
            @Param("latitud") Double latitud,
            @Param("radioMetros") Double radioMetros,
            @Param("limite") Integer limite);

    @Query("""
                SELECT new com.infraestrctura.synthevia.dto.ViaResumenDto(
                    v.idVia,
                    v.codigoSegmento,
                    v.nombreVia
                )
                FROM ViaSegmento v
                ORDER BY v.nombreVia ASC, v.codigoSegmento ASC
            """)
    List<ViaResumenDto> listarResumen();

    @Query(value = """
    SELECT jsonb_build_object(
        'type', 'Feature',
        'geometry', ST_AsGeoJSON(v.geom)::jsonb,
        'properties', jsonb_build_object(
            'idVia', v.id_via,
            'codigoSegmento', v.codigo_segmento,
            'nombreVia', v.nombre_via
        )
    )::text
    FROM via_segmento v
    WHERE v.id_via = :idVia
    """, nativeQuery = true)
String obtenerViaComoGeoJson(@Param("idVia") Integer idVia);
}
