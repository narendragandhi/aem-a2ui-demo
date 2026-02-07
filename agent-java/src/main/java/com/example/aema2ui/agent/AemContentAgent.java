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

    // Condensed prompt for intent parsing
    public static final String PARSE_INPUT_PROMPT = """
        Extract from request: %s
        JSON: {"detectedComponentType":"hero|product|teaser|banner|general","targetAudience":"audience","brandStyle":"style","toneOfVoice":"tone"}
        """;

    // Condensed brand guidelines for faster LLM processing
    public static final String BRAND_GUIDELINES = """
        BRAND: Acme Corp - Professional, Innovative, Trustworthy
        HEADLINES: Action verbs (Transform, Discover, Unlock), max 6 words
        COPY: Clear, scannable, under 150 chars
        CTAs: "Start Free Trial", "See It In Action", "Explore Now"
        AVOID: Jargon, passive voice, "best/leading"
        """;

    public static final String GENERATE_CONTENT_PROMPT = """
        %s
        Generate %s content for: %s
        Audience: %s | Style: %s | Tone: %s

        Reply ONLY with this JSON (no other text):
        {"title":"short headline","subtitle":"value prop","description":"brief copy","ctaText":"action","ctaUrl":"/path"}
        """;

    @Autowired
    public AemContentAgent(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Parse user input to understand their intent.
     * PERFORMANCE OPTIMIZATION: Uses fast keyword matching first.
     * Only uses LLM for complex inputs where component type is unclear.
     */
    @Action
    public UserInput parseUserIntent(String rawInput) {
        log.info("Parsing user intent from: {}", rawInput);

        String input = rawInput != null ? rawInput.toLowerCase() : "";
        String detectedType = detectComponentType(input);

        // OPTIMIZATION: Skip LLM parsing if we can clearly detect component type
        // This saves one LLM call per request for common cases
        if (!detectedType.equals("general")) {
            log.info("Fast path: detected component type '{}' from keywords", detectedType);
            return UserInput.builder()
                .rawText(rawInput)
                .detectedComponentType(detectedType)
                .targetAudience("general audience")
                .brandStyle("professional and modern")
                .toneOfVoice("professional yet approachable")
                .build();
        }

        // Only use LLM for ambiguous inputs where we need deeper understanding
        if (llmService.isEnabled()) {
            try {
                log.info("Using {} for intent parsing (complex input)", llmService.getProvider());
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
        UserInput parsed = UserInput.builder()
            .rawText(rawInput)
            .detectedComponentType(detectedType)
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
                        BRAND_GUIDELINES, componentType, input.getRawText(), audience, brandStyle, tone),
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

    /**
     * Generate content using templates only (no LLM).
     * Fast path for instant responses.
     */
    public ContentSuggestion generateTemplateContent(String rawInput, String componentType) {
        String type = componentType != null && !componentType.isEmpty()
            ? componentType
            : detectComponentType(rawInput.toLowerCase());
        ContentSuggestion suggestion = createTemplateSuggestion(type, rawInput);
        suggestion.setComponentType(type);
        log.info("Template-only content generated for type: {}", type);
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

    // Multiple template variations for variety
    private static final String[][] HERO_TEMPLATES = {
        {"Transform Your Digital Experience", "Innovation Meets Simplicity", "Empower your team with tools designed for the modern enterprise.", "See It In Action", "/demo"},
        {"Unlock Your Team's Potential", "Speed & Efficiency Redefined", "Accelerate productivity with seamless workflow automation.", "Start Free Trial", "/trial"},
        {"Elevate Your Business Today", "Enterprise-Grade Solutions", "Scale confidently with security and performance built-in.", "Get Started", "/start"},
        {"Discover Smarter Workflows", "Automation That Adapts", "Reduce manual work by 70% with intelligent process automation.", "Watch Demo", "/demo"},
        {"Build the Future of Work", "Collaboration Reimagined", "Connect teams, tools, and data in one unified platform.", "Explore Now", "/explore"}
    };

    private static final String[][] PRODUCT_TEMPLATES = {
        {"Enterprise Security Suite", "Protection That Scales", "Zero-trust security trusted by Fortune 500 companies.", "Start Free Trial", "$299/mo"},
        {"Workflow Automation Pro", "Automate Everything", "Build powerful automations without writing code.", "Try It Free", "$149/mo"},
        {"Analytics Dashboard Plus", "Insights in Real-Time", "Make data-driven decisions with live dashboards.", "Get Started", "$199/mo"},
        {"Team Collaboration Hub", "Work Better Together", "All your communication and files in one place.", "Start Free", "$99/mo"},
        {"Cloud Integration Platform", "Connect Any System", "500+ pre-built connectors. Deploy in minutes.", "View Pricing", "$249/mo"}
    };

    private static final String[][] TEASER_TEMPLATES = {
        {"Seamless Integrations", "Connect Your Stack", "One-click integrations with 200+ enterprise tools.", "Learn More", "/integrations"},
        {"Real-Time Analytics", "Data at Your Fingertips", "Track KPIs and metrics that matter most.", "See Features", "/analytics"},
        {"Enterprise Security", "Bank-Grade Protection", "SOC 2 certified with end-to-end encryption.", "View Security", "/security"},
        {"24/7 Support", "We're Here For You", "Expert support whenever you need it most.", "Contact Us", "/support"},
        {"Global Scale", "Deploy Anywhere", "99.99% uptime with data centers worldwide.", "See Infrastructure", "/infrastructure"}
    };

    private static final String[][] BANNER_TEMPLATES = {
        {"Limited Time: 30% Off Annual Plans", "Enterprise-Ready Today", "Join 10,000+ companies already transforming.", "Claim Offer", "/pricing"},
        {"Free Workshop: Digital Transformation", "Learn From Experts", "Register now for our upcoming masterclass.", "Reserve Spot", "/workshop"},
        {"New Feature: AI-Powered Insights", "Smarter Decisions Faster", "Discover patterns humans miss with ML analytics.", "Try It Now", "/ai-features"},
        {"Customer Success Story", "How Acme Saved 40% on Ops", "Read how leading companies achieve more.", "Read Case Study", "/customers"},
        {"Product Update: v3.0 Released", "Faster. Smarter. Better.", "50+ new features and 2x performance boost.", "See What's New", "/changelog"}
    };

    private int templateIndex = 0;

    private ContentSuggestion createTemplateSuggestion(String componentType, String rawInput) {
        // Rotate through templates for variety
        String[][] templates = switch (componentType.toLowerCase()) {
            case "hero" -> HERO_TEMPLATES;
            case "product" -> PRODUCT_TEMPLATES;
            case "teaser" -> TEASER_TEMPLATES;
            case "banner" -> BANNER_TEMPLATES;
            default -> HERO_TEMPLATES;
        };

        // Use input hash to select template (same input = same output, different input = different output)
        int index = Math.abs(rawInput.hashCode()) % templates.length;
        String[] t = templates[index];

        String imageUrl = getDefaultImageUrl(componentType);

        ContentSuggestion.ContentSuggestionBuilder builder = ContentSuggestion.builder()
            .title(t[0])
            .subtitle(t[1])
            .description(t[2])
            .ctaText(t[3])
            .ctaUrl(t[4])
            .imageUrl(imageUrl)
            .imageAlt(t[0]);

        // Add price for product type
        if (componentType.equalsIgnoreCase("product") && t.length > 4) {
            builder.price(t[4]);
            builder.ctaUrl("/pricing");
        }

        return builder.build();
    }
}
