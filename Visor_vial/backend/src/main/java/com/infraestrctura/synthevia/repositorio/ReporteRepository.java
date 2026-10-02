package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    Page<Reporte> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    Page<Reporte> findByEstadoOrderByFechaCreacionDesc(String estado, Pageable pageable);

    @Query("SELECT r FROM Reporte r WHERE " +
           "r.eliminadoEn IS NULL AND " +
           "(:estado IS NULL OR r.estado = :estado) AND " +
           "(:busqueda IS NULL OR :busqueda = '' OR " +
           "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(r.idVia AS string) LIKE CONCAT('%', :busqueda, '%')) " +
           "ORDER BY r.fechaCreacion DESC")
    Page<Reporte> buscar(
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    List<Reporte> findByIdUsuario(Integer idUsuario);

    long countByEstado(String estado);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NULL")
    long contarTotal();

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NULL AND r.estado = :estado")
    long contarActivosPorEstado(@Param("estado") String estado);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.eliminadoEn IS NOT NULL")
    long contarAnulados();
}
