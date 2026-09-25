package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.SolicitudRegistro;
import com.infraestrctura.synthevia.entidad.Usuario;
import com.infraestrctura.synthevia.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ResultadoRegistro registrar(SolicitudRegistro solicitud) {

        String dni = solicitud
                .dni()
                .trim();

        String correo = solicitud
                .correoElectronico()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByDni(dni)) {
            return ResultadoRegistro.DNI_EXISTENTE;
        }

        if (usuarioRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            return ResultadoRegistro.CORREO_EXISTENTE;
        }

        Usuario usuario = new Usuario();
        usuario.setDni(dni);
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setApellidos(solicitud.apellidos().trim());
        usuario.setCorreoElectronico(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(solicitud.contrasena()));
        usuario.setRol("CIUDADANO");
        usuario.setReportesHoy((short) 0);
        usuario.setFechaUltimoReporte(null);
        usuario.setFechaCreacion(OffsetDateTime.now(ZoneOffset.UTC));
        usuarioRepository.save(usuario);

        return ResultadoRegistro.CREADO;
    }

    public enum ResultadoRegistro {
        CREADO,
        DNI_EXISTENTE,
        CORREO_EXISTENTE
    }

    public Optional<Usuario> autenticar(String dni, String contrasena) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni( dni.trim());

        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        String hashGuardado = usuario.getContrasenaHash();

        if (hashGuardado == null || hashGuardado.isBlank()) {
            return Optional.empty();
        }

        boolean contrasenaCorrecta = passwordEncoder.matches(contrasena, hashGuardado);

        if (!contrasenaCorrecta) {
            return Optional.empty();
        }

        return Optional.of(usuario);
    }

}