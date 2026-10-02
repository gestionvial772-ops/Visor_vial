package com.infraestrctura.synthevia.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class CaptchaService {

    private final RestClient restClient;

    private final String secretKey;

    public CaptchaService(
            RestClient.Builder restClientBuilder,
            @Value("${recaptcha.secret-key}") String secretKey) {

        this.restClient = restClientBuilder.baseUrl("https://www.google.com/recaptcha/api").build();
        this.secretKey = secretKey;
    }

    public boolean validar(String captchaToken) {

        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();

        formulario.add("secret", secretKey);

        formulario.add("response", captchaToken);

        RespuestaCaptcha respuesta = restClient
                .post()
                .uri("/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaCaptcha.class);

        return respuesta != null && respuesta.success();
    }

    private record RespuestaCaptcha(boolean success) {
    }
}