package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a comment on content during the review process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewComment {

    private String id;
    private String author;
    private String authorName;
    private String content;
    private String field;  // Which field the comment applies to (title, description, etc.)
    private Instant timestamp;
    private boolean resolved;
    private String resolvedBy;
    private Instant resolvedAt;
}
