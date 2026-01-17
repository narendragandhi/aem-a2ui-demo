package com.example.aema2ui.service.aem;

import com.example.aema2ui.config.AemConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for AEM Content operations.
 * Supports creating pages, content fragments, and managing content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AemContentClient {

    private final AemHttpClient httpClient;
    private final AemConfig config;

    /**
     * Create a new page in AEM
     *
     * @param parentPath Parent path (e.g., /content/aem-demo)
     * @param pageName   Page name (will be slugified)
     * @param template   Template path
     * @param properties Page properties
     * @return The created page path
     */
    public String createPage(String parentPath, String pageName, String template, Map<String, Object> properties) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            String safeName = slugify(pageName);
            String pagePath = parentPath + "/" + safeName;

            // Create page using Sling POST servlet
            Map<String, String> formData = new HashMap<>();
            formData.put("./jcr:primaryType", "cq:Page");
            formData.put("./jcr:content/jcr:primaryType", "cq:PageContent");
            formData.put("./jcr:content/jcr:title", pageName);
            formData.put("./jcr:content/cq:template", template);
            formData.put("./jcr:content/sling:resourceType", "aem-demo/components/page");

            // Add custom properties
            if (properties != null) {
                properties.forEach((key, value) -> {
                    if (value != null) {
                        formData.put("./jcr:content/" + key, value.toString());
                    }
                });
            }

            httpClient.postForm(pagePath, formData);
            log.info("Created page: {}", pagePath);

            return pagePath;
        } catch (Exception e) {
            log.error("Failed to create page: {}/{}", parentPath, pageName, e);
            throw new RuntimeException("Failed to create page: " + e.getMessage(), e);
        }
    }

    /**
     * Create a content fragment in AEM DAM
     *
     * @param folderPath Folder path in DAM
     * @param name       Fragment name
     * @param modelPath  Content Fragment Model path
     * @param content    Fragment content/elements
     * @return The created fragment path
     */
    public String createContentFragment(String folderPath, String name, String modelPath, Map<String, Object> content) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            String safeName = slugify(name);
            String fragmentPath = folderPath + "/" + safeName;

            // Ensure folder exists
            ensureFolderExists(folderPath);

            // Create content fragment via Assets HTTP API
            Map<String, String> formData = new HashMap<>();
            formData.put("./jcr:primaryType", "dam:Asset");
            formData.put("./jcr:content/jcr:primaryType", "dam:AssetContent");
            formData.put("./jcr:content/data/cq:model", modelPath);
            formData.put("./jcr:content/data/jcr:primaryType", "nt:unstructured");

            // Map content to fragment elements
            if (content != null) {
                content.forEach((key, value) -> {
                    if (value != null) {
                        formData.put("./jcr:content/data/master/" + key, value.toString());
                    }
                });
            }

            // Add metadata
            formData.put("./jcr:content/metadata/dc:title", name);
            formData.put("./jcr:content/metadata/generatedAt", Instant.now().toString());
            formData.put("./jcr:content/metadata/generatedBy", "aem-a2ui-demo");

            httpClient.postForm(fragmentPath, formData);
            log.info("Created content fragment: {}", fragmentPath);

            return fragmentPath;
        } catch (Exception e) {
            log.error("Failed to create content fragment: {}/{}", folderPath, name, e);
            throw new RuntimeException("Failed to create content fragment: " + e.getMessage(), e);
        }
    }

    /**
     * Get content from AEM as a Map
     */
    public Map<String, Object> getContent(String path) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            return httpClient.getAsMap(path + ".json");
        } catch (Exception e) {
            log.error("Failed to get content: {}", path, e);
            throw new RuntimeException("Failed to get content: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing content
     */
    public void updateContent(String path, Map<String, Object> properties) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            Map<String, String> formData = new HashMap<>();
            properties.forEach((key, value) -> {
                if (value != null) {
                    formData.put("./" + key, value.toString());
                }
            });

            httpClient.postForm(path, formData);
            log.info("Updated content: {}", path);
        } catch (Exception e) {
            log.error("Failed to update content: {}", path, e);
            throw new RuntimeException("Failed to update content: " + e.getMessage(), e);
        }
    }

    /**
     * Delete content from AEM
     */
    public void deleteContent(String path) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            Map<String, String> formData = new HashMap<>();
            formData.put(":operation", "delete");

            httpClient.postForm(path, formData);
            log.info("Deleted content: {}", path);
        } catch (Exception e) {
            log.error("Failed to delete content: {}", path, e);
            throw new RuntimeException("Failed to delete content: " + e.getMessage(), e);
        }
    }

    /**
     * Ensure a folder exists in DAM, creating if necessary
     */
    private void ensureFolderExists(String folderPath) {
        try {
            // Check if folder exists
            httpClient.get(folderPath + ".json");
        } catch (Exception e) {
            // Folder doesn't exist, create it
            try {
                Map<String, String> formData = new HashMap<>();
                formData.put("./jcr:primaryType", "sling:OrderedFolder");
                formData.put("./jcr:content/jcr:primaryType", "nt:unstructured");

                httpClient.postForm(folderPath, formData);
                log.info("Created folder: {}", folderPath);
            } catch (Exception createError) {
                log.warn("Could not create folder: {}", folderPath);
            }
        }
    }

    /**
     * Convert a name to a URL-safe slug
     */
    private String slugify(String input) {
        if (input == null || input.isEmpty()) {
            return "content-" + UUID.randomUUID().toString().substring(0, 8);
        }

        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
