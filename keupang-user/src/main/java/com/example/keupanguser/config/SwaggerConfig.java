package com.example.keupanguser.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(@Value("${openapi.service.url}") String url){
        return new OpenAPI()
            .servers(List.of(new Server().url(url)))
            .info(new Info().title("User Service API")
                .version("v0.0.1"));
    }
}
