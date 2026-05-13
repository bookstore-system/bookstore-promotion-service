package com.hamtech.bookstorepromotionservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI promotionServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookstore Promotion Service API")
                        .description("REST API khuyến mãi. Endpoint quản trị có thể gắn JWT (role ADMIN) tùy triển khai.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .name(BEARER_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

