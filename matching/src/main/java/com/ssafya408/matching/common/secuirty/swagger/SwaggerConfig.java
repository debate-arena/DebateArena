package com.ssafya408.matching.common.secuirty.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server().url("http://localhost:8081").description("Local Server")
            ));
    }

    private Info apiInfo() {
        return new Info()
            .title("Matching Service API")
            .description("매칭 서비스 WebSocket 및 REST API 문서")
            .version("1.0.0")
            .contact(new Contact()
                .name("SSAFY A408")
                .email("contact@ssafy.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }
} 