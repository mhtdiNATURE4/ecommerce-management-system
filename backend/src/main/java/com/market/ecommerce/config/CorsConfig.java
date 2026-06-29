package com.market.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins from environment, e.g.
     * ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
     *
     * If empty, no origins are allowed (safer default). Do NOT hardcode localhost or production URLs here.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${ALLOWED_ORIGINS:}") String allowedOriginsEnv) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = Collections.emptyList();
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        if (allowedOrigins.isEmpty()) {
            // Local development default origins for frontend dev servers.
            configuration.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "http://localhost:5173",
                    "http://127.0.0.1:5173"
            ));
            configuration.setAllowCredentials(true);
        } else if (allowedOrigins.size() == 1 && "*".equals(allowedOrigins.get(0))) {
            // Explicit wildcard (discouraged) -> keep behavior but disable credentials
            configuration.setAllowedOrigins(Arrays.asList("*"));
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOrigins(allowedOrigins);
            // When specific origins are set, credentials can be safely allowed
            configuration.setAllowCredentials(true);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}