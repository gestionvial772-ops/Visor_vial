package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.AnalisisIaDto;
import com.infraestrctura.synthevia.dto.RespuestaGeminiDto;
import com.infraestrctura.synthevia.dto.TipoDanoDto;
import com.infraestrctura.synthevia.dto.TipoDanoFilaDto;
import com.infraestrctura.synthevia.entidad.Clasificacion;
import com.infraestrctura.synthevia.repositorio.ClasificacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Orquesta el analisis visual con Gemini y lo persiste en la tabla
 * clasificacion. Si el reporte ya fue analizado, el resultado anterior se
 * reemplaza para permitir el reanalisis manual desde el panel.
 */
@Service
@Transactional(readOnly = true)
public class AnalisisDanoService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisDanoService.class);

    private static final String SIN_DANO_ETIQUETA = "SIN DANO VISIBLE";

    private final GeminiDanoService geminiDanoService;
    private final ClasificacionRepository clasificacionRepository;
    private final String modelo;

    public AnalisisDanoService(
            GeminiDanoService geminiDanoService,
            ClasificacionRepository clasificacionRepository,
            @Value("${gemini.model}") String modelo) {

        this.geminiDanoService = geminiDanoService;
        this.clasificacionRepository = clasificacionRepository;
        this.modelo = modelo;
    }

    /**
     * Analiza la imagen y guarda (o reemplaza) la clasificacion del reporte.
     */
    @Transactional
    public AnalisisIaDto analizarYGuardar(
            Integer idReporte,
            MultipartFile imagen,
            String descripcion) {

        return analizarYGuardar(idReporte, leerBytes(imagen), mimeTypeDe(imagen), descripcion);
    }

    /**
     * Variante para cuando la evidencia ya se recuperó de Supabase Storage.
     */
    @Transactional
    public AnalisisIaDto analizarYGuardar(
            Integer idReporte,
            byte[] imagenBytes,
            String mimeType,
            String descripcion) {

        log.info("Analisis de IA iniciado. idReporte={}", idReporte);

        RespuestaGeminiDto cruda = geminiDanoService.analizar(imagenBytes, mimeType, descripcion);

        RespuestaGeminiDto normalizada = geminiDanoService.normalizar(cruda);

        Clasificacion clasificacion = clasificacionRepository
                .findByIdReporte(idReporte)
                .orElseGet(Clasificacion::new);

        boolean eraNuevo = clasificacion.getIdClasificacion() == null;

        clasificacion.setIdReporte(idReporte);
        clasificacion.setSeveridad(normalizada.severidad().shortValue());
        clasificacion.setEtiquetaDetectada(construirEtiqueta(normalizada.tiposDeteccion()));
        clasificacion.setNivelConfianza(toConfianza(normalizada.nivelConfianza()));
        clasificacion.setModeloIaUtilizado(nombreModelo());
        clasificacion.setVersionModelo(nombreModelo());
        clasificacion.setTieneDano(normalizada.tieneDano());
        clasificacion.setCantidadDanos(normalizada.cantidadDanos().shortValue());
        clasificacion.setReporteBreve(normalizada.reporteBreve());
        clasificacion.setFechaProcesamiento(OffsetDateTime.now());

        clasificacionRepository.save(clasificacion);

        log.info(
                "Analisis de IA {}. idReporte={}, tieneDano={}, cantidadDanos={}, severidad={}",
                eraNuevo ? "guardado" : "actualizado",
                idReporte,
                normalizada.tieneDano(),
                normalizada.cantidadDanos(),
                normalizada.severidad());

        return aDto(idReporte, clasificacion);
    }

    private byte[] leerBytes(MultipartFile imagen) {

        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("La fotografía es obligatoria para el análisis de IA.");
        }

        try {
            return imagen.getBytes();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen para el análisis de IA.", e);
        }
    }

    private String mimeTypeDe(MultipartFile imagen) {
        if (imagen == null || imagen.getContentType() == null || imagen.getContentType().isBlank()) {
            return "image/jpeg";
        }

        return imagen.getContentType();
    }

    public Optional<AnalisisIaDto> obtenerAnalisis(Integer idReporte) {
        return clasificacionRepository
                .findByIdReporte(idReporte)
                .map(c -> aDto(idReporte, c));
    }

    /**
     * Reconstruye la tabla de ocho filas del administrador a partir de la
     * etiqueta persistida, que guarda los codigos separados por coma.
     */
    private AnalisisIaDto aDto(Integer idReporte, Clasificacion c) {

        Map<String, TipoDanoDto> detectados = new LinkedHashMap<>();

        if (StringUtils.hasText(c.getEtiquetaDetectada())) {
            for (String parte : c.getEtiquetaDetectada().split(";")) {
                TipoDanoDto tipo = parsearTipo(parte);

                if (tipo != null) {
                    detectados.putIfAbsent(tipo.tipo(), tipo);
                }
            }
        }

        List<TipoDanoFilaDto> filas = new ArrayList<>();

        for (CatalogoDanos.Definicion definicion : CatalogoDanos.DEFINICIONES) {
            TipoDanoDto detectado = detectados.get(definicion.codigo());

            boolean activo = detectado != null;

            filas.add(new TipoDanoFilaDto(
                    definicion.codigo(),
                    definicion.etiqueta(),
                    activo,
                    activo && detectado.cantidad() != null ? detectado.cantidad() : 0,
                    activo ? detectado.confianza() : null));
        }

        boolean tieneDano = Boolean.TRUE.equals(c.getTieneDano());

        int cantidadDanos = c.getCantidadDanos() == null
                ? 0
                : c.getCantidadDanos();

        short severidad = c.getSeveridad() == null ? (short) 1 : c.getSeveridad();

        return new AnalisisIaDto(
                idReporte,
                true,
                tieneDano,
                cantidadDanos,
                severidad,
                etiquetaSeveridad(severidad),
                filas,
                c.getNivelConfianza(),
                c.getModeloIaUtilizado(),
                c.getVersionModelo(),
                c.getReporteBreve(),
                c.getFechaProcesamiento());
    }

    /**
     * Serializa los tipos detectados con su cantidad y su confianza por tipo,
     * con el formato CODIGO:cantidad:confianza separado por punto y coma. Cabe
     * de sobra en los 255 caracteres de etiqueta_detectada y evita perder ese
     * detalle al persistir.
     */
    private String construirEtiqueta(List<TipoDanoDto> tipos) {
        if (tipos == null || tipos.isEmpty()) {
            return SIN_DANO_ETIQUETA;
        }

        String etiqueta = tipos.stream()
                .map(t -> String.format(
                        "%s:%d:%d",
                        t.tipo(),
                        t.cantidad() == null ? 1 : t.cantidad(),
                        t.confianza() == null ? 0 : t.confianza()))
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(SIN_DANO_ETIQUETA);

        return recortar(etiqueta, 255);
    }

    /**
     * Interpreta un segmento CODIGO:cantidad:confianza. Tolera el formato
     * antiguo que solo traia el codigo, para no romper datos ya guardados.
     */
    private TipoDanoDto parsearTipo(String parte) {

        if (parte == null) {
            return null;
        }

        String[] campos = parte.trim().split(":");

        String codigo = campos[0].trim().toUpperCase(Locale.ROOT);

        if (codigo.isEmpty() || SIN_DANO_ETIQUETA.equals(codigo)) {
            return null;
        }

        Integer cantidad = null;
        Integer confianza = null;

        try {
            if (campos.length > 1) {
                cantidad = Integer.valueOf(campos[1].trim());
            }

            if (campos.length > 2) {
                confianza = Integer.valueOf(campos[2].trim());
            }
        } catch (NumberFormatException e) {
            cantidad = null;
            confianza = null;
        }

        return new TipoDanoDto(codigo, cantidad, confianza);
    }

    private BigDecimal toConfianza(Integer nivelConfianza) {
        if (nivelConfianza == null) {
            return null;
        }

        return BigDecimal.valueOf(Math.min(100, Math.max(0, nivelConfianza)));
    }

    private String nombreModelo() {
        return recortar(modelo, 255);
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }

        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private String etiquetaSeveridad(short severidad) {
        return switch (severidad) {
            case 1 -> "Leve";
            case 2 -> "Moderado";
            case 3 -> "Severo";
            default -> "Leve";
        };
    }
}
