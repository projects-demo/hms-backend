package com.hms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hmsOpenApi() {
        final String bearerScheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("HMS Backend API")
                        .description("""
                                Multi-tenant Hospital Management System API.

                                **How to test in Swagger:**
                                1. Onboard a hospital: `POST /api/v1/platform/tenants` (requires a platform-admin token - seed one directly in hms_master.platform_users for local dev, see README).
                                2. Login as that hospital's admin: `POST /api/v1/auth/login` with the `tenantCode` you chose, plus the `adminUsername`/`adminPassword` you set during onboarding.
                                3. Click **Authorize** below and paste the returned `accessToken` as a Bearer token.
                                4. All subsequent calls are automatically scoped to that hospital's schema - you cannot see another hospital's data with this token.
                                """)
                        .version("v0.1.0")
                        .contact(new Contact().name("HMS Engineering")))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components().addSecuritySchemes(bearerScheme,
                        new SecurityScheme()
                                .name(bearerScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
