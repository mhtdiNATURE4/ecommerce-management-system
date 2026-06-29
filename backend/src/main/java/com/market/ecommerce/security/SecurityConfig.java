package com.market.ecommerce.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor // تقوم بتوليد الـ Constructor تلقائياً للحقن (Dependency Injection)
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;
        private final Environment env;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    // إضافة مهمة جداً: ستستخدم هذا الـ Bean في AuthService للتحقق من الإيميل وكلمة المرور عند الـ Login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                        boolean swaggerAllowed = env != null && env.acceptsProfiles(Profiles.of("dev", "local"));

                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        auth.requestMatchers("/api/auth/**").permitAll();
                        auth.requestMatchers("/api/health").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll();

                        if (swaggerAllowed) {
                            auth.requestMatchers(
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/api-docs/**",
                                    "/v3/api-docs/**"
                            ).permitAll();
                        }

                        auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                        auth.anyRequest().authenticated();
                })
                .headers(headers -> {
                    boolean isProd = env != null && env.acceptsProfiles(Profiles.of("prod"));

                    // HSTS - enable only in prod (assuming TLS termination exists)
                    if (isProd) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)); // 1 year
                    }

                    // X-Content-Type-Options: nosniff
                    headers.contentTypeOptions(Customizer.withDefaults());

                    // X-Frame-Options: DENY
                    headers.frameOptions(frame -> frame.deny());

                    // Referrer-Policy
                    headers.referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

                    // Content-Security-Policy: strict in prod, relaxed in dev/local to allow Swagger UI resources
                    String cspProd = "default-src 'self'; img-src 'self' data:; script-src 'self'; style-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self';";
                    String cspDev = "default-src 'self' 'unsafe-inline' data: blob:; img-src 'self' data: blob:; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';";

                    headers.contentSecurityPolicy(isProd ? cspProd : cspDev);
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
