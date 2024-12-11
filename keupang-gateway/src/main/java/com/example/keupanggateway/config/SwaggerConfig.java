package com.example.keupanggateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@OpenAPIDefinition
public class SwaggerConfig {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder
            .routes()
            // request.path 가 다음과 같으면 uri()로 이동
            .route(r -> r.path("/user/v3/swagger-ui.html").and().method(HttpMethod.GET).uri("lb://user"))
            .build();
    }
}
