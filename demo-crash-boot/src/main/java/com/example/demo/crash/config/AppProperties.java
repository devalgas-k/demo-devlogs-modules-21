package com.example.demo.crash.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.integration")
public class AppProperties {

    @NotBlank(message = "L'URL de l'API externe est obligatoire pour le démarrage (app.integration.api-url)")
    private String apiUrl;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}
