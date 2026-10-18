package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

    /**
     * Busqueda del panel de administracion. El total de reportes viene del
     * conteo sobre reporte, porque usuario no lo guarda y asi el listado no
     * depende de una tabla desnormalizada que pueda quedar desfasada.
     */
    @Query(value = "SELECT u FROM Usuario u WHERE " +
           "(:rol IS NULL OR u.rol = :rol) AND " +
           "(:busqueda IS NULL OR :busqueda = '' OR " +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.correoElectronico) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "u.dni LIKE CONCAT('%', :busqueda, '%')) " +
           "ORDER BY u.fechaCreacion DESC",
           countQuery = "SELECT COUNT(u) FROM Usuario u WHERE " +
           "(:rol IS NULL OR u.rol = :rol) AND " +
           "(:busqueda IS NULL OR :busqueda = '' OR " +
           "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.correoElectronico) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "u.dni LIKE CONCAT('%', :busqueda, '%'))")
    org.springframework.data.domain.Page<Usuario> buscar(
            @Param("rol") String rol,
            @Param("busqueda") String busqueda,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(u) FROM Usuario u")
    long contarTotal();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol = :rol")
    long contarPorRol(@Param("rol") String rol);

    /**
     * Cuentas que registraron al menos un reporte vigente hoy. No se usa la
     * columna reportes_hoy porque nunca se incrementa: solo se pone en cero al
     * crear la cuenta, asi que siempre valdria cero.
     */
    @Query("SELECT COUNT(DISTINCT r.idUsuario) FROM Reporte r " +
           "WHERE r.eliminadoEn IS NULL AND CAST(r.fechaCreacion AS java.time.LocalDate) = CURRENT_DATE")
    long contarActivosHoy();
}