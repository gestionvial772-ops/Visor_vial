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
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/public")
public class AutenticacionController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionController.class);

    private final UsuarioService usuarioService;
    private final CaptchaService captchaService;
    private final SecurityContextRepository securityContextRepository;

    public AutenticacionController(UsuarioService usuarioService, CaptchaService captchaService,
            SecurityContextRepository securityContextRepository) {
        this.usuarioService = usuarioService;
        this.captchaService = captchaService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return token;
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud,
            HttpServletRequest request, HttpServletResponse response) {
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

        // Reemplazar la sesion anterior evita fijacion de sesion y rota el CSRF.
        var anterior = request.getSession(false);
        if (anterior != null) anterior.invalidate();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                u.getIdUsuario().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol()))));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

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
