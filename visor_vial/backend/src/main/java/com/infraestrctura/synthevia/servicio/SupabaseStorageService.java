package com.infraestrctura.synthevia.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final long TAMANIO_MAXIMO = 5L * 1024L * 1024L;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final RestClient restClient;
    private final String bucket;

    public SupabaseStorageService(
            RestClient.Builder restClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.secret-key}") String secretKey,
            @Value("${supabase.storage.bucket}") String bucket) {

        this.restClient = restClientBuilder
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("apikey", secretKey)
                .build();

        this.bucket = bucket;
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