package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    @Query(value = "SELECT r FROM Reporte r LEFT JOIN FETCH r.via v WHERE " +
           "r.eliminadoEn IS NULL AND " +
           "(:estado IS NULL OR r.estado = :estado) AND " +
           "(:busqueda IS NULL OR :busqueda = '' OR " +
           "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(r.idReporte AS string) LIKE CONCAT('%', :busqueda, '%') OR " +
           "LOWER(v.nombreVia) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(v.codigoSegmento) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "ORDER BY r.fechaCreacion DESC",
           countQuery = "SELECT COUNT(r) FROM Reporte r LEFT JOIN r.via v WHERE " +
           "r.eliminadoEn IS NULL AND " +
           "(:estado IS NULL OR r.estado = :estado) AND " +
           "(:busqueda IS NULL OR :busqueda = '' OR " +
           "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(r.idReporte AS string) LIKE CONCAT('%', :busqueda, '%') OR " +
           "LOWER(v.nombreVia) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(v.codigoSegmento) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<Reporte> buscar(
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    Page<Reporte> findByIdUsuarioAndEliminadoEnIsNullOrderByFechaCreacionDesc(
            Integer idUsuario, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NULL")
    long contarTotal();

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NULL AND r.estado = :estado")
    long contarActivosPorEstado(@Param("estado") String estado);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NOT NULL")
    long contarAnulados();
}
