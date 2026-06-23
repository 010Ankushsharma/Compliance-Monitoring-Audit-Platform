package com.company.compliance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 configuration.
 *
 * <p>Swagger UI available at: {@code /swagger-ui.html}
 * OpenAPI JSON spec at: {@code /api-docs}
 *
 * <p>All endpoints require Bearer JWT authentication by default.
 * The lock icon appears on every endpoint in Swagger UI.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${spring.application.name:compliance-audit-platform}")
    private String appName;

    @Bean
    public OpenAPI compliancePlatformOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerAuthScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Compliance Monitoring & Audit Platform API")
                .version("1.0.0")
                .description("""
                        ## Overview
                        Enterprise REST API for:
                        - **Policy Management** — Create and manage compliance policies for ISO 27001, SOC 2, GDPR, HIPAA, PCI DSS, and NIST CSF
                        - **Audit Trail** — Immutable, SHA-256 hash-chained audit log with tamper detection
                        - **Violation Detection** — Real-time and scheduled policy evaluation with risk scoring
                        - **Alerts** — Multi-channel notifications (email, Slack, webhook) with deduplication and escalation
                        - **Reports** — Async PDF/Excel/CSV report generation with pre-built regulatory templates

                        ## Authentication
                        All endpoints (except `/api/v1/auth/*`) require a valid Bearer JWT token.
                        Obtain a token via `POST /api/v1/auth/login`.

                        ## Roles
                        | Role | Permissions |
                        |---|---|
                        | `SUPER_ADMIN` | Full access including organisation management |
                        | `COMPLIANCE_OFFICER` | Create/edit policies, manage violations and reports |
                        | `AUDITOR` | Read all data, update violation status |
                        | `ANALYST` | Read access to all resources |
                        | `API_CLIENT` | Read-only programmatic access |
                        """)
                .contact(new Contact()
                        .name("Platform Engineering Team")
                        .email("platform@company.com")
                        .url("https://compliance.company.com"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> serverList() {
        return List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://compliance-api.company.com").description("Production")
        );
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter the JWT token obtained from POST /api/v1/auth/login");
    }
}
