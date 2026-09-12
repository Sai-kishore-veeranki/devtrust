package com.vsk.devtrust.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Was hardcoded to two localhost origins with no way to add a real
    // production domain without a code change and rebuild. Now driven by
    // config, defaulting to the same localhost values for local dev so
    // nothing breaks there — production sets DEVTRUST_CORS_ORIGINS instead.
    @Value("${devtrust.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                // PATCH was missing — IncidentController's resolve-incident
                // endpoint is a PATCH, so the dashboard's "Mark resolved"
                // button is silently blocked by CORS in a real browser
                // despite working fine in curl/Postman (which don't enforce it).
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
