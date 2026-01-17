package com.example.aema2ui.service.aem;

import com.example.aema2ui.config.AemConfig;
import com.example.aema2ui.model.aem.DamAsset;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Client for AEM DAM (Digital Asset Management) operations.
 * Provides asset browsing, searching, and upload capabilities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AemDamClient {

    private final AemHttpClient httpClient;
    private final AemConfig config;

    /**
     * List assets in a folder
     *
     * @param folderPath Path to the folder (e.g., /content/dam/aem-demo)
     * @return List of assets in the folder
     */
    public List<DamAsset> listAssets(String folderPath) {
        if (!httpClient.isConnected()) {
            log.warn("AEM not connected, returning empty asset list");
            return Collections.emptyList();
        }

        try {
            // Use Assets HTTP API
            String apiPath = "/api/assets" + folderPath + ".json";
            JsonNode response = httpClient.get(apiPath);

            List<DamAsset> assets = new ArrayList<>();

            // Parse entities (children)
            if (response.has("entities") && response.get("entities").isArray()) {
                for (JsonNode entity : response.get("entities")) {
                    DamAsset asset = parseAsset(entity);
                    if (asset != null) {
                        assets.add(asset);
                    }
                }
            }

            log.debug("Listed {} assets in {}", assets.size(), folderPath);
            return assets;
        } catch (Exception e) {
            log.error("Failed to list assets: {}", folderPath, e);
            return Collections.emptyList();
        }
    }

    /**
     * Search assets using QueryBuilder
     *
     * @param query    Search query (fulltext)
     * @param mimeType Optional MIME type filter (e.g., "image/")
     * @return List of matching assets
     */
    public List<DamAsset> searchAssets(String query, String mimeType) {
        if (!httpClient.isConnected()) {
            return Collections.emptyList();
        }

        try {
            StringBuilder queryPath = new StringBuilder("/bin/querybuilder.json?");
            queryPath.append("path=").append(config.getDamRoot());
            queryPath.append("&type=dam:Asset");
            queryPath.append("&p.limit=50");

            if (query != null && !query.isEmpty()) {
                queryPath.append("&fulltext=").append(query);
            }

            if (mimeType != null && !mimeType.isEmpty()) {
                queryPath.append("&property=jcr:content/metadata/dc:format");
                queryPath.append("&property.operation=like");
                queryPath.append("&property.value=").append(mimeType).append("%");
            }

            JsonNode response = httpClient.get(queryPath.toString());
            List<DamAsset> assets = new ArrayList<>();

            if (response.has("hits") && response.get("hits").isArray()) {
                for (JsonNode hit : response.get("hits")) {
                    String path = getTextValue(hit, "path", null);
                    if (path != null) {
                        // Fetch full asset details
                        DamAsset asset = getAsset(path);
                        if (asset != null) {
                            assets.add(asset);
                        }
                    }
                }
            }

            log.info("Found {} assets matching query: {}", assets.size(), query);
            return assets;
        } catch (Exception e) {
            log.error("Failed to search assets", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get a single asset's details
     *
     * @param assetPath Path to the asset
     * @return Asset details or null if not found
     */
    public DamAsset getAsset(String assetPath) {
        if (!httpClient.isConnected()) {
            return null;
        }

        try {
            String apiPath = "/api/assets" + assetPath + ".json";
            JsonNode response = httpClient.get(apiPath);
            return parseAssetDetails(response, assetPath);
        } catch (Exception e) {
            log.error("Failed to get asset: {}", assetPath, e);
            return null;
        }
    }

    /**
     * Get available renditions for an asset
     */
    public List<Map<String, String>> getRenditions(String assetPath) {
        if (!httpClient.isConnected()) {
            return Collections.emptyList();
        }

        try {
            JsonNode response = httpClient.get(assetPath + "/jcr:content/renditions.json");
            List<Map<String, String>> renditions = new ArrayList<>();

            Iterator<String> fieldNames = response.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (!name.startsWith("jcr:") && !name.startsWith("cq:")) {
                    renditions.add(Map.of(
                        "name", name,
                        "url", httpClient.getAuthorUrl() + assetPath + "/jcr:content/renditions/" + name
                    ));
                }
            }

            return renditions;
        } catch (Exception e) {
            log.error("Failed to get renditions: {}", assetPath, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse asset from Assets API entity
     */
    private DamAsset parseAsset(JsonNode entity) {
        try {
            JsonNode properties = entity.path("properties");

            String path = getTextValue(properties, "path", null);
            if (path == null) {
                path = getTextValue(entity, "href", null);
            }

            String name = getTextValue(properties, "name", "");
            String title = getTextValue(properties, "dc:title", name);

            // Determine type
            String classType = "";
            if (entity.has("class") && entity.get("class").isArray()) {
                for (JsonNode cls : entity.get("class")) {
                    classType = cls.asText();
                    break;
                }
            }

            boolean isFolder = "assets/folder".equals(classType);
            String type = isFolder ? "folder" : determineAssetType(properties);

            return DamAsset.builder()
                .path(path != null ? path.replace("/api/assets", "") : null)
                .name(name)
                .title(title)
                .type(type)
                .folder(isFolder)
                .mimeType(getTextValue(properties, "dc:format", null))
                .thumbnailUrl(buildThumbnailUrl(path))
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse asset", e);
            return null;
        }
    }

    /**
     * Parse full asset details from API response
     */
    private DamAsset parseAssetDetails(JsonNode response, String path) {
        try {
            JsonNode properties = response.path("properties");
            JsonNode metadata = properties.path("metadata");

            String name = getTextValue(properties, "name", path.substring(path.lastIndexOf('/') + 1));
            String title = getTextValue(metadata, "dc:title", name);
            String mimeType = getTextValue(metadata, "dc:format", null);

            return DamAsset.builder()
                .path(path)
                .name(name)
                .title(title)
                .type(determineAssetType(metadata))
                .mimeType(mimeType)
                .width(getIntValue(metadata, "tiff:ImageWidth", null))
                .height(getIntValue(metadata, "tiff:ImageLength", null))
                .size(getLongValue(properties, "size", null))
                .description(getTextValue(metadata, "dc:description", null))
                .created(getTextValue(properties, "jcr:created", null))
                .lastModified(getTextValue(properties, "jcr:lastModified", null))
                .createdBy(getTextValue(properties, "jcr:createdBy", null))
                .thumbnailUrl(buildThumbnailUrl(path))
                .originalUrl(httpClient.getAuthorUrl() + path)
                .folder(false)
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse asset details", e);
            return null;
        }
    }

    /**
     * Determine asset type from metadata
     */
    private String determineAssetType(JsonNode metadata) {
        String format = getTextValue(metadata, "dc:format", "");
        if (format.startsWith("image/")) return "image";
        if (format.startsWith("video/")) return "video";
        if (format.startsWith("audio/")) return "audio";
        if (format.contains("pdf")) return "document";
        if (format.contains("word") || format.contains("document")) return "document";
        if (format.contains("excel") || format.contains("spreadsheet")) return "spreadsheet";
        return "file";
    }

    /**
     * Build thumbnail URL for an asset
     */
    private String buildThumbnailUrl(String path) {
        if (path == null) return null;
        String cleanPath = path.replace("/api/assets", "");
        return httpClient.getAuthorUrl() + cleanPath + "/jcr:content/renditions/cq5dam.thumbnail.319.319.png";
    }

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field)) return defaultValue;
        JsonNode value = node.get(field);
        return value.isTextual() ? value.asText() : defaultValue;
    }

    private Integer getIntValue(JsonNode node, String field, Integer defaultValue) {
        if (node == null || !node.has(field)) return defaultValue;
        JsonNode value = node.get(field);
        return value.isNumber() ? value.asInt() : defaultValue;
    }

    private Long getLongValue(JsonNode node, String field, Long defaultValue) {
        if (node == null || !node.has(field)) return defaultValue;
        JsonNode value = node.get(field);
        return value.isNumber() ? value.asLong() : defaultValue;
    }
}
