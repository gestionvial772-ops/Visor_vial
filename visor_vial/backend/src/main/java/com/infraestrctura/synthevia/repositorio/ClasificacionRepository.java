package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Clasificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClasificacionRepository extends JpaRepository<Clasificacion, Integer> {
    Optional <Clasificacion> findByIdReporte(Integer idReporte);
} 