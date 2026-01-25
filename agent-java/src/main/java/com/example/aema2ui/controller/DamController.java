package com.example.aema2ui.controller;

import com.example.aema2ui.config.AemConfig;
import com.example.aema2ui.model.aem.DamAsset;
import com.example.aema2ui.service.aem.AemDamClient;
import com.example.aema2ui.service.aem.AemHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for DAM (Digital Asset Management) operations.
 * Provides endpoints for browsing, searching, and managing assets.
 */
@Slf4j
@RestController
@RequestMapping("/dam")
@RequiredArgsConstructor
public class DamController {

    private final AemDamClient damClient;
    private final AemHttpClient httpClient;
    private final AemConfig config;

    /**
     * Browse assets in a folder
     */
    @GetMapping("/browse")
    public ResponseEntity<?> browse(@RequestParam(defaultValue = "") String path) {
        if (!httpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected",
                "message", "Cannot browse DAM while AEM is disconnected"
            ));
        }

        try {
            String folderPath = path.isEmpty() ? config.getDamRoot() : path;
            List<DamAsset> assets = damClient.listAssets(folderPath);

            return ResponseEntity.ok(Map.of(
                "path", folderPath,
                "assets", assets,
                "count", assets.size()
            ));
        } catch (Exception e) {
            log.error("Failed to browse DAM: {}", path, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Browse failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Search assets
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String type) {

        if (!httpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected"
            ));
        }

        try {
            // Map type to MIME type prefix
            String mimeType = null;
            if (type != null) {
                switch (type.toLowerCase()) {
                    case "image" -> mimeType = "image/";
                    case "video" -> mimeType = "video/";
                    case "document" -> mimeType = "application/pdf";
                    case "audio" -> mimeType = "audio/";
                }
            }

            List<DamAsset> assets = damClient.searchAssets(q, mimeType);

            return ResponseEntity.ok(Map.of(
                "query", q,
                "type", type != null ? type : "all",
                "assets", assets,
                "count", assets.size()
            ));
        } catch (Exception e) {
            log.error("Failed to search DAM: {}", q, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Search failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get single asset details
     */
    @GetMapping("/asset/**")
    public ResponseEntity<?> getAsset(@RequestParam String path) {
        if (!httpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected"
            ));
        }

        try {
            DamAsset asset = damClient.getAsset(path);
            if (asset == null) {
                return ResponseEntity.notFound().build();
            }

            // Include renditions
            var renditions = damClient.getRenditions(path);

            return ResponseEntity.ok(Map.of(
                "asset", asset,
                "renditions", renditions
            ));
        } catch (Exception e) {
            log.error("Failed to get asset: {}", path, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get asset",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get folder tree for navigation
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getTree(@RequestParam(defaultValue = "") String root) {
        if (!httpClient.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "AEM not connected"
            ));
        }

        try {
            String folderPath = root.isEmpty() ? config.getDamRoot() : root;
            List<DamAsset> assets = damClient.listAssets(folderPath);

            // Filter to folders only
            List<DamAsset> folders = assets.stream()
                    .filter(DamAsset::isFolder)
                    .toList();

            return ResponseEntity.ok(Map.of(
                "root", folderPath,
                "folders", folders
            ));
        } catch (Exception e) {
            log.error("Failed to get folder tree", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get tree",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get DAM configuration
     */
    @GetMapping("/config")
    public ResponseEntity<?> getDamConfig() {
        return ResponseEntity.ok(Map.of(
            "damRoot", config.getDamRoot(),
            "authorUrl", config.getAuthorUrl(),
            "connected", httpClient.isConnected()
        ));
    }
}
