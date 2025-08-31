package com.tuorg.fleetcare.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val bearerName = "bearer-jwt"
        return OpenAPI()
            .info(
                Info()
                    .title("FleetCare API")
                    .version("v1")
                    .description("APIs de FleetCare (auth, reportes, notificaciones).")
            )
            .components(
                Components().addSecuritySchemes(
                    bearerName, SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            // Requisito de seguridad global (se ignora en endpoints sin @SecurityRequirement)
            .addSecurityItem(SecurityRequirement().addList(bearerName))
    }
}