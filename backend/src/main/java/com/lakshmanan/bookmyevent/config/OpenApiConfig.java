package com.lakshmanan.bookmyevent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookMyEventOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("BookMyEvent API")
                .version("1.0.0")
                .description("REST API for browsing events and booking tickets. "
                        + "JWT-secured authentication with oversell-safe (pessimistic-locked) booking.")
                .contact(new Contact().name("Lakshmanan"))
                .license(new License().name("MIT")));
    }
}
