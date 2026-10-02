package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

}
