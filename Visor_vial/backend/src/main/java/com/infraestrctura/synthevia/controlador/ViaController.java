package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.infraestrctura.synthevia.dto.ViaResumenDto;
import java.util.List;

@RestController
@RequestMapping("/admin/vias")
public class ViaController {

    private final ViaSegmentoRepository viaRepository;

    public ViaController(ViaSegmentoRepository viaRepository) {
        this.viaRepository = viaRepository;
    }

    @GetMapping
    public ResponseEntity<List<ViaResumenDto>> listar() {
        return ResponseEntity.ok(viaRepository.listarResumen());
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> obtenerGeoJson() {
        String geoJson = viaRepository.obtenerViasComoGeoJson();
        return ResponseEntity.ok(geoJson);
    }

    @GetMapping("/{id}/geojson")
    public ResponseEntity<String> obtenerGeoJson(@PathVariable Integer id) {
        String geoJson = viaRepository.obtenerViaComoGeoJson(id);

        if (geoJson == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(geoJson);
    }
}
