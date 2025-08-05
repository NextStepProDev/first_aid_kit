package com.firstaid.infrastructure.configuration;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SuppressWarnings("unused")
public class CorsConfig {
    /**
     * Configures CORS settings for the application.
     * Allows all origins, methods, and headers, and supports credentials.
     *
     * @return a WebMvcConfigurer that applies the CORS configuration.
     * <p>
     * This configuration was added to allow the frontend (e.g., Swagger UI or any deployed client) to communicate
     * with the backend without CORS errors during development and deployment.
     */

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}