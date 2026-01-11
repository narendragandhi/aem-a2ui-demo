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

    // Brand guidelines that inform all content generation
    public static final String BRAND_GUIDELINES = """
        === BRAND GUIDELINES FOR ACME CORP ===

        BRAND VOICE:
        - Tone: Professional, Innovative, Trustworthy
        - Personality: We speak with confidence but never arrogance. We're experts who make complex ideas accessible.

        WRITING RULES:
        - Headlines: Bold, concise, action-oriented (max 6 words)
        - Use action verbs: Transform, Discover, Unlock, Accelerate, Simplify, Elevate, Power, Build
        - Body copy: Clear, scannable, max 2 sentences per thought
        - AVOID: Jargon, passive voice, superlatives like "best" or "leading"

        VALUE PILLARS (incorporate these themes):
        - Speed & Efficiency
        - Enterprise Security
        - Seamless Integration

        TARGET AUDIENCE: Enterprise IT decision-makers and digital marketers

        CTA STYLE: Action verbs with urgency but not pressure
        - Good CTAs: "Start Free Trial", "See It In Action", "Get Your Demo", "Explore Now"
        - Avoid: "Buy Now", "Don't Miss Out", "Act Fast"

        EXAMPLE GOOD HEADLINES:
        - "Transform Your Workflow in Minutes"
        - "Security That Scales With You"
        - "One Platform. Endless Possibilities."

        === END BRAND GUIDELINES ===
        """;

    public static final String GENERATE_CONTENT_PROMPT = """
        You are a brand-aware content generation AI for Acme Corp.

        %s

        Generate compelling marketing content for an AEM %s component.

        Context:
        - Target audience: %s
        - Brand style: %s
        - Tone of voice: %s
        - Original request: %s

        IMPORTANT REQUIREMENTS:
        1. Follow the brand guidelines above strictly
        2. Use action-oriented headlines (6 words or less)
        3. Incorporate at least one value pillar theme
        4. Keep descriptions scannable (under 150 characters)
        5. Use approved CTA styles only
        6. Never use words from the "AVOID" list

        Respond with JSON only:
        {
          "title": "Action-oriented headline following brand voice",
          "subtitle": "Supporting subtitle that reinforces value",
          "description": "Clear, scannable description incorporating value pillars",
          "ctaText": "Brand-approved CTA text",
          "ctaUrl": "/relevant-url",
          "imageUrl": "https://images.unsplash.com/photo-XXXXX?w=800",
          "imageAlt": "Descriptive alt text for the image"
        }

        Make the content feel authentic, professional, and aligned with brand guidelines.
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
                        BRAND_GUIDELINES, componentType, audience, brandStyle, tone, input.getRawText()),
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
        // Templates follow brand guidelines: action verbs, value pillars, approved CTA styles
        return switch (componentType.toLowerCase()) {
            case "hero" -> ContentSuggestion.builder()
                .title("Transform Your Workflow")
                .subtitle("Speed & Efficiency Redefined")
                .description("Accelerate your team's productivity with seamless enterprise integration.")
                .ctaText("See It In Action")
                .ctaUrl("/demo")
                .imageUrl("https://images.unsplash.com/photo-1497366216548-37526070297c?w=1200")
                .imageAlt("Modern enterprise dashboard")
                .build();
            case "product" -> ContentSuggestion.builder()
                .title("Unlock Enterprise Power")
                .subtitle("Security That Scales")
                .description("Built for enterprises. Trusted by security teams worldwide.")
                .ctaText("Start Free Trial")
                .ctaUrl("/trial")
                .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800")
                .imageAlt("Enterprise product showcase")
                .price("Contact Sales")
                .build();
            case "teaser" -> ContentSuggestion.builder()
                .title("Discover Seamless Integration")
                .subtitle("Connect Everything")
                .description("One platform. Endless possibilities. Your tools, unified.")
                .ctaText("Explore Now")
                .ctaUrl("/integrations")
                .imageUrl("https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600")
                .imageAlt("Integration ecosystem")
                .build();
            case "banner" -> ContentSuggestion.builder()
                .title("Accelerate Your Growth")
                .subtitle("Enterprise-Ready Today")
                .description("Join thousands of companies transforming their operations.")
                .ctaText("Get Your Demo")
                .ctaUrl("/contact")
                .imageUrl("https://images.unsplash.com/photo-1557804506-669a67965ba0?w=1200")
                .imageAlt("Team collaboration")
                .build();
            default -> ContentSuggestion.builder()
                .title("Simplify Your Operations")
                .subtitle("Built for Enterprise")
                .description("Powerful solutions designed for scale, security, and speed.")
                .ctaText("Learn More")
                .ctaUrl("/overview")
                .imageUrl("https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=800")
                .imageAlt("Modern office environment")
                .build();
        };
    }
}
