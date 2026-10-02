package com.infraestrctura.synthevia.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final long TAMANIO_MAXIMO = 5L * 1024L * 1024L;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    /** Vigencia de la URL firmada que se envia al navegador. */
    private static final int DURACION_FIRMA_SEGUNDOS = 3600;

    private final RestClient restClient;
    private final RestClient restClientExterno;
    private final String bucket;
    private final String supabaseUrl;

    public SupabaseStorageService(
            RestClient.Builder restClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.secret-key}") String secretKey,
            @Value("${supabase.storage.bucket}") String bucket) {

        this.restClient = restClientBuilder
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("apikey", secretKey)
                .build();

        this.restClientExterno = restClientBuilder
                .requestFactory(GeminiDanoService.crearFactory(30, 10))
                .build();

        this.bucket = bucket;
        this.supabaseUrl = supabaseUrl;
    }

    public String generarRuta(MultipartFile archivo) {

        validar(archivo);

        String extension = obtenerExtension(archivo.getContentType());

        LocalDate fecha = LocalDate.now(ZoneOffset.UTC);

        return String.format(
                "reportes/%d/%02d/%s.%s",
                fecha.getYear(),
                fecha.getMonthValue(),
                UUID.randomUUID(),
                extension);
    }

    public void subir(
            String ruta,
            MultipartFile archivo) {

        validar(archivo);

        try {

            restClient
                    .post()
                    .uri("/object/" + bucket + "/" + ruta)
                    .contentType(
                            MediaType.parseMediaType(
                                    archivo.getContentType()))
                    .header("x-upsert", "false")
                    .body(archivo.getBytes())
                    .retrieve()
                    .toBodilessEntity();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen recibida.", e);

        } catch (Exception e) {
            throw new IllegalStateException(  "No se pudo subir la imagen a Supabase Storage.",e);
        }
    }

    public void eliminarSilenciosamente(String ruta) {

        try {
            restClient
                    .delete()
                    .uri("/object/" + bucket + "/" + ruta)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception ignored) {
        }
    }

    /**
     * URL que puede cargar un navegador. El bucket es privado, asi que no se
     * puede usar /object/public: hay que firmar la ruta con una clave temporal.
     *
     * <p>Si el valor ya es una URL absoluta se devuelve tal cual, porque en ese
     * caso el reporte se creo desde el panel con una URL publica.</p>
     */
    public String urlPublica(String imagenUrl) {

        if (imagenUrl == null || imagenUrl.isBlank()) {
            return null;
        }

        String referencia = imagenUrl.trim();

        if (esUrlAbsoluta(referencia)) {
            return referencia;
        }

        try {
            Map<String, Object> cuerpo = Map.of("expiresIn", DURACION_FIRMA_SEGUNDOS);

            Firma firmado = restClient
                    .post()
                    .uri("/object/sign/" + bucket + "/" + referencia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Firma.class);

            if (firmado == null || firmado.signedUrl() == null) {
                throw new IllegalStateException("Supabase no devolvió la firma.");
            }

            // la API devuelve una ruta relativa a /object/sign, hay que anteponer
            // el host y el prefijo /storage/v1
            return supabaseUrl + "/storage/v1" + firmado.signedUrl();

        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo generar la URL de la imagen: " + referencia, e);
        }
    }

    private record Firma(
            @JsonProperty("signedURL")
            String signedUrl) {
    }

    /**
     * Recupera la evidencia de un reporte. La columna imagen_url puede guardar
     * dos cosas segun como se creo el reporte: una ruta dentro del bucket de
     * Supabase Storage (flujo ciudadano) o una URL publica externa (reportes
     * cargados por URL desde el panel). Se soportan ambas.
     */
    public ImagenDescargada descargar(String imagenUrl) {

        if (imagenUrl == null || imagenUrl.isBlank()) {
            throw new IllegalArgumentException("El reporte no tiene imagen registrada.");
        }

        String referencia = imagenUrl.trim();

        try {
            byte[] contenido = esUrlAbsoluta(referencia)
                    ? descargarExterno(referencia)
                    : descargarDelBucket(referencia);

            if (contenido == null || contenido.length == 0) {
                throw new IllegalStateException("La imagen está vacía: " + referencia);
            }

            return new ImagenDescargada(contenido, deducirMimeType(referencia));

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo recuperar la imagen del reporte: " + referencia, e);
        }
    }

    private boolean esUrlAbsoluta(String referencia) {
        String minuscula = referencia.toLowerCase();
        return minuscula.startsWith("http://") || minuscula.startsWith("https://");
    }

    private byte[] descargarDelBucket(String ruta) {

        return restClient
                .get()
                .uri("/object/" + bucket + "/" + ruta)
                .retrieve()
                .body(byte[].class);
    }

    /**
     * Descarga desde una URL externa usando un cliente aparte, sin la cabecera
     * apikey, para no filtrar la clave de Supabase a terceros.
     */
    private byte[] descargarExterno(String url) {

        return restClientExterno
                .get()
                .uri(url)
                .retrieve()
                .body(byte[].class);
    }

    public record ImagenDescargada(byte[] contenido, String mimeType) {
    }

    private String deducirMimeType(String referencia) {

        String sinQuery = referencia;
        int indice = sinQuery.indexOf('?');

        if (indice >= 0) {
            sinQuery = sinQuery.substring(0, indice);
        }

        String minuscula = sinQuery.toLowerCase();

        if (minuscula.endsWith(".png")) {
            return "image/png";
        }

        if (minuscula.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/jpeg";
    }

    private void validar(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {

            throw new IllegalArgumentException("La fotografía es obligatoria.");
        }

        if (archivo.getSize() > TAMANIO_MAXIMO) {

            throw new IllegalArgumentException("La fotografía no puede superar los 5 MB.");
        }

        String tipoContenido = archivo.getContentType();

        if (tipoContenido == null || !TIPOS_PERMITIDOS.contains(tipoContenido)) {

            throw new IllegalArgumentException("Formato de imagen no permitido.");
        }
    }
    private String obtenerExtension(String contentType) {

        return switch (contentType) {

            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default ->
                throw new IllegalArgumentException("Formato de imagen no permitido.");
        };
    }
}