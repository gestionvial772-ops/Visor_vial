package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.ActualizarUsuarioRequest;
import com.infraestrctura.synthevia.dto.CambiarRolRequest;
import com.infraestrctura.synthevia.dto.EstadisticasUsuariosDto;
import com.infraestrctura.synthevia.dto.SolicitudRegistro;
import com.infraestrctura.synthevia.dto.UsuarioResumenDto;
import com.infraestrctura.synthevia.entidad.Usuario;
import com.infraestrctura.synthevia.repositorio.ReporteRepository;
import com.infraestrctura.synthevia.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_CIUDADANO = "CIUDADANO";

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final ReporteRepository reporteRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            ReporteRepository reporteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.reporteRepository = reporteRepository;
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
        usuario.setRol(ROL_CIUDADANO);
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

        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(dni.trim());

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

    // ── Panel de administracion ────────────────────────────────────────────────

    public Page<UsuarioResumenDto> listar(int pagina, int tamanio, String rol, String busqueda) {
        Pageable pageable = paginacion(pagina, tamanio);

        Page<Usuario> paginaUsuarios = usuarioRepository.buscar(
                rol == null || rol.isBlank() ? null : rol.trim(),
                busqueda == null ? null : busqueda.trim(),
                pageable);

        return paginaUsuarios.map(this::aResumen);
    }

    public EstadisticasUsuariosDto obtenerEstadisticas() {
        long total = usuarioRepository.contarTotal();

        return new EstadisticasUsuariosDto(
                total,
                usuarioRepository.contarPorRol(ROL_CIUDADANO),
                usuarioRepository.contarPorRol(ROL_ADMIN),
                usuarioRepository.contarActivosHoy());
    }

    @Transactional
    public UsuarioResumenDto actualizar(Integer id, ActualizarUsuarioRequest request) {
        Usuario usuario = obtener(id);

        String nombre = validarTexto(request.nombre(), "nombre");
        String apellidos = validarTexto(request.apellidos(), "apellidos");
        String correo = request.correoElectronico() == null
                ? null
                : request.correoElectronico().trim().toLowerCase(Locale.ROOT);

        if (correo != null) {
            if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El correo electrónico no tiene un formato válido.");
            }

            // El correo es unico: se rechaza si ya lo usa otra cuenta.
            usuarioRepository.findAll().stream()
                    .filter(u -> correo.equals(u.getCorreoElectronico().toLowerCase(Locale.ROOT)))
                    .filter(u -> !u.getIdUsuario().equals(id))
                    .findFirst()
                    .ifPresent(u -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ese correo ya está registrado en otra cuenta.");
                    });
        }

        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);

        if (correo != null) {
            usuario.setCorreoElectronico(correo);
        }

        return aResumen(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResumenDto cambiarRol(Integer id, CambiarRolRequest request) {
        Usuario usuario = obtener(id);

        String rol = request.rol() == null
                ? ""
                : request.rol().trim().toUpperCase(Locale.ROOT);

        if (!ROL_ADMIN.equals(rol) && !ROL_CIUDADANO.equals(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El rol debe ser ADMIN o CIUDADANO.");
        }

        usuario.setRol(rol);

        return aResumen(usuarioRepository.save(usuario));
    }

    private Usuario obtener(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El usuario #" + id + " no existe."));
    }

    private String validarTexto(String valor, String campo) {
        if (valor == null || !StringUtils.hasText(valor)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El campo " + campo + " no puede quedar vacío.");
        }

        String limpio = valor.trim();

        if (limpio.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El campo " + campo + " no puede superar los 100 caracteres.");
        }

        return limpio;
    }

    private Pageable paginacion(int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Paginación inválida (tamaño: 1 a 100).");
        }

        return PageRequest.of(pagina, tamanio);
    }

    private UsuarioResumenDto aResumen(Usuario u) {
        List<Long> ids = List.of(u.getIdUsuario().longValue());

        long totalReportes = reporteRepository.contarReportesDeUsuarios(ids);

        return new UsuarioResumenDto(
                u.getIdUsuario(),
                u.getDni(),
                u.getNombre(),
                u.getApellidos(),
                u.getCorreoElectronico(),
                u.getRol(),
                u.getReportesHoy() == null ? 0 : u.getReportesHoy(),
                u.getFechaUltimoReporte(),
                u.getFechaCreacion(),
                totalReportes);
    }
}