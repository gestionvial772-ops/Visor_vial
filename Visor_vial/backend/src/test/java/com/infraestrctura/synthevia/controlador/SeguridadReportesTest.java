package com.infraestrctura.synthevia.controlador;

import com.infraestrctura.synthevia.config.SecurityConfig;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.entidad.Usuario;
import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import com.infraestrctura.synthevia.servicio.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.data.domain.Page;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeguridadReportesTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    ReporteService reportes;
    SupabaseStorageService storage;

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config {
        @Bean ReporteService reportes() { return mock(ReporteService.class); }
        @Bean SupabaseStorageService storage() { return mock(SupabaseStorageService.class); }
        @Bean ViaSegmentoRepository vias() { return mock(ViaSegmentoRepository.class); }
        @Bean UsuarioService usuarios() { return mock(UsuarioService.class); }
        @Bean CaptchaService captcha() { return mock(CaptchaService.class); }
        @Bean ReporteController admin(ReporteService r, SupabaseStorageService s) {
            return new ReporteController(r, s);
        }
        @Bean ReporteCiudadanoController ciudadano(ReporteService r, ViaSegmentoRepository v,
                SupabaseStorageService s) { return new ReporteCiudadanoController(r, v, s); }
        @Bean AutenticacionController auth(UsuarioService u, CaptchaService c, SecurityContextRepository s) {
            return new AutenticacionController(u, c, s);
        }
    }

    @BeforeEach
    void preparar() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        reportes = context.getBean(ReporteService.class);
        storage = context.getBean(SupabaseStorageService.class);
    }

    @AfterEach void cerrar() { context.close(); }

    @Test void listadoDelPanelFuncionaSinSesionComoAntes() throws Exception {
        when(reportes.listar(0, 10, null, null)).thenReturn(Page.empty());
        mvc.perform(get("/admin/reportes")).andExpect(status().isOk());
        mvc.perform(get("/reportes/mios")).andExpect(status().isUnauthorized());
        verify(reportes).listar(0, 10, null, null);
    }

    @Test void ciudadanoPuedeAdministrarTemporalmenteConSesionYCsrf() throws Exception {
        when(reportes.listar(0, 10, null, null)).thenReturn(Page.empty());
        when(reportes.eliminar(70)).thenReturn(true);
        mvc.perform(get("/admin/reportes").with(user("42").roles("CIUDADANO")))
                .andExpect(status().isOk());
        mvc.perform(delete("/admin/reportes/70").with(user("42").roles("CIUDADANO")).with(csrf()))
                .andExpect(status().isOk());
        verify(reportes).listar(0, 10, null, null);
        verify(reportes).eliminar(70);
    }

    @Test void adminNecesitaCsrfParaAnular() throws Exception {
        mvc.perform(delete("/admin/reportes/70").with(user("42").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportes);
        when(reportes.eliminar(70)).thenReturn(true);
        mvc.perform(delete("/admin/reportes/70").with(user("42").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        verify(reportes).eliminar(70);
    }

    @Test void propiosUsaIdentidadDeSesion() throws Exception {
        when(reportes.listarPropios(42, 0, 10)).thenReturn(Page.empty());
        mvc.perform(get("/reportes/mios?idUsuario=1").with(user("42").roles("CIUDADANO")))
                .andExpect(status().isOk());
        verify(reportes).listarPropios(42, 0, 10);
    }

    @Test void imagenSoloParaElPropietario() throws Exception {
        Reporte r = new Reporte();
        r.setIdUsuario(42);
        r.setImagenUrl("reportes/foto.png");
        when(reportes.obtener(70)).thenReturn(Optional.of(r));
        mvc.perform(get("/reportes/70/imagen").with(user("99").roles("CIUDADANO")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(storage);
        when(storage.urlPublica("reportes/foto.png")).thenReturn("https://example.test/firmada");
        mvc.perform(get("/reportes/70/imagen").with(user("42").roles("CIUDADANO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://example.test/firmada"));
    }

    @Test void loginPersisteSesionRotaIdentificadorYLogoutLaInvalida() throws Exception {
        Usuario u = new Usuario();
        u.setIdUsuario(42);
        u.setNombre("Fabricio");
        u.setRol("CIUDADANO");
        when(context.getBean(CaptchaService.class).validar("captcha")).thenReturn(true);
        when(context.getBean(UsuarioService.class).autenticar("12345678", "clave"))
                .thenReturn(Optional.of(u));
        var csrfResponse = mvc.perform(get("/public/csrf"))
                .andExpect(status().isOk()).andReturn();
        var csrfJson = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(csrfResponse.getResponse().getContentAsString());
        var anterior = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        var resultado = mvc.perform(post("/public/iniciar-sesion").session(anterior)
                .header(csrfJson.get("headerName").asText(), csrfJson.get("token").asText())
                .contentType("application/json")
                .content("{\"dni\":\"12345678\",\"contrasena\":\"clave\",\"captchaToken\":\"captcha\",\"recordarme\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exito").value(true)).andReturn();
        var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertNotNull(sesion);
        assertNotEquals(anterior.getId(), sesion.getId());
        when(reportes.listarPropios(42, 0, 10)).thenReturn(Page.empty());
        mvc.perform(get("/reportes/mios").session(sesion)).andExpect(status().isOk());
        when(reportes.listar(0, 10, null, null)).thenReturn(Page.empty());
        mvc.perform(get("/admin/reportes").session(sesion)).andExpect(status().isOk());
        mvc.perform(post("/public/cerrar-sesion").session(sesion).with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(sesion.isInvalid());
    }
}
