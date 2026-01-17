package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a versioned snapshot of content.
 * Used for tracking content history during reviews.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersion {

    private String id;
    private String contentId;
    private int version;
    private ContentSuggestion content;
    private String createdBy;
    private Instant createdAt;
    private String changeNote;
}
