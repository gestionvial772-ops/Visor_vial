package com.infraestrctura.synthevia.servicio;

import com.infraestrctura.synthevia.dto.CalidadImagenDto;
import com.infraestrctura.synthevia.dto.RespuestaGeminiDto;
import com.infraestrctura.synthevia.dto.TipoDanoDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cliente de Google Gemini para el analisis visual del pavimento.
 *
 * <p>La peticion declara un responseSchema, de modo que la API responde con
 * JSON validado contra el contrato {@link RespuestaGeminiDto} en lugar de
 * texto libre que haya que parsear a mano.</p>
 */
@Service
public class GeminiDanoService {

    private static final Logger log = LoggerFactory.getLogger(GeminiDanoService.class);

    private static final long REINTENTO_ESPERA_MS = 2000L;

    private static final String INSTRUCCIONES_SISTEMA = """
            Eres un ingeniero civil especialista en conservacion de pavimentos asfaltados.
            Analizas UNA fotografia de la superficie de una via publica de la ciudad de Trujillo, Peru,
            y determinas que deterioros visibles presenta.

            CATALOGO CERRADO DE DETERIOROS (usa exclusivamente estos codigos):
            - FISURAS: lineas de fractura en la capa asfaltada. Se ve la linea, pero NO hay
              perdida de material. Incluye transversal, longitudinal, diagonal y en red.
            - PIEL_DE_COCODRILO: mallado de muchas fisuras cortas que se cortan entre si formando
              celdas poligonales, como la piel de un cocodrilo. Es un PATRON, no una sola grieta.
            - RAJADURA: el material de la capa superficial se ha desprendido o esta suelto
              (gravilla perdida, desagregacion), pero la superficie conserva su nivel.
              No hay fractura dominante ni cavidad.
            - BACHES: depresion poco profunda en la superficie donde se perdio una capa de
              material, pero se ve el FONDO de la depression y la base sigue en su lugar.
            - HUECOS: cavidad profunda de borde definido donde el material se perdio hasta
              la subrasante. Se ve el suelo, tierra o vacio al fondo. Un bache se rellena
              con agua o arena; un hueco es un socavon.
            - DESNIVEL: escalon o diferencia de nivel puntual entre dos tramos de la calzada.
              El salto es el fenomeno dominante, no hay cavidad ni grieta.
            - HUNDIMIENTO: la calzada se hunde de forma amplia y continua por perdida de
              soporte del terreno. La superficie se deforma hacia abajo; no hay cavidad
              definida ni red de grietas como fenomeno dominante.
            - DANO_EN_RAMPA: descarga en el encuentro de una rampa, o peldaño visible en el
              borde de la calzada. Solo aplica si hay una rampa identificable en la imagen.

            REGLA DE DESEMPATE (aplicala siempre, es lo mas importante):
            Un mismo fenomeno puede encajar en varias categorias. Decide por la MECANICA DOMINANTE:
              - se ve una linea, no falta material            -> FISURAS
              - las lineas forman un mallado de celdas         -> PIEL_DE_COCODRILO
              - falta material en la superficie, sin cavidad   -> RAJADURA
              - depresion con fondo visible, poco profunda     -> BACHES
              - cavidad profunda con suelo o vacio al fondo    -> HUECOS
              - el salto entre dos niveles es lo que domina    -> DESNIVEL
              - la calzada se hunde en conjunto               -> HUNDIMIENTO
            Nunca asignes dos categorias que describan el mismo defecto. Si un fenomeno tiene
            varios componentes (por ejemplo un socavon rodeado de grietas), asigna la categoria
            del componente dominante y cuenta las grietas como FISURAS solo si son claramente
            un fenomeno independiente.

            OTRAS REGLAS OBLIGATORIAS:
            1. Evalua los ocho conceptos, pero reporta en tipos_deteccion SOLO los que realmente observes.
            2. No inventes deterioros ni completes la lista por simetria. Ante duda, no lo reportes.
            3. Si la imagen no muestra un pavimento deteriorado, entonces tiene_dano = false,
               cantidad_danos = 0, severidad = 1 y tipos_deteccion = [].
            4. cantidad_danos es el total de deterioros observados: la suma de las cantidades por tipo.
            5. severidad usa los valores 1 (LEVE), 2 (MODERADO) o 3 (SEVERO) segun la magnitud
               del dano respecto de la superficie circundante.
            6. La descripcion escrita por el ciudadano es solo una pista. Si contradice lo que se ve
               en la fotografia, la fotografia tiene prioridad.
            7. Antes de responder, revisa punto por punto los ocho conceptos y descarta los que
               no puedas señalar de forma concreta en la imagen.
            8. reporte_breve debe tener entre 20 y 45 palabras en espanol, describir el estado del
               pavimento y el riesgo para los peatones y conductores. Sin listas ni encabezados.

            COMO CALCULAS nivel_confianza (importante, sigue esta escala al pie de la letra):
            El numero NO es tu opinion sobre el analisis. Es una medida de la CALIDAD DE LA EVIDENCIA
            fotografica que recibiste. No confundas "estoy seguro del tipo de dano" con "la foto me
            permite emitir un diagnostico confiable".

            Antes de decidir el numero, evalua estos cinco factores de la imagen:
              a) ENCUADRE: el deterioro esta centrado y completo, o esta cortado por el borde?
              b) DISTANCIA Y ESCALA: se distinguen la textura y el borde de la cavidad, o es un plano
                 lejano donde solo se ve una mancha oscura?
              c) ILUMINACION Y SOMBRA: hay contraluz, sombra dura o penumbra que ocultan el fondo?
              d) NITIDEZ: esta movida, con ruido, o nitida?
              e) OCCLUSION: hay vehiculos, personas, follaje, agua o conos tapando parte del dano?

            Traduce esa evaluacion a la escala:
              90 a 100: los cinco factores son favorables. Encuadre centrado, escala cercana,
                         luz uniforme, imagen nitida y sin nada que tape el deterioro.
              75 a 89:  la mayor parte es favorable, pero un factor dificulta leer el fondo
                         o el borde (por ejemplo buena luz pero algo lejos, o leve movimiento).
              55 a 74:  dos o mas factores son desfavorables. El tipo de dano probably es correcto,
                         pero no puedes afirmar cuanta extension tiene ni ver el fondo con claridad.
              30 a 54:  la imagen no permite un diagnostico solido. Ves una zona afectada pero no
                         distingues bien que deterioro es. Reporta solo lo que sea inequivoco.
              0 a 29:   la imagen es inutil para el diagnostico (borrosa, de noche, lejana,
                         o el dano no se ve). En ese caso deja tiene_dano en false y tipos_deteccion vacio.

            La confianza NO sube porque el dano sea grave ni porque hayas razonado mucho. Solo sube
            cuando la fotografia es buena. Un bache enorme en una foto movida y tomada desde un
            auto en marcha tiene confianza baja aunque sea obvio que hay un bache.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;
    private final String apiBaseUrl;

    public GeminiDanoService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.api-base-url}") String apiBaseUrl,
            @Value("${gemini.timeout-segundos}") long timeoutSegundos) {

        this.restClient = restClientBuilder
                .requestFactory(crearFactory(timeoutSegundos, 10))
                .build();

        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Se usa SimpleClientHttpRequestFactory y no la variante del JDK porque esta
     * ultima reporta los tiempos agotados como "Request cancelled", un
     * IOException opaco que no permite distinguir un fallo de red real.
     */
    static SimpleClientHttpRequestFactory crearFactory(long segundos, long connectSegundos) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(segundos));
        factory.setConnectTimeout(Duration.ofSeconds(connectSegundos));
        return factory;
    }

    /**
     * Analiza la imagen del reporte y devuelve la respuesta ya validada por
     * el responseSchema de Gemini.
     */
    public RespuestaGeminiDto analizar(
            MultipartFile imagen,
            String descripcion) {

        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("La fotografía es obligatoria para el análisis de IA.");
        }

        try {
            return analizar(
                    imagen.getBytes(),
                    imagen.getContentType() != null ? imagen.getContentType() : "image/jpeg",
                    descripcion);

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen para el análisis de IA.", e);
        }
    }

    public RespuestaGeminiDto analizar(
            byte[] imagenBytes,
            String mimeType,
            String descripcion) {

        if (imagenBytes == null || imagenBytes.length == 0) {
            throw new IllegalArgumentException("La fotografía es obligatoria para el análisis de IA.");
        }

        Map<String, Object> cuerpo = construirCuerpo(
                Base64.getEncoder().encodeToString(imagenBytes),
                mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType,
                descripcion);

        String url = apiBaseUrl + "/" + model + ":generateContent?key=" + apiKey;

        return conReintento(() -> {

            String cuerpoRespuesta = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);

            return extraerRespuesta(cuerpoRespuesta);
        });
    }

    /**
     * Reintenta una sola vez ante fallos transitorios: errores de red puros y
     * respuestas 5xx del servicio (503 por demanda alta es habitual en el free
     * tier). NO reintenta ante 429, porque la cuota gratuita del proyecto es de
     * 20 requests por dia y un reintento en ese caso la consume igual.
     */
    private RespuestaGeminiDto conReintento(Supplier<RespuestaGeminiDto> llamada) {

        try {
            return llamada.get();

        } catch (ResourceAccessException e) {
            log.warn("Fallo de red al llamar a Gemini. Se reintenta una vez. Causa: {}",
                    e.getMessage());

            return llamada.get();

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().is5xxServerError()) {
                log.warn("Gemini respondio {}. Se reintenta una vez.",
                        e.getStatusCode().value());

                pausar();

                return llamada.get();
            }

            throw e;
        }
    }

    private void pausar() {
        try {
            Thread.sleep(REINTENTO_ESPERA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> construirCuerpo(
            String imagenBase64,
            String mimeType,
            String descripcion) {

        Map<String, Object> parteTexto = Map.of(
                "text", construirMensajeUsuario(descripcion));

        Map<String, Object> parteImagen = Map.of(
                "inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", imagenBase64));

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("role", "user");
        contenido.put("parts", List.of(parteTexto, parteImagen));

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", INSTRUCCIONES_SISTEMA)));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        // El razonamiento interno se descuenta de max_output_tokens. Con 0 el modelo
        // respondia de memoria y se equivocaba; con 0 tampoco cabia la respuesta y
        // salia truncada. 1024 de pensamiento + 4096 de salida cubren ambos casos.
        generationConfig.put("max_output_tokens", 4096);
        generationConfig.put("thinking_config", Map.of("thinking_budget", 1024));
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("response_schema", construirEsquemaRespuesta());

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("system_instruction", systemInstruction);
        cuerpo.put("contents", List.of(contenido));
        cuerpo.put("generation_config", generationConfig);

        return cuerpo;
    }

    private String construirMensajeUsuario(String descripcion) {
        String base = "Analiza esta fotografía de la superficie de la vía y devuelve el análisis de daños.";

        if (StringUtils.hasText(descripcion)) {
            return base + "\n\nDescripción escrita por el ciudadano: \"" + descripcion.trim() + "\"";
        }

        return base + "\n\nEl ciudadano no escribió ninguna descripción.";
    }

    private Map<String, Object> construirEsquemaRespuesta() {
        Map<String, Object> tipoDetectado = new LinkedHashMap<>();
        tipoDetectado.put("type", "OBJECT");
        tipoDetectado.put("properties", Map.of(
                "tipo", Map.of(
                        "type", "STRING",
                        "enum", CatalogoDanos.DEFINICIONES.stream()
                                .map(CatalogoDanos.Definicion::codigo)
                                .toList()),
                "cantidad", Map.of("type", "INTEGER", "minimum", 1, "maximum", 50),
                "confianza", Map.of("type", "INTEGER", "minimum", 0, "maximum", 100)));
        tipoDetectado.put("required", List.of("tipo", "cantidad", "confianza"));

        Map<String, Object> severidad = new LinkedHashMap<>();
        severidad.put("type", "INTEGER");
        severidad.put("minimum", 1);
        severidad.put("maximum", 3);

        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("tiene_dano", Map.of("type", "BOOLEAN"));
        propiedades.put("cantidad_danos", Map.of("type", "INTEGER", "minimum", 0, "maximum", 50));
        propiedades.put("severidad", severidad);
        propiedades.put("nivel_confianza", Map.of("type", "INTEGER", "minimum", 0, "maximum", 100));
        propiedades.put("tipos_deteccion", Map.of(
                "type", "ARRAY",
                "items", tipoDetectado));
        propiedades.put("calidad_imagen", construirEsquemaCalidad());
        propiedades.put("reporte_breve", Map.of("type", "STRING"));

        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "OBJECT");
        esquema.put("properties", propiedades);
        esquema.put("required", List.of(
                "tiene_dano",
                "cantidad_danos",
                "severidad",
                "nivel_confianza",
                "tipos_deteccion",
                "calidad_imagen",
                "reporte_breve"));

        return esquema;
    }

    /**
     * Los cinco factores que el modelo debe puntuar para derivar de ahi la
     * confianza. Se exigen en el esquema para que el numero final tenga un
     * respaldo explicito y no sea una opinion sin sustento.
     */
    private Map<String, Object> construirEsquemaCalidad() {
        Map<String, Object> factor = Map.of(
                "type", "INTEGER",
                "minimum", 0,
                "maximum", 100);

        Map<String, Object> propiedades = new LinkedHashMap<>();
        propiedades.put("encuadre", factor);
        propiedades.put("escala", factor);
        propiedades.put("iluminacion", factor);
        propiedades.put("nitidez", factor);
        propiedades.put("oclusion", factor);

        Map<String, Object> calidad = new LinkedHashMap<>();
        calidad.put("type", "OBJECT");
        calidad.put("properties", propiedades);
        calidad.put("required", List.of(
                "encuadre",
                "escala",
                "iluminacion",
                "nitidez",
                "oclusion"));

        return calidad;
    }

    private RespuestaGeminiDto extraerRespuesta(String cuerpoRespuesta) {

        if (!StringUtils.hasText(cuerpoRespuesta)) {
            throw new IllegalStateException("Gemini devolvió una respuesta vacía.");
        }

        try {
            JsonNode raiz = objectMapper.readTree(cuerpoRespuesta);

            JsonNode candidates = raiz.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException(
                        "Gemini no devolvió candidatos. Respuesta: " + cuerpoRespuesta);
            }

            JsonNode parts = candidates.get(0)
                    .path("content")
                    .path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                String motivo = candidates.get(0).path("finishReason").asString("");
                throw new IllegalStateException(
                        "Gemini terminó sin contenido (finishReason=" + motivo + ").");
            }

            StringBuilder texto = new StringBuilder();

            for (JsonNode part : parts) {
                if (part.has("text")) {
                    texto.append(part.get("text").asString());
                }
            }

            if (texto.isEmpty()) {
                throw new IllegalStateException("Gemini devolvió un contenido sin texto.");
            }

            String contenido = texto.toString();

            try {
                return objectMapper.readValue(contenido, RespuestaGeminiDto.class);

            } catch (JacksonException e) {
                throw new IllegalStateException(
                        "Gemini truncó la respuesta. finishReason="
                                + candidates.get(0).path("finishReason").asString("")
                                + ". Texto recibido: " + contenido,
                        e);
            }

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "La respuesta de Gemini no fue JSON válido: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * Normaliza la respuesta cruda: descarta tipos ajenos al catalogo y
     * recalcula el total a partir de las cantidades por tipo.
     */
    public RespuestaGeminiDto normalizar(RespuestaGeminiDto cruda) {

        List<TipoDanoDto> detectados = new ArrayList<>();
        int total = 0;

        if (cruda.tiposDeteccion() != null) {
            for (TipoDanoDto tipo : cruda.tiposDeteccion()) {
                if (tipo == null || !StringUtils.hasText(tipo.tipo())) {
                    continue;
                }

                String codigo = tipo.tipo().trim().toUpperCase().replace(' ', '_').replace('-', '_');

                boolean perteneceAlCatalogo = CatalogoDanos.DEFINICIONES.stream()
                        .anyMatch(d -> d.codigo().equals(codigo));

                if (!perteneceAlCatalogo) {
                    log.warn("Se descartó el tipo de daño fuera de catálogo: {}", tipo.tipo());
                    continue;
                }

                int cantidad = tipo.cantidad() == null ? 1 : Math.max(1, tipo.cantidad());

                int confianza = tipo.confianza() == null
                        ? 0
                        : Math.min(100, Math.max(0, tipo.confianza()));

                detectados.add(new TipoDanoDto(codigo, cantidad, confianza));

                total += cantidad;
            }
        }

        boolean tieneDano = !detectados.isEmpty();

        int cantidadDanos = tieneDano ? total : 0;

        int severidad = cruda.severidad() == null ? 1 : Math.min(3, Math.max(1, cruda.severidad()));

        if (!tieneDano) {
            severidad = 1;
        }

        CalidadImagenDto calidad = normalizarCalidad(cruda.calidadImagen());

        int confianzaGlobal = derivarConfianza(calidad, tieneDano);

        String reporte = cruda.reporteBreve();

        return new RespuestaGeminiDto(
                tieneDano,
                cantidadDanos,
                severidad,
                confianzaGlobal,
                detectados,
                calidad,
                reporte);
    }

    private CalidadImagenDto normalizarCalidad(CalidadImagenDto cruda) {

        if (cruda == null) {
            return new CalidadImagenDto(50, 50, 50, 50, 50);
        }

        return new CalidadImagenDto(
                acotar(cruda.encuadre()),
                acotar(cruda.escala()),
                acotar(cruda.iluminacion()),
                acotar(cruda.nitidez()),
                acotar(cruda.oclusion()));
    }

    private int acotar(Integer valor) {
        if (valor == null) {
            return 50;
        }

        return Math.min(100, Math.max(0, valor));
    }

    /**
     * Deriva la confianza del promedio de los cinco factores de calidad. El
     * promedio pondera igual porque ninguno manda sobre los otros: una foto
     * nitida pero tomada desde lejos no permite diagnosticar igual de bien.
     *
     * <p>Si no se detecto ningun deterioro la confianza se recorta a la mitad
     * porque normalmente significa que la foto no Allowe ver la superficie.</p>
     */
    private int derivarConfianza(CalidadImagenDto calidad, boolean tieneDano) {

        int suma = calidad.encuadre()
                + calidad.escala()
                + calidad.iluminacion()
                + calidad.nitidez()
                + calidad.oclusion();

        int promedio = Math.round(suma / 5.0f);

        return tieneDano ? promedio : Math.min(promedio, 50);
    }
}
