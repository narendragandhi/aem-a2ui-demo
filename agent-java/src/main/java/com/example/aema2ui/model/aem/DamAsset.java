package com.example.aema2ui.model.aem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a DAM asset from AEM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamAsset {

    /**
     * Full JCR path to the asset
     */
    private String path;

    /**
     * Asset name (file name)
     */
    private String name;

    /**
     * Asset title from metadata
     */
    private String title;

    /**
     * Asset type (image, video, document, folder)
     */
    private String type;

    /**
     * MIME type (e.g., image/jpeg, video/mp4)
     */
    private String mimeType;

    /**
     * File size in bytes
     */
    private Long size;

    /**
     * Image width (for images)
     */
    private Integer width;

    /**
     * Image height (for images)
     */
    private Integer height;

    /**
     * URL to thumbnail rendition
     */
    private String thumbnailUrl;

    /**
     * URL to original asset
     */
    private String originalUrl;

    /**
     * Last modified timestamp
     */
    private String lastModified;

    /**
     * Created timestamp
     */
    private String created;

    /**
     * Creator user
     */
    private String createdBy;

    /**
     * Asset description
     */
    private String description;

    /**
     * Tags applied to asset
     */
    private List<String> tags;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Whether this is a folder
     */
    private boolean folder;

    /**
     * Child assets (if this is a folder)
     */
    private List<DamAsset> children;
}
