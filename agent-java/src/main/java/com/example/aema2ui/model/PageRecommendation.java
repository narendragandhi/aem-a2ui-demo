package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an AI-recommended page layout based on user input.
 * This is a key A2UI feature where the agent suggests optimal component arrangements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRecommendation {

    /**
     * Overall page type detected from user input
     */
    private String pageType;

    /**
     * Agent's reasoning for the recommendation
     */
    private String reasoning;

    /**
     * Ordered list of recommended sections
     */
    private List<SectionRecommendation> sections;

    /**
     * Confidence score (0-100)
     */
    private int confidence;

    /**
     * Alternative layouts the user might consider
     */
    private List<String> alternatives;

    /**
     * Represents a single section recommendation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionRecommendation {
        /**
         * Component type (hero, teaser, product, etc.)
         */
        private String componentType;

        /**
         * Display name for the UI
         */
        private String displayName;

        /**
         * Why this component is recommended here
         */
        private String reason;

        /**
         * Suggested prompt for content generation
         */
        private String suggestedPrompt;

        /**
         * Position in the page (1-based)
         */
        private int position;

        /**
         * Whether this section is required or optional
         */
        private boolean required;

        /**
         * Icon for display
         */
        private String icon;
    }
}
