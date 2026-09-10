package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.ViaSegmento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViaSegmentoRepository extends JpaRepository<ViaSegmento, Integer> {

    List<ViaSegmento> findAllByOrderByNombreViaAsc();
}
