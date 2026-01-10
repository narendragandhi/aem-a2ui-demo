package com.example.aema2ui.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.example.aema2ui.model.ContentSuggestion;
import com.example.aema2ui.model.UserInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Embabel Agent for generating AEM content suggestions.
 *
 * This agent uses Embabel annotations for future AI/LLM integration.
 * Currently provides template-based content generation as fallback.
 *
 * When full Embabel runtime is configured with API keys, the LLM prompts
 * will be used for intelligent content generation.
 */
@Slf4j
@Component
@Agent(description = "AI agent that generates content suggestions for AEM components")
public class AemContentAgent {

    // Prompts for LLM integration
    public static final String PARSE_INPUT_PROMPT = """
        Analyze the following user request for AEM content creation and extract:
        - The type of component they want (hero, product, teaser, banner, or general)
        - Any target audience mentioned
        - Any brand or style preferences
        - The desired tone of voice

        User request: %s
        """;

    public static final String GENERATE_CONTENT_PROMPT = """
        Generate compelling marketing content for an AEM %s component.

        Context:
        - Target audience: %s
        - Brand style: %s
        - Tone of voice: %s
        - Original request: %s

        Create professional, engaging content that:
        1. Has a strong headline that captures attention
        2. Includes a clear value proposition
        3. Has a compelling call-to-action
        4. Uses appropriate imagery suggestions
        5. Follows best practices for web content

        Make the content feel authentic and persuasive.
        """;

    /**
     * Parse user input to understand their intent.
     * Uses keyword matching, ready for LLM upgrade when runtime is configured.
     */
    @Action
    public UserInput parseUserIntent(String rawInput) {
        log.info("Parsing user intent from: {}", rawInput);

        String input = rawInput != null ? rawInput.toLowerCase() : "";

        UserInput parsed = UserInput.builder()
            .rawText(rawInput)
            .detectedComponentType(detectComponentType(input))
            .targetAudience("general audience")
            .brandStyle("professional and modern")
            .toneOfVoice("professional yet approachable")
            .build();

        log.info("Detected component type: {}", parsed.getDetectedComponentType());
        return parsed;
    }

    /**
     * Generate content suggestion based on parsed user input.
     * Uses templates, ready for LLM upgrade when runtime is configured.
     */
    @AchievesGoal(description = "Generate content suggestion for AEM component")
    @Action
    public ContentSuggestion generateContent(UserInput input) {
        log.info("Generating content for component type: {}", input.getDetectedComponentType());

        String componentType = input.getDetectedComponentType() != null
            ? input.getDetectedComponentType()
            : "general";

        ContentSuggestion suggestion = createTemplateSuggestion(componentType, input.getRawText());
        suggestion.setComponentType(componentType);

        log.info("Generated content suggestion: {}", suggestion.getTitle());
        return suggestion;
    }

    private String detectComponentType(String input) {
        if (input.contains("hero")) return "hero";
        if (input.contains("product")) return "product";
        if (input.contains("teaser")) return "teaser";
        if (input.contains("banner")) return "banner";
        return "general";
    }

    private ContentSuggestion createTemplateSuggestion(String componentType, String rawInput) {
        return switch (componentType.toLowerCase()) {
            case "hero" -> ContentSuggestion.builder()
                .title("Unleash Your Potential")
                .subtitle("Innovation Awaits")
                .description("Discover innovative solutions designed to transform your digital experience.")
                .ctaText("Get Started")
                .ctaUrl("/get-started")
                .imageUrl("https://images.unsplash.com/photo-1497366216548-37526070297c?w=1200")
                .imageAlt("Hero banner image")
                .build();
            case "product" -> ContentSuggestion.builder()
                .title("Premium Quality Products")
                .subtitle("Crafted with Care")
                .description("Crafted with precision and care for the discerning customer.")
                .ctaText("Shop Now")
                .ctaUrl("/products")
                .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800")
                .imageAlt("Product showcase")
                .price("$99.99")
                .build();
            case "teaser" -> ContentSuggestion.builder()
                .title("Explore What's New")
                .subtitle("Stay Ahead")
                .description("Stay ahead of the curve with our latest innovations and offerings.")
                .ctaText("Learn More")
                .ctaUrl("/whats-new")
                .imageUrl("https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600")
                .imageAlt("Teaser image")
                .build();
            case "banner" -> ContentSuggestion.builder()
                .title("Special Offer Inside")
                .subtitle("Limited Time Only")
                .description("Don't miss out on exclusive deals available for a limited time only.")
                .ctaText("Claim Offer")
                .ctaUrl("/offers")
                .imageUrl("https://images.unsplash.com/photo-1557804506-669a67965ba0?w=1200")
                .imageAlt("Banner image")
                .build();
            default -> ContentSuggestion.builder()
                .title("Welcome to Our World")
                .subtitle("Excellence in Everything")
                .description("Experience excellence in everything we do. Your journey starts here.")
                .ctaText("Explore")
                .ctaUrl("/explore")
                .imageUrl("https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=800")
                .imageAlt("Welcome image")
                .build();
        };
    }
}
