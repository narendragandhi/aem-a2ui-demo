package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a review process for content.
 * Tracks reviewers, comments, and approval status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private String id;
    private String contentId;
    private ContentSuggestion content;
    private ReviewStatus status;

    @Builder.Default
    private List<Reviewer> reviewers = new ArrayList<>();

    @Builder.Default
    private List<ReviewComment> comments = new ArrayList<>();

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private String approvedBy;
    private Instant approvedAt;
    private String rejectedBy;
    private Instant rejectedAt;
    private String rejectionReason;

    private int version;
}
