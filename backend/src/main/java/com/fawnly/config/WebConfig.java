package com.fawnly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsConfig corsConfig;

    public WebConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS handled via CorsConfigurationSource bean in SecurityConfig
    }
}
