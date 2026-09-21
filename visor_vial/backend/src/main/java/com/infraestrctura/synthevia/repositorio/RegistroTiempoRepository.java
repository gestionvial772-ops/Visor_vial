package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.RegistroTiempo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroTiempoRepository extends JpaRepository<RegistroTiempo, Integer> {
    List<RegistroTiempo> findByIdReporteOrderByTimestampAsc(Integer idReporte);
}