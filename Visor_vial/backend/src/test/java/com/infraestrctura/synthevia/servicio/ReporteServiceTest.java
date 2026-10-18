package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.CrearReporteDto;
import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.repositorio.ReporteRepository;
import com.infraestrctura.synthevia.repositorio.ViaSegmentoRepository;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteServiceTest {
    ReporteRepository repository = mock(ReporteRepository.class);
    ViaSegmentoRepository vias = mock(ViaSegmentoRepository.class);
    CoordenadaService coordenadas = mock(CoordenadaService.class);
    RegistroTiempoService tiempos = mock(RegistroTiempoService.class);
    SupabaseStorageService storage = mock(SupabaseStorageService.class);
    ReporteService service = new ReporteService(repository, coordenadas, tiempos, storage, vias);
    MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[]{1});
    CrearReporteDto dto = new CrearReporteDto(15, "Bache", -12.0, -77.0,
            BigDecimal.ONE, OffsetDateTime.now());

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    void prepararCreacion() {
        when(vias.existsById(15)).thenReturn(true);
        when(storage.generarRuta(imagen)).thenReturn("reportes/prueba.png");
        when(repository.save(any())).thenAnswer(invocation -> {
            Reporte r = invocation.getArgument(0);
            r.setIdReporte(70);
            return r;
        });
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void guardaViaUsuarioCoordenadaEImagenEnElMismoFlujo() {
        prepararCreacion();
        Reporte reporte = service.crearDesdeSistema(dto, imagen, 42);
        assertEquals(42, reporte.getIdUsuario());
        assertEquals(15, reporte.getIdVia());
        assertEquals("reportes/prueba.png", reporte.getImagenUrl());
        var orden = inOrder(repository, coordenadas, storage, tiempos);
        orden.verify(repository).save(any());
        orden.verify(coordenadas).guardar(70, -12.0, -77.0, BigDecimal.ONE);
        orden.verify(storage).subir("reportes/prueba.png", imagen);
        orden.verify(tiempos).registrarConsolidacion(70);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage, never()).eliminarSilenciosamente(any());
    }

    @Test
    void limpiaStorageSiFallaElCommit() {
        prepararCreacion();
        service.crearDesdeSistema(dto, imagen, 42);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).eliminarSilenciosamente("reportes/prueba.png");
    }

    @Test
    void propagaFalloDeStorageParaRevertirLaTransaccion() {
        prepararCreacion();
        doThrow(new IllegalStateException("Storage caido")).when(storage).subir(any(), any());
        assertThrows(IllegalStateException.class, () -> service.crearDesdeSistema(dto, imagen, 42));
        verify(tiempos, never()).registrarConsolidacion(any());
    }

    @Test
    void rechazaViaInexistenteAntesDeGuardarOEnviarImagen() {
        assertThrows(ResponseStatusException.class, () -> service.crearDesdeSistema(dto, imagen, 42));
        verifyNoInteractions(repository, coordenadas, tiempos, storage);
    }

    @Test
    void ignoraUsuarioEnviadoPorElCliente() {
        Reporte body = new Reporte();
        body.setIdUsuario(1);
        service.crear(body, 42);
        verify(repository).save(argThat(r -> r.getIdUsuario().equals(42)));
    }

    @Test
    void anulacionConservaLaFilaYBloqueaLecturaYActualizacion() {
        Reporte r = new Reporte();
        r.setIdReporte(70);
        r.setEstado("PENDIENTE");
        when(repository.findById(70)).thenReturn(Optional.of(r));
        assertTrue(service.eliminar(70));
        assertNotNull(r.getEliminadoEn());
        assertEquals("PENDIENTE", r.getEstado());
        assertTrue(service.obtener(70).isEmpty());
        assertTrue(service.actualizarEstado(70, "RESUELTO").isEmpty());
        assertFalse(service.eliminar(70));
        verify(repository, times(1)).save(r);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void estadisticasUsanSoloConsultasDeActivos() {
        service.obtenerEstadisticas();
        verify(repository).contarTotal();
        for (String estado : new String[]{"PENDIENTE", "EN_PROCESO", "RESUELTO", "DUPLICADO"})
            verify(repository).contarActivosPorEstado(estado);
        verifyNoMoreInteractions(repository);
    }
}
