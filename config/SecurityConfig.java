package com.company.compliance.config;

import com.company.compliance.security.JwtAuthenticationFilter;
import com.company.compliance.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the Compliance Platform.
 *
 * <p>Security posture:
 * <ul>
 *   <li>Stateless JWT — no HTTP session, no CSRF (REST API)</li>
 *   <li>CORS locked to configured origins</li>
 *   <li>Strict CSP, HSTS, X-Frame-Options, Referrer-Policy headers</li>
 *   <li>Method-level security via {@code @PreAuthorize} / {@code @RequiresRole}</li>
 *   <li>Rate limiting applied before authentication</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitFilter         rateLimitFilter;
    private final UserDetailsService      userDetailsService;
    private final AppProperties           appProperties;

    // ── Public endpoints ──────────────────────────────────────────
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // ── Filter chain ──────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (stateless REST API) ─────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ──────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session management (stateless) ────────────────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Security response headers ─────────────────────────
            .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))        // 1 year HSTS
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives(
                                    "default-src 'none'; "
                                    + "frame-ancestors 'none'; "
                                    + "form-action 'self'"))
                    .referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(contentType -> {})       // X-Content-Type-Options: nosniff
            )

            // ── Authorization rules ───────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Super-admin only
                    .requestMatchers("/api/v1/organizations/**").hasRole("SUPER_ADMIN")

                    // Compliance officers and above can manage policies
                    .requestMatchers(HttpMethod.POST,   "/api/v1/policies/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER")
                    .requestMatchers(HttpMethod.PUT,    "/api/v1/policies/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER")
                    .requestMatchers(HttpMethod.PATCH,  "/api/v1/policies/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/policies/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER")

                    // All authenticated users can read
                    .requestMatchers(HttpMethod.GET, "/api/v1/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER",
                                        "AUDITOR", "ANALYST", "API_CLIENT")

                    // Violation status updates require at least Auditor role
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/violations/**")
                            .hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER", "AUDITOR")

                    // All other write operations
                    .anyRequest().hasAnyRole("SUPER_ADMIN", "COMPLIANCE_OFFICER")
            )

            // ── Custom filters ────────────────────────────────────
            .addFilterBefore(rateLimitFilter,    UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter,      UsernamePasswordAuthenticationFilter.class)

            // ── Exception handling ────────────────────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":401,\"error\":\"UNAUTHORIZED\","
                                + "\"message\":\"Authentication required\"}");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":403,\"error\":\"FORBIDDEN\","
                                + "\"message\":\"You do not have permission to access this resource\"}");
                    })
            );

        return http.build();
    }

    // ── Beans ─────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt cost factor 12 — ~250ms on modern hardware (brute-force resistant)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(true); // prevent user enumeration
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        AppProperties.CorsProperties cfg = appProperties.getCors();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cfg.allowedOriginList());
        configuration.setAllowedMethods(cfg.allowedMethodList());
        configuration.setAllowedHeaders(List.of(cfg.getAllowedHeaders()));
        configuration.setAllowCredentials(cfg.isAllowCredentials());
        configuration.setMaxAge(cfg.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
