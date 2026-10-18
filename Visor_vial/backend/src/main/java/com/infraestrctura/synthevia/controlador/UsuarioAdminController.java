package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.ActualizarUsuarioRequest;
import com.infraestrctura.synthevia.dto.CambiarRolRequest;
import com.infraestrctura.synthevia.dto.EstadisticasUsuariosDto;
import com.infraestrctura.synthevia.dto.PaginaDto;
import com.infraestrctura.synthevia.dto.UsuarioResumenDto;
import com.infraestrctura.synthevia.servicio.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de cuentas del panel. Solo expone lectura y edicion de datos
 * visibles: la contrasena no se devuelve ni se escribe por aqui, el DNI es
 * inmutable y el borrado de cuentas no existe porque dejaria reportes huerfanos.
 */
@RestController
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAdminController.class);

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<PaginaDto<UsuarioResumenDto>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String busqueda) {

        Page<UsuarioResumenDto> result = usuarioService.listar(pagina, tamanio, rol, busqueda);

        log.info("Listar usuarios: pagina={}, rol={}, busqueda={}, total={}",
                pagina, rol, busqueda, result.getTotalElements());

        return ResponseEntity.ok(new PaginaDto<>(
                result.getContent(),
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements()));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasUsuariosDto> estadisticas() {
        return ResponseEntity.ok(usuarioService.obtenerEstadisticas());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResumenDto> actualizar(
            @PathVariable Integer id,
            @RequestBody ActualizarUsuarioRequest request) {

        UsuarioResumenDto actualizado = usuarioService.actualizar(id, request);

        log.info("Usuario actualizado. id={}", id);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResumenDto> cambiarRol(
            @PathVariable Integer id,
            @RequestBody CambiarRolRequest request) {

        UsuarioResumenDto actualizado = usuarioService.cambiarRol(id, request);

        log.info("Rol actualizado. id={}, rol={}", id, actualizado.rol());

        return ResponseEntity.ok(actualizado);
    }
}