package com.market.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
public class EcommerceApplication {
	private static final Logger log = LoggerFactory.getLogger(EcommerceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EcommerceApplication.class, args);
	}

	// Fail-fast for production, but allow dev/local to start with a dev fallback secret.
	@Bean
	public ApplicationRunner validateConfig(@Value("${jwt.secret:}") String jwtSecret, Environment env) {
		return args -> {
			boolean isDevLike = env != null && env.acceptsProfiles(Profiles.of("dev", "local"));

			// Log active profiles and doc URLs
			String[] active = env != null ? env.getActiveProfiles() : new String[0];
			log.info("Active Spring profiles: {}", (Object) active);
			if (isDevLike) {
				log.info("Swagger UI available at: /swagger-ui.html");
				log.info("OpenAPI JSON available at: /api-docs (or /v3/api-docs)");
			}

			if (isDevLike) {
				// In dev/local: allow a dev fallback secret but warn loudly
				if (jwtSecret == null || jwtSecret.isBlank()) {
					log.warn("Running with dev/local profile and no JWT_SECRET provided. Using insecure dev fallback secret. Do NOT use in production.");
					return;
				}
				if (jwtSecret.startsWith("CHANGE_THIS") || jwtSecret.length() < 32) {
					log.warn("JWT secret appears insecure for dev/local; continuing startup for development purposes.");
					return;
				}
				// Otherwise, jwtSecret provided looks ok - proceed
				return;
			}

			// Production and other profiles: enforce valid JWT secret
			if (jwtSecret == null || jwtSecret.isBlank()) {
				throw new IllegalStateException("JWT secret (JWT_SECRET) must be set as environment variable and must not be empty");
			}
			if (jwtSecret.startsWith("CHANGE_THIS") || jwtSecret.length() < 32) {
				throw new IllegalStateException("JWT secret appears to be insecure; set a sufficiently long random JWT_SECRET (32+ chars)");
			}
		};
	}

}
