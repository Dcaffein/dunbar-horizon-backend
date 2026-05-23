package com.example.DunbarHorizon.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String cookieName = "access_token";

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(cookieName);

        Components components = new Components()
                .addSecuritySchemes(cookieName, new SecurityScheme()
                        .name(cookieName)
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE));

        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Info apiInfo() {
        return new Info()
                .title("DunbarHorizon API 명세서")
                .description("DunbarHorizon 백엔드 API")
                .version("1.0.0");
    }
}