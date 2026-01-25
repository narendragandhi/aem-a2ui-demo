package com.example.aema2ui.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Web configuration for CORS and HTTP settings.
 *
 * Production configuration recommendations:
 * - Set cors.allowed-origins to specific domains (e.g., "https://your-domain.com")
 * - Never use "*" for allowed origins in production with credentials
 * - Set cors.allow-credentials=false unless specifically needed
 */
@Configuration
public class WebConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    // Default to localhost for development, should be overridden in production
    private static final String DEFAULT_ALLOWED_ORIGINS = "http://localhost:5173,http://localhost:3000";

    @Value("${cors.allowed-origins:" + DEFAULT_ALLOWED_ORIGINS + "}")
    private String allowedOrigins;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${security.strict-mode:false}")
    private boolean strictMode;

    @PostConstruct
    public void validateConfiguration() {
        List<String> origins = Arrays.asList(allowedOrigins.split(","));

        // Warn if using wildcard origin
        if (origins.contains("*")) {
            if (strictMode) {
                throw new IllegalStateException(
                    "CORS wildcard origin (*) is not allowed in strict mode. " +
                    "Set cors.allowed-origins to specific domains or disable security.strict-mode.");
            }
            logger.warn("⚠️  CORS is configured with wildcard origin (*). " +
                       "This is insecure for production. Set cors.allowed-origins to specific domains.");
        }

        // Warn if credentials enabled with wildcard
        if (allowCredentials && origins.contains("*")) {
            logger.warn("⚠️  CORS credentials are enabled with wildcard origin. " +
                       "This configuration will be rejected by browsers.");
        }

        logger.info("CORS configured for origins: {}", allowedOrigins);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);

                boolean hasWildcard = Arrays.asList(origins).contains("*");

                if (hasWildcard) {
                    // Use allowedOriginPatterns for wildcard to work with credentials
                    registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Request-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining")
                        .allowCredentials(allowCredentials)
                        .maxAge(maxAge);
                } else {
                    registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Request-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining")
                        .allowCredentials(allowCredentials)
                        .maxAge(maxAge);
                }
            }
        };
    }
}
