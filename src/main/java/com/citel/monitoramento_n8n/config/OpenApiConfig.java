package com.citel.monitoramento_n8n.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI()
    {
        return new OpenAPI()
                .info(new Info().title("API Conecta").version("2.0.0").description("API Auxiliar do projeto Conecta"))
                // Sem isto o Swagger UI não tem botão Authorize e não envia o header Authorization,
                // o que impede testar qualquer endpoint autenticado pelo /doc.
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido pelo POST /Autenticar. Expira em 2h.")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }

}
