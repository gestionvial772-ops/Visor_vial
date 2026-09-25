package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.entidad.ViaSegmento;
import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/vias")
public class ViaController {

    private final ViaSegmentoRepository viaRepository;

    public ViaController(ViaSegmentoRepository viaRepository) {
        this.viaRepository = viaRepository;
    }

    @GetMapping
    public ResponseEntity<List<ViaSegmento>> listar() {
        return ResponseEntity.ok(viaRepository.findAllByOrderByNombreViaAsc());
    }
}
