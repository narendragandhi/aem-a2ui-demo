package com.example.aema2ui.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service that generates content suggestions and A2UI responses.
 */
@Service
@RequiredArgsConstructor
public class ContentSuggestionService {

    private final A2UIMessageBuilder builder;

    /**
     * Generates A2UI messages based on user input.
     */
    public List<Map<String, Object>> generateSuggestion(String userInput) {
        String surfaceId = "suggestion_" + UUID.randomUUID().toString().substring(0, 8);

        // Generate content based on input keywords
        ContentSuggestion suggestion = generateContentFromInput(userInput);

        // Build A2UI messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. Begin Rendering
        messages.add(builder.beginRendering(surfaceId, "root"));

        // 2. Surface Update with components
        messages.add(builder.surfaceUpdate(surfaceId, buildComponents()));

        // 3. Data Model Update
        messages.add(builder.dataModelUpdate(surfaceId, "suggestion", List.of(
            builder.dataString("title", suggestion.title),
            builder.dataString("description", suggestion.description),
            builder.dataString("imageUrl", suggestion.imageUrl)
        )));

        return messages;
    }

    /**
     * Builds the component tree for the suggestion UI.
     */
    private List<Map<String, Object>> buildComponents() {
        List<Map<String, Object>> components = new ArrayList<>();

        // Root column
        components.add(builder.column("root", List.of("header", "preview", "form", "actions")));

        // Header
        components.add(builder.text("header", "Content Suggestion", "h2"));

        // Image preview
        components.add(builder.image("preview", "/suggestion/imageUrl", "Preview image"));

        // Form column
        components.add(builder.column("form", List.of("title_field", "desc_field")));

        // Title field
        components.add(builder.textField("title_field", "Title", "/suggestion/title", null));

        // Description field
        components.add(builder.textField("desc_field", "Description", "/suggestion/description", 3));

        // Actions row
        components.add(builder.row("actions", List.of("apply_btn", "regenerate_btn")));

        // Apply button
        components.add(builder.button("apply_btn", "Apply to Component", "apply_suggestion"));

        // Regenerate button
        components.add(builder.button("regenerate_btn", "Try Again", "regenerate"));

        return components;
    }

    /**
     * Generates content suggestion based on user input.
     */
    private ContentSuggestion generateContentFromInput(String userInput) {
        String input = userInput != null ? userInput.toLowerCase() : "";

        if (input.contains("hero")) {
            return new ContentSuggestion(
                "Unleash Your Potential",
                "Discover innovative solutions designed to transform your digital experience.",
                "https://picsum.photos/800/400"
            );
        } else if (input.contains("product")) {
            return new ContentSuggestion(
                "Premium Quality Products",
                "Crafted with precision and care for the discerning customer.",
                "https://picsum.photos/800/400?random=2"
            );
        } else if (input.contains("teaser")) {
            return new ContentSuggestion(
                "Explore What's New",
                "Stay ahead of the curve with our latest innovations and offerings.",
                "https://picsum.photos/800/400?random=3"
            );
        } else if (input.contains("banner")) {
            return new ContentSuggestion(
                "Special Offer Inside",
                "Don't miss out on exclusive deals available for a limited time only.",
                "https://picsum.photos/800/400?random=4"
            );
        } else {
            return new ContentSuggestion(
                "Welcome to Our World",
                "Experience excellence in everything we do. Your journey starts here.",
                "https://picsum.photos/800/400?random=5"
            );
        }
    }

    /**
     * Simple record for content suggestion data.
     */
    private record ContentSuggestion(String title, String description, String imageUrl) {}
}
