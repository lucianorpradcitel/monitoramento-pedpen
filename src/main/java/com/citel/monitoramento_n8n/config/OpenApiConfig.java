package com.citel.monitoramento_n8n.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI()
    {
        return new OpenAPI().info(new Info().title("API de Monitoramento de Pedidos do N8N").version("1.0.0").description("API para registrar e consultar o status  da integração de pedidos"));
    }

}
