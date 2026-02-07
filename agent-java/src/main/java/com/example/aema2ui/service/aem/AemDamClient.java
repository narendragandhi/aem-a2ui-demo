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
     * List assets in a folder using Sling JSON export.
     * Works on all AEM versions without additional configuration.
     *
     * @param folderPath Path to the folder (e.g., /content/dam/wknd)
     * @return List of assets in the folder
     */
    public List<DamAsset> listAssets(String folderPath) {
        if (!httpClient.isConnected()) {
            log.warn("AEM not connected, returning empty asset list");
            return Collections.emptyList();
        }

        try {
            // Use Sling JSON export with depth 1 (immediate children only)
            String apiPath = folderPath + ".1.json";
            JsonNode response = httpClient.get(apiPath);

            List<DamAsset> assets = new ArrayList<>();

            // Iterate through all child nodes
            Iterator<String> fieldNames = response.fieldNames();
            while (fieldNames.hasNext()) {
                String childName = fieldNames.next();
                // Skip JCR system properties
                if (childName.startsWith("jcr:") || childName.startsWith("rep:")) {
                    continue;
                }

                JsonNode childNode = response.get(childName);
                if (childNode.isObject()) {
                    DamAsset asset = parseSlingAsset(childNode, folderPath + "/" + childName, childName);
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
     * Get a single asset's details using Sling JSON export.
     *
     * @param assetPath Path to the asset
     * @return Asset details or null if not found
     */
    public DamAsset getAsset(String assetPath) {
        if (!httpClient.isConnected()) {
            return null;
        }

        try {
            // Use Sling JSON export with depth 2 for metadata access
            String apiPath = assetPath + ".2.json";
            JsonNode response = httpClient.get(apiPath);
            String name = assetPath.substring(assetPath.lastIndexOf('/') + 1);
            return parseSlingAssetDetails(response, assetPath, name);
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
     * Parse asset from Sling JSON export (folder listing).
     */
    private DamAsset parseSlingAsset(JsonNode node, String path, String name) {
        try {
            String primaryType = getTextValue(node, "jcr:primaryType", "");

            // Check if it's a folder or asset
            boolean isFolder = "sling:Folder".equals(primaryType) ||
                               "sling:OrderedFolder".equals(primaryType) ||
                               "nt:folder".equals(primaryType);
            boolean isAsset = "dam:Asset".equals(primaryType);

            if (!isFolder && !isAsset) {
                return null; // Skip non-DAM nodes
            }

            String title = name;
            String mimeType = null;
            String type = isFolder ? "folder" : "file";

            // For assets, try to get metadata from jcr:content/metadata
            if (isAsset && node.has("jcr:content")) {
                JsonNode jcrContent = node.get("jcr:content");
                if (jcrContent.has("metadata")) {
                    JsonNode metadata = jcrContent.get("metadata");
                    title = getTextValue(metadata, "dc:title", name);
                    mimeType = getTextValue(metadata, "dc:format", null);
                    type = determineAssetType(metadata);
                }
            }

            return DamAsset.builder()
                .path(path)
                .name(name)
                .title(title)
                .type(type)
                .folder(isFolder)
                .mimeType(mimeType)
                .thumbnailUrl(isAsset ? buildThumbnailUrl(path) : null)
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse asset: {}", path, e);
            return null;
        }
    }

    /**
     * Parse full asset details from Sling JSON export.
     */
    private DamAsset parseSlingAssetDetails(JsonNode response, String path, String name) {
        try {
            String primaryType = getTextValue(response, "jcr:primaryType", "");
            boolean isFolder = "sling:Folder".equals(primaryType) ||
                               "sling:OrderedFolder".equals(primaryType);

            if (isFolder) {
                return DamAsset.builder()
                    .path(path)
                    .name(name)
                    .title(name)
                    .type("folder")
                    .folder(true)
                    .build();
            }

            // Get metadata from jcr:content/metadata
            JsonNode jcrContent = response.path("jcr:content");
            JsonNode metadata = jcrContent.path("metadata");

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
                .description(getTextValue(metadata, "dc:description", null))
                .created(getTextValue(response, "jcr:created", null))
                .lastModified(getTextValue(jcrContent, "jcr:lastModified", null))
                .createdBy(getTextValue(response, "jcr:createdBy", null))
                .thumbnailUrl(buildThumbnailUrl(path))
                .originalUrl(buildOriginalUrl(path))
                .folder(false)
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse asset details: {}", path, e);
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
     * Build thumbnail URL for an asset.
     * Uses the proxy endpoint to avoid CORS issues and handle authentication.
     */
    private String buildThumbnailUrl(String path) {
        if (path == null) return null;
        // Use proxy endpoint to avoid CORS issues - the frontend will call this
        // URL format: /dam/proxy?path=/content/dam/...&rendition=cq5dam.thumbnail.319.319.png
        try {
            return "/dam/proxy?path=" + java.net.URLEncoder.encode(path, "UTF-8") + "&rendition=cq5dam.thumbnail.319.319.png";
        } catch (java.io.UnsupportedEncodingException e) {
            return "/dam/proxy?path=" + path + "&rendition=cq5dam.thumbnail.319.319.png";
        }
    }

    /**
     * Build original asset URL using proxy
     */
    private String buildOriginalUrl(String path) {
        if (path == null) return null;
        try {
            return "/dam/proxy?path=" + java.net.URLEncoder.encode(path, "UTF-8") + "&rendition=original";
        } catch (java.io.UnsupportedEncodingException e) {
            return "/dam/proxy?path=" + path + "&rendition=original";
        }
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
