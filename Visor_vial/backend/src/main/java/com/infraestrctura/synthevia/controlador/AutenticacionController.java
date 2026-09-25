package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.dto.RespuestaInicioSesion;
import com.infraestrctura.synthevia.dto.RespuestaRegistro;
import com.infraestrctura.synthevia.dto.SolicitudInicioSesion;
import com.infraestrctura.synthevia.dto.SolicitudRegistro;
import com.infraestrctura.synthevia.entidad.Usuario;
import com.infraestrctura.synthevia.servicio.CaptchaService;
import com.infraestrctura.synthevia.servicio.UsuarioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/public")
public class AutenticacionController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionController.class);

    private final UsuarioService usuarioService;
    private final CaptchaService captchaService;

    public AutenticacionController(UsuarioService usuarioService, CaptchaService captchaService) {
        this.usuarioService = usuarioService;
        this.captchaService = captchaService;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        log.info("Intento de inicio de sesión para DNI: {}", solicitud.dni());

        if (!captchaService.validar(solicitud.captchaToken())) {
            log.warn("CAPTCHA inválido para DNI: {}", solicitud.dni());
            return ResponseEntity.badRequest().body(
                    new RespuestaInicioSesion(
                            false,
                            "No se pudo validar el CAPTCHA.",
                            null,
                            null));
        }

        Optional<Usuario> usuario = usuarioService.autenticar(
                solicitud.dni(),
                solicitud.contrasena());

        if (usuario.isEmpty()) {
            log.warn("Credenciales incorrectas para DNI: {}", solicitud.dni());
            return ResponseEntity.ok(
                    new RespuestaInicioSesion(
                            false,
                            "DNI o contraseña incorrectos",
                            null,
                            null));
        }

        Usuario u = usuario.get();

        log.info("Inicio de sesión exitoso - Usuario: {}, Rol: {}", u.getNombre(), u.getRol());

        return ResponseEntity.ok(
                new RespuestaInicioSesion(
                        true,
                        "Inicio de sesión exitoso",
                        u.getNombre(),
                        u.getRol()));
    }

    @PostMapping("/registrarse")
    public ResponseEntity<RespuestaRegistro> registrarse(@Valid @RequestBody SolicitudRegistro solicitud) {
        log.info("Solicitud de registro para DNI: {}", solicitud.dni());

        if (!captchaService.validar(solicitud.captchaToken())) {
            log.warn("CAPTCHA inválido durante registro para DNI: {}", solicitud.dni());
            return ResponseEntity.badRequest().body(
                    new RespuestaRegistro(
                            false,
                            "No se pudo validar el CAPTCHA."));
        }

        UsuarioService.ResultadoRegistro resultado = usuarioService.registrar(solicitud);

        return switch (resultado) {
            case DNI_EXISTENTE -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RespuestaRegistro(false, "El DNI ya se encuentra registrado."));

            case CORREO_EXISTENTE -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RespuestaRegistro(false, "El correo electrónico ya se encuentra registrado."));

            case CREADO -> {
                log.info("Ciudadano registrado correctamente: DNI {}", solicitud.dni());
                yield ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(new RespuestaRegistro(true, "Cuenta creada correctamente."));
            }
        };
    }
}