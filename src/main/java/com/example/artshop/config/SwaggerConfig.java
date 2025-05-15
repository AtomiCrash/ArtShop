package com.example.artshop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI artShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ArtShop API")
                        .description("API for managing artworks, artists and classifications")
                        .version("1.0")
                        .contact(new Contact()
                                .name("ArtShop Support")
                                .email("support@artshop.com")));
    }
}