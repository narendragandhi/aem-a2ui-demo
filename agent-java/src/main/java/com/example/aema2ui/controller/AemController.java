package com.example.aema2ui.controller;

import com.example.aema2ui.config.AemConfig;
import com.example.aema2ui.service.aem.AemContentClient;
import com.example.aema2ui.service.aem.AemHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for AEM integration endpoints.
 * Provides health check and content operations.
 */
@Slf4j
@RestController
@RequestMapping("/aem")
@RequiredArgsConstructor
public class AemController {

    private final AemHttpClient aemHttpClient;
    private final AemConfig aemConfig;
    private final AemContentClient aemContentClient;

    /**
     * Check AEM connection health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", aemConfig.isEnabled());
        status.put("authorUrl", aemConfig.getAuthorUrl());

        boolean connected = aemHttpClient.checkConnection();
        status.put("connected", connected);
        status.put("status", connected ? "CONNECTED" : "DISCONNECTED");

        if (!aemConfig.isEnabled()) {
            status.put("message", "AEM integration is disabled. Using mock mode.");
        } else if (!connected) {
            status.put("message", "Cannot connect to AEM at " + aemConfig.getAuthorUrl());
        } else {
            status.put("message", "Connected to AEM successfully");
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Save content to AEM as a page or content fragment
     */
    @PostMapping("/content")
    public ResponseEntity<Map<String, Object>> saveContent(@RequestBody Map<String, Object> request) {
        if (!aemHttpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected",
                "message", "Cannot save content while AEM is disconnected"
            ));
        }

        try {
            String contentType = (String) request.getOrDefault("type", "page");
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) request.get("properties");

            String path;
            if ("fragment".equals(contentType)) {
                String folder = (String) request.getOrDefault("folder", aemConfig.getDamRoot() + "/generated");
                String model = (String) request.getOrDefault("model", "/conf/aem-demo/settings/dam/cfm/models/generated-content");
                path = aemContentClient.createContentFragment(folder, name, model, properties);
            } else {
                String parentPath = (String) request.getOrDefault("parentPath", aemConfig.getContentRoot());
                String template = (String) request.getOrDefault("template", "/conf/aem-demo/settings/wcm/templates/content-page");
                path = aemContentClient.createPage(parentPath, name, template, properties);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "path", path,
                "aemUrl", aemConfig.getAuthorUrl() + "/editor.html" + path + ".html"
            ));
        } catch (Exception e) {
            log.error("Failed to save content to AEM", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Save failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get content from AEM
     */
    @GetMapping("/content/**")
    public ResponseEntity<Map<String, Object>> getContent(@RequestParam String path) {
        if (!aemHttpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected"
            ));
        }

        try {
            Map<String, Object> content = aemContentClient.getContent(path);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            log.error("Failed to get content from AEM: {}", path, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get content",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get AEM configuration (non-sensitive)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "enabled", aemConfig.isEnabled(),
            "authorUrl", aemConfig.getAuthorUrl(),
            "contentRoot", aemConfig.getContentRoot(),
            "damRoot", aemConfig.getDamRoot(),
            "connected", aemHttpClient.isConnected()
        ));
    }
}
