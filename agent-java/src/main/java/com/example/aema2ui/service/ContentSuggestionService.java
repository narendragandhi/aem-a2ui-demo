package com.example.aema2ui.service;

import com.example.aema2ui.agent.AemContentAgent;
import com.example.aema2ui.model.ContentSuggestion;
import com.example.aema2ui.model.UserInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service that generates content suggestions and A2UI responses.
 * Uses the AemContentAgent for content generation, with AI integration ready for future use.
 */
@Slf4j
@Service
public class ContentSuggestionService {

    private final A2UIMessageBuilder builder;
    private final AemContentAgent contentAgent;

    @Value("${aem.agent.ai.enabled:false}")
    private boolean aiEnabled;

    @Autowired
    public ContentSuggestionService(A2UIMessageBuilder builder, AemContentAgent contentAgent) {
        this.builder = builder;
        this.contentAgent = contentAgent;
    }

    /**
     * Generates A2UI messages based on user input.
     */
    public List<Map<String, Object>> generateSuggestion(String userInput) {
        String surfaceId = "suggestion_" + UUID.randomUUID().toString().substring(0, 8);

        // Generate content using the agent
        ContentSuggestion suggestion = generateContent(userInput);

        // Build A2UI messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. Begin Rendering
        messages.add(builder.beginRendering(surfaceId, "root"));

        // 2. Surface Update with components
        messages.add(builder.surfaceUpdate(surfaceId, buildComponents(suggestion)));

        // 3. Data Model Update
        messages.add(builder.dataModelUpdate(surfaceId, "suggestion", buildDataModel(suggestion)));

        return messages;
    }

    /**
     * Generate content using the AemContentAgent.
     */
    private ContentSuggestion generateContent(String userInput) {
        if (aiEnabled) {
            log.info("AI mode enabled - using AemContentAgent (template-based, AI ready)");
        }

        // Parse user intent and generate content
        UserInput parsed = contentAgent.parseUserIntent(userInput);
        return contentAgent.generateContent(parsed);
    }

    /**
     * Builds the component tree for the suggestion UI.
     */
    private List<Map<String, Object>> buildComponents(ContentSuggestion suggestion) {
        List<Map<String, Object>> components = new ArrayList<>();

        // Root column
        components.add(builder.column("root", List.of("header", "preview", "form", "actions")));

        // Header with component type
        String headerText = "Content Suggestion" +
            (suggestion.getComponentType() != null ? " (" + suggestion.getComponentType() + ")" : "");
        components.add(builder.text("header", headerText, "h2"));

        // Image preview
        components.add(builder.image("preview", "/suggestion/imageUrl", "Preview image"));

        // Form column
        List<String> formFields = new ArrayList<>(List.of("title_field", "subtitle_field", "desc_field"));
        if (suggestion.getPrice() != null) {
            formFields.add("price_field");
        }
        formFields.add("cta_field");
        components.add(builder.column("form", formFields));

        // Title field
        components.add(builder.textField("title_field", "Title", "/suggestion/title", null));

        // Subtitle field
        components.add(builder.textField("subtitle_field", "Subtitle", "/suggestion/subtitle", null));

        // Description field
        components.add(builder.textField("desc_field", "Description", "/suggestion/description", 3));

        // Price field (optional)
        if (suggestion.getPrice() != null) {
            components.add(builder.textField("price_field", "Price", "/suggestion/price", null));
        }

        // CTA field
        components.add(builder.textField("cta_field", "Button Text", "/suggestion/ctaText", null));

        // Actions row
        components.add(builder.row("actions", List.of("apply_btn", "regenerate_btn")));

        // Apply button
        components.add(builder.button("apply_btn", "Apply to Component", "apply_suggestion"));

        // Regenerate button
        components.add(builder.button("regenerate_btn", "Try Again", "regenerate"));

        return components;
    }

    /**
     * Builds the data model for the suggestion.
     */
    private List<Map<String, Object>> buildDataModel(ContentSuggestion suggestion) {
        List<Map<String, Object>> data = new ArrayList<>();

        data.add(builder.dataString("title", suggestion.getTitle()));
        data.add(builder.dataString("subtitle", suggestion.getSubtitle() != null ? suggestion.getSubtitle() : ""));
        data.add(builder.dataString("description", suggestion.getDescription()));
        data.add(builder.dataString("imageUrl", suggestion.getImageUrl()));
        data.add(builder.dataString("ctaText", suggestion.getCtaText() != null ? suggestion.getCtaText() : "Learn More"));
        data.add(builder.dataString("ctaUrl", suggestion.getCtaUrl() != null ? suggestion.getCtaUrl() : "#"));

        if (suggestion.getPrice() != null) {
            data.add(builder.dataString("price", suggestion.getPrice()));
        }

        return data;
    }
}
