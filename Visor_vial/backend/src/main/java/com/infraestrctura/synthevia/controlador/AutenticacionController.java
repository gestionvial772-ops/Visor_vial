package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.RespuestaInicioSesion;
import com.infraestrctura.synthevia.dto.SolicitudInicioSesion;
import com.infraestrctura.synthevia.entidad.Usuario;
import com.infraestrctura.synthevia.repositorio.UsuarioRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@RestController
@RequestMapping("/public")
public class AutenticacionController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionController.class);

    private final UsuarioRepository usuarioRepository;

    public AutenticacionController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(
            @Valid @RequestBody SolicitudInicioSesion solicitud) {

        log.info("Intento de inicio de sesión para DNI: {}", solicitud.dni());

        String hash = hashearContrasena(solicitud.contrasena());
        Optional<Usuario> usuario = usuarioRepository
                .findByDniAndContrasenaHash(solicitud.dni(), hash);

        if (usuario.isEmpty()) {
            log.warn("Credenciales incorrectas para DNI: {}", solicitud.dni());
            return ResponseEntity.ok(new RespuestaInicioSesion(
                    false, "DNI o contraseña incorrectos", null, null));
        }

        Usuario u = usuario.get();
        log.info("Inicio de sesión exitoso - Usuario: {}, Rol: {}", u.getNombre(), u.getRol());
        return ResponseEntity.ok(new RespuestaInicioSesion(
                true, "Inicio de sesión exitoso", u.getNombre(), u.getRol()));
    }

    private String hashearContrasena(String contrasena) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(contrasena.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
