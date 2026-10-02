package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Coordenada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface CoordenadaRepository extends JpaRepository<Coordenada, Integer>{
    Optional<Coordenada> findByIdReporte(Integer idReporte);
} 
