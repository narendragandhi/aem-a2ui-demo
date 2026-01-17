package com.example.aema2ui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AEM SDK integration.
 *
 * Supports connection to local AEM SDK for:
 * - Workflow API integration
 * - Content storage (pages, content fragments)
 * - DAM asset browsing and upload
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aem")
public class AemConfig {

    /**
     * Enable/disable real AEM integration.
     * When disabled, mock implementations are used.
     */
    private boolean enabled = true;

    /**
     * AEM Author instance URL (e.g., http://localhost:4502)
     */
    private String authorUrl = "http://localhost:4502";

    /**
     * AEM Publish instance URL (e.g., http://localhost:4503)
     */
    private String publishUrl = "http://localhost:4503";

    /**
     * AEM username for authentication
     */
    private String username = "admin";

    /**
     * AEM password for authentication
     */
    private String password = "admin";

    /**
     * Root path for content pages
     */
    private String contentRoot = "/content/aem-demo";

    /**
     * Root path for DAM assets
     */
    private String damRoot = "/content/dam/aem-demo";

    /**
     * Path to workflow models
     */
    private String workflowModelsPath = "/var/workflow/models";

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 30000;

    /**
     * Maximum retry attempts for failed requests
     */
    private int maxRetries = 3;

    /**
     * Delay between retry attempts in milliseconds
     */
    private int retryDelayMillis = 1000;
}
