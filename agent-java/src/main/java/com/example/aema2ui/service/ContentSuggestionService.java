package com.example.aema2ui.service;

import com.example.aema2ui.agent.AemContentAgent;
import com.example.aema2ui.model.ContentSuggestion;
import com.example.aema2ui.model.UserInput;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Value("${aem.agent.ai.enabled:false}")
    private boolean aiEnabled;

    @Autowired
    public ContentSuggestionService(A2UIMessageBuilder builder, AemContentAgent contentAgent, ObjectMapper objectMapper) {
        this.builder = builder;
        this.contentAgent = contentAgent;
        this.objectMapper = objectMapper;
    }

    /**
     * Result record for multiple suggestions.
     */
    public record SuggestionsResult(List<Map<String, Object>> messages, List<Map<String, Object>> artifacts) {}

    /**
     * Generates multiple content suggestions with variations.
     */
    public SuggestionsResult generateMultipleSuggestions(String userInput, int count) {
        List<Map<String, Object>> messages = new ArrayList<>();
        List<Map<String, Object>> artifacts = new ArrayList<>();

        // Parse user intent once
        UserInput parsed = contentAgent.parseUserIntent(userInput);
        String componentType = parsed.getDetectedComponentType();

        // Generate multiple variations
        List<ContentSuggestion> suggestions = generateVariations(parsed, count);

        // Create artifacts with the suggestions data for client consumption
        for (int i = 0; i < suggestions.size(); i++) {
            ContentSuggestion suggestion = suggestions.get(i);
            artifacts.add(createArtifact(suggestion, i + 1));
        }

        // Also include A2UI messages for the first suggestion
        if (!suggestions.isEmpty()) {
            String surfaceId = "suggestion_" + UUID.randomUUID().toString().substring(0, 8);
            messages.add(builder.beginRendering(surfaceId, "root"));
            messages.add(builder.surfaceUpdate(surfaceId, buildComponents(suggestions.get(0))));
            messages.add(builder.dataModelUpdate(surfaceId, "suggestion", buildDataModel(suggestions.get(0))));
        }

        return new SuggestionsResult(messages, artifacts);
    }

    /**
     * Generate multiple variations of content.
     */
    private List<ContentSuggestion> generateVariations(UserInput parsed, int count) {
        List<ContentSuggestion> variations = new ArrayList<>();

        // First variation from the agent
        variations.add(contentAgent.generateContent(parsed));

        // Generate additional variations with different styles
        String[] styles = {"bold and impactful", "friendly and conversational", "professional and elegant"};

        for (int i = 1; i < count && i < styles.length; i++) {
            UserInput variantInput = UserInput.builder()
                .rawText(parsed.getRawText() + ". Style: " + styles[i])
                .detectedComponentType(parsed.getDetectedComponentType())
                .targetAudience(parsed.getTargetAudience())
                .brandStyle(styles[i])
                .toneOfVoice(parsed.getToneOfVoice())
                .build();

            variations.add(contentAgent.generateContent(variantInput));
        }

        return variations;
    }

    /**
     * Create an artifact containing the suggestion data.
     */
    private Map<String, Object> createArtifact(ContentSuggestion suggestion, int index) {
        try {
            String json = objectMapper.writeValueAsString(suggestion);
            return Map.of(
                "index", index,
                "name", "suggestion_" + index,
                "parts", List.of(Map.of(
                    "type", "application/json",
                    "data", json
                ))
            );
        } catch (Exception e) {
            log.error("Failed to serialize suggestion", e);
            return Map.of("error", e.getMessage());
        }
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
