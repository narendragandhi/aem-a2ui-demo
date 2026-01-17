package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a reviewer assigned to review content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reviewer {

    private String id;
    private String name;
    private String email;
    private String avatar;
    private ReviewerRole role;
    private boolean hasReviewed;

    public enum ReviewerRole {
        REVIEWER,
        APPROVER
    }
}
