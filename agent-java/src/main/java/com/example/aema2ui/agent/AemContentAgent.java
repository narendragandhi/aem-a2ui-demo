package com.example.aema2ui.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.example.aema2ui.model.ContentSuggestion;
import com.example.aema2ui.model.UserInput;
import com.example.aema2ui.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Embabel Agent for generating AEM content suggestions.
 *
 * This agent supports multiple LLM providers:
 * - OpenAI (GPT models)
 * - Anthropic (Claude models)
 * - Ollama (Local models like Llama, Mistral)
 *
 * When LLM is not configured, falls back to template-based generation.
 */
@Slf4j
@Component
@Agent(description = "AI agent that generates content suggestions for AEM components")
public class AemContentAgent {

    private final LlmService llmService;

    // Prompts for LLM integration
    public static final String PARSE_INPUT_PROMPT = """
        Analyze the following user request for AEM content creation and extract the following as JSON:
        - detectedComponentType: The type of component (hero, product, teaser, banner, or general)
        - targetAudience: The target audience mentioned (or "general audience" if not specified)
        - brandStyle: Any brand or style preferences (or "professional and modern" if not specified)
        - toneOfVoice: The tone requested (or "professional yet approachable" if not specified)

        User request: %s

        Respond with JSON only:
        {"detectedComponentType": "...", "targetAudience": "...", "brandStyle": "...", "toneOfVoice": "..."}
        """;

    public static final String GENERATE_CONTENT_PROMPT = """
        Generate compelling marketing content for an AEM %s component.

        Context:
        - Target audience: %s
        - Brand style: %s
        - Tone of voice: %s
        - Original request: %s

        Create professional, engaging content and respond with JSON only:
        {
          "title": "A strong headline that captures attention",
          "subtitle": "A supporting subtitle",
          "description": "A compelling description with clear value proposition",
          "ctaText": "Call-to-action button text",
          "ctaUrl": "/relevant-url",
          "imageUrl": "https://images.unsplash.com/photo-XXXXX?w=800",
          "imageAlt": "Descriptive alt text for the image"
        }

        Make the content feel authentic and persuasive.
        """;

    @Autowired
    public AemContentAgent(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Parse user input to understand their intent.
     * Uses LLM when enabled, otherwise falls back to keyword matching.
     */
    @Action
    public UserInput parseUserIntent(String rawInput) {
        log.info("Parsing user intent from: {}", rawInput);

        if (llmService.isEnabled()) {
            try {
                log.info("Using {} for intent parsing", llmService.getProvider());
                UserInput parsed = llmService.generateObject(
                    String.format(PARSE_INPUT_PROMPT, rawInput),
                    UserInput.class
                );
                parsed.setRawText(rawInput);
                log.info("LLM detected component type: {}", parsed.getDetectedComponentType());
                return parsed;
            } catch (Exception e) {
                log.warn("LLM parsing failed, falling back to templates: {}", e.getMessage());
            }
        }

        // Fallback to keyword matching
        String input = rawInput != null ? rawInput.toLowerCase() : "";

        UserInput parsed = UserInput.builder()
            .rawText(rawInput)
            .detectedComponentType(detectComponentType(input))
            .targetAudience("general audience")
            .brandStyle("professional and modern")
            .toneOfVoice("professional yet approachable")
            .build();

        log.info("Template detected component type: {}", parsed.getDetectedComponentType());
        return parsed;
    }

    /**
     * Generate content suggestion based on parsed user input.
     * Uses LLM when enabled, otherwise falls back to templates.
     */
    @AchievesGoal(description = "Generate content suggestion for AEM component")
    @Action
    public ContentSuggestion generateContent(UserInput input) {
        log.info("Generating content for component type: {}", input.getDetectedComponentType());

        String componentType = input.getDetectedComponentType() != null
            ? input.getDetectedComponentType()
            : "general";

        if (llmService.isEnabled()) {
            try {
                log.info("Using {} for content generation", llmService.getProvider());

                String audience = input.getTargetAudience() != null
                    ? input.getTargetAudience()
                    : "general audience";
                String brandStyle = input.getBrandStyle() != null
                    ? input.getBrandStyle()
                    : "professional and modern";
                String tone = input.getToneOfVoice() != null
                    ? input.getToneOfVoice()
                    : "professional yet approachable";

                ContentSuggestion suggestion = llmService.generateObject(
                    String.format(GENERATE_CONTENT_PROMPT,
                        componentType, audience, brandStyle, tone, input.getRawText()),
                    ContentSuggestion.class
                );

                suggestion.setComponentType(componentType);

                // Add default image if not provided
                if (suggestion.getImageUrl() == null || suggestion.getImageUrl().isEmpty()) {
                    suggestion.setImageUrl(getDefaultImageUrl(componentType));
                }

                log.info("LLM generated content: {}", suggestion.getTitle());
                return suggestion;
            } catch (Exception e) {
                log.warn("LLM generation failed, falling back to templates: {}", e.getMessage());
            }
        }

        // Fallback to template
        ContentSuggestion suggestion = createTemplateSuggestion(componentType, input.getRawText());
        suggestion.setComponentType(componentType);

        log.info("Template generated content: {}", suggestion.getTitle());
        return suggestion;
    }

    private String detectComponentType(String input) {
        if (input.contains("hero")) return "hero";
        if (input.contains("product")) return "product";
        if (input.contains("teaser")) return "teaser";
        if (input.contains("banner")) return "banner";
        return "general";
    }

    private String getDefaultImageUrl(String componentType) {
        return switch (componentType.toLowerCase()) {
            case "hero" -> "https://images.unsplash.com/photo-1497366216548-37526070297c?w=1200";
            case "product" -> "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800";
            case "teaser" -> "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600";
            case "banner" -> "https://images.unsplash.com/photo-1557804506-669a67965ba0?w=1200";
            default -> "https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=800";
        };
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
