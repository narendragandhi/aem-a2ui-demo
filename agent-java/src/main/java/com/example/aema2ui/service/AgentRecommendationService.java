package com.example.aema2ui.service;

import com.example.aema2ui.model.PageRecommendation;
import com.example.aema2ui.model.PageRecommendation.SectionRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI-driven component and layout recommendation service.
 *
 * This is a key A2UI feature: instead of the user manually selecting components,
 * the agent analyzes their input and recommends an optimal page layout.
 *
 * Flow:
 * 1. User describes what they want (e.g., "landing page for summer sale")
 * 2. Agent analyzes keywords, intent, and context
 * 3. Agent recommends components and their arrangement
 * 4. User can accept, modify, or reject recommendations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRecommendationService {

    private final LlmService llmService;

    // Component metadata for recommendations
    private static final Map<String, ComponentInfo> COMPONENT_CATALOG = Map.ofEntries(
        Map.entry("hero", new ComponentInfo("Hero Banner", "🦸", "High-impact visual header with CTA")),
        Map.entry("teaser", new ComponentInfo("Teaser", "📰", "Content preview card")),
        Map.entry("product", new ComponentInfo("Product Card", "🛍️", "E-commerce product display")),
        Map.entry("cta", new ComponentInfo("Call to Action", "📣", "Conversion-focused banner")),
        Map.entry("quote", new ComponentInfo("Quote/Testimonial", "💬", "Social proof element")),
        Map.entry("navigation", new ComponentInfo("Navigation", "🧭", "Site navigation menu")),
        Map.entry("footer", new ComponentInfo("Footer", "📋", "Page footer with links")),
        Map.entry("accordion", new ComponentInfo("Accordion", "📂", "Collapsible FAQ sections")),
        Map.entry("tabs", new ComponentInfo("Tabs", "📑", "Tabbed content panels")),
        Map.entry("carousel", new ComponentInfo("Carousel", "🎠", "Image/content slider")),
        Map.entry("video", new ComponentInfo("Video Player", "🎬", "Embedded video content")),
        Map.entry("gallery", new ComponentInfo("Image Gallery", "🖼️", "Photo grid/gallery")),
        Map.entry("pricing", new ComponentInfo("Pricing Table", "💰", "Price comparison table")),
        Map.entry("form", new ComponentInfo("Form", "📝", "Data collection form")),
        Map.entry("social", new ComponentInfo("Social Share", "🔗", "Social media links"))
    );

    // Condensed LLM prompt for layout recommendations
    private static final String RECOMMEND_PROMPT = """
        Recommend page layout for: %s
        Components: hero,teaser,product,cta,quote,navigation,footer,accordion,tabs,carousel,video,gallery,pricing,form,social
        JSON: {"pageType":"landing|product|blog|general","reasoning":"why","confidence":85,"sections":[{"componentType":"hero","reason":"why","suggestedPrompt":"prompt"}],"alternatives":["alt1"]}
        """;

    /**
     * Analyze user input and recommend a page layout.
     * Uses LLM when available, falls back to rule-based recommendations.
     */
    public PageRecommendation recommendLayout(String userInput) {
        log.info("Analyzing input for layout recommendation: {}", userInput);

        if (llmService.isEnabled()) {
            try {
                return recommendWithLlm(userInput);
            } catch (Exception e) {
                log.warn("LLM recommendation failed, using rule-based: {}", e.getMessage());
            }
        }

        return recommendWithRules(userInput);
    }

    /**
     * LLM-based intelligent recommendation
     */
    @SuppressWarnings("unchecked")
    private PageRecommendation recommendWithLlm(String userInput) {
        log.info("Using {} for layout recommendation", llmService.getProvider());

        // Use Map for flexible LLM response parsing
        Map<String, Object> response = llmService.generateObject(
            String.format(RECOMMEND_PROMPT, userInput),
            Map.class
        );

        String pageType = (String) response.getOrDefault("pageType", "general");
        String reasoning = (String) response.getOrDefault("reasoning", "AI-recommended layout");
        int confidence = response.get("confidence") instanceof Number
            ? ((Number) response.get("confidence")).intValue()
            : 75;
        List<String> alternatives = (List<String>) response.getOrDefault("alternatives", List.of());

        // Parse sections
        List<SectionRecommendation> sections = new ArrayList<>();
        List<Map<String, String>> rawSections = (List<Map<String, String>>) response.getOrDefault("sections", List.of());

        int position = 1;
        for (Map<String, String> rawSection : rawSections) {
            String componentType = rawSection.getOrDefault("componentType", "teaser");
            ComponentInfo info = COMPONENT_CATALOG.getOrDefault(
                componentType,
                new ComponentInfo(componentType, "📦", "Component")
            );

            sections.add(SectionRecommendation.builder()
                .componentType(componentType)
                .displayName(info.displayName())
                .icon(info.icon())
                .reason(rawSection.getOrDefault("reason", "Recommended component"))
                .suggestedPrompt(rawSection.getOrDefault("suggestedPrompt", "Content for " + componentType))
                .position(position)
                .required(position <= 2) // First 2 components are required
                .build());
            position++;
        }

        return PageRecommendation.builder()
            .pageType(pageType)
            .reasoning(reasoning)
            .confidence(confidence)
            .sections(sections)
            .alternatives(alternatives)
            .build();
    }

    /**
     * Rule-based recommendation when LLM is unavailable.
     * Analyzes keywords and patterns to suggest appropriate layouts.
     */
    private PageRecommendation recommendWithRules(String userInput) {
        String input = userInput.toLowerCase();

        // Detect page type from keywords
        PageType detectedType = detectPageType(input);

        // Get recommended sections for this page type
        List<SectionRecommendation> sections = getRecommendedSections(detectedType, input);

        return PageRecommendation.builder()
            .pageType(detectedType.name)
            .reasoning(detectedType.reasoning)
            .confidence(detectedType.confidence)
            .sections(sections)
            .alternatives(detectedType.alternatives)
            .build();
    }

    /**
     * Detect page type from user input
     */
    private PageType detectPageType(String input) {
        // Landing page patterns
        if (containsAny(input, "landing", "campaign", "promotion", "launch", "sale", "offer", "discount")) {
            return new PageType("landing",
                "Detected promotional/campaign language suggesting a landing page optimized for conversions",
                85,
                List.of("Product showcase page", "Simple CTA page"));
        }

        // Product page patterns
        if (containsAny(input, "product", "buy", "purchase", "pricing", "features", "specs", "compare")) {
            return new PageType("product",
                "Product-related keywords indicate a product showcase or e-commerce page",
                88,
                List.of("Comparison page", "Features page"));
        }

        // Blog/article patterns
        if (containsAny(input, "blog", "article", "story", "news", "post", "content", "read")) {
            return new PageType("blog",
                "Content-focused keywords suggest a blog or article page layout",
                82,
                List.of("News page", "Resource page"));
        }

        // About/company patterns
        if (containsAny(input, "about", "team", "company", "story", "mission", "values", "history")) {
            return new PageType("about",
                "Company/brand keywords indicate an about or team page",
                80,
                List.of("Team page", "Culture page"));
        }

        // Contact/support patterns
        if (containsAny(input, "contact", "support", "help", "faq", "question")) {
            return new PageType("contact",
                "Support-related keywords suggest a contact or FAQ page",
                83,
                List.of("Support page", "Help center"));
        }

        // Default: general landing page
        return new PageType("general",
            "General content page with balanced component mix",
            70,
            List.of("Landing page", "Product page", "Blog page"));
    }

    /**
     * Get recommended sections based on page type
     */
    private List<SectionRecommendation> getRecommendedSections(PageType pageType, String input) {
        List<SectionRecommendation> sections = new ArrayList<>();
        int position = 1;

        // Always start with navigation
        sections.add(createSection("navigation", position++,
            "Site navigation for user orientation",
            "Main navigation with brand logo and key page links",
            false));

        switch (pageType.name) {
            case "landing" -> {
                sections.add(createSection("hero", position++,
                    "High-impact hero to capture attention immediately",
                    extractPromptContext(input, "Compelling hero for"),
                    true));
                sections.add(createSection("teaser", position++,
                    "Feature highlights to build interest",
                    "Key benefit or feature highlight",
                    true));
                sections.add(createSection("teaser", position++,
                    "Additional value proposition",
                    "Secondary benefit or feature",
                    false));
                sections.add(createSection("quote", position++,
                    "Social proof to build trust",
                    "Customer testimonial or success story",
                    false));
                sections.add(createSection("cta", position++,
                    "Strong CTA to drive conversions",
                    "Primary conversion call-to-action",
                    true));
            }
            case "product" -> {
                sections.add(createSection("hero", position++,
                    "Product introduction with key value proposition",
                    extractPromptContext(input, "Product showcase for"),
                    true));
                sections.add(createSection("product", position++,
                    "Detailed product information",
                    "Main product details with pricing",
                    true));
                sections.add(createSection("tabs", position++,
                    "Organized product details (specs, reviews, etc.)",
                    "Product specifications, reviews, and FAQ tabs",
                    false));
                sections.add(createSection("quote", position++,
                    "Customer reviews for social proof",
                    "Customer testimonial about product",
                    false));
                sections.add(createSection("cta", position++,
                    "Purchase or trial CTA",
                    "Product purchase or trial call-to-action",
                    true));
            }
            case "blog" -> {
                sections.add(createSection("hero", position++,
                    "Article header with title and metadata",
                    extractPromptContext(input, "Blog article header for"),
                    true));
                sections.add(createSection("teaser", position++,
                    "Article content preview or introduction",
                    "Article introduction and key points",
                    true));
                sections.add(createSection("accordion", position++,
                    "Structured content sections",
                    "Article sections with expandable details",
                    false));
                sections.add(createSection("social", position++,
                    "Social sharing for content distribution",
                    "Share buttons and author bio",
                    false));
            }
            case "about" -> {
                sections.add(createSection("hero", position++,
                    "Company/brand story introduction",
                    extractPromptContext(input, "Company introduction for"),
                    true));
                sections.add(createSection("teaser", position++,
                    "Mission and values highlight",
                    "Company mission and core values",
                    true));
                sections.add(createSection("gallery", position++,
                    "Team or office photos",
                    "Team photos or company culture gallery",
                    false));
                sections.add(createSection("quote", position++,
                    "Leadership quote or company milestone",
                    "Founder quote or company achievement",
                    false));
            }
            case "contact" -> {
                sections.add(createSection("hero", position++,
                    "Contact page header",
                    "Get in touch header with subtitle",
                    true));
                sections.add(createSection("form", position++,
                    "Contact form for inquiries",
                    "Contact form with name, email, and message fields",
                    true));
                sections.add(createSection("accordion", position++,
                    "FAQ section for common questions",
                    "Frequently asked questions",
                    false));
            }
            default -> {
                // General page layout
                sections.add(createSection("hero", position++,
                    "Page header with main message",
                    extractPromptContext(input, "Page header for"),
                    true));
                sections.add(createSection("teaser", position++,
                    "Key content section",
                    "Main content highlight",
                    true));
                sections.add(createSection("cta", position++,
                    "Page call-to-action",
                    "Primary action for visitors",
                    false));
            }
        }

        // Always end with footer
        sections.add(createSection("footer", position,
            "Standard page footer with links and legal",
            "Footer with sitemap, social links, and copyright",
            false));

        return sections;
    }

    private SectionRecommendation createSection(String type, int position, String reason,
                                                 String prompt, boolean required) {
        ComponentInfo info = COMPONENT_CATALOG.getOrDefault(type,
            new ComponentInfo(type, "📦", "Component"));

        return SectionRecommendation.builder()
            .componentType(type)
            .displayName(info.displayName())
            .icon(info.icon())
            .reason(reason)
            .suggestedPrompt(prompt)
            .position(position)
            .required(required)
            .build();
    }

    /**
     * Extract context from user input for prompt suggestions
     */
    private String extractPromptContext(String input, String prefix) {
        // Clean up input and create a prompt suggestion
        String cleaned = input.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100) + "...";
        }
        return prefix + " " + cleaned;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
                       .matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    // Helper records
    private record ComponentInfo(String displayName, String icon, String description) {}

    private record PageType(String name, String reasoning, int confidence, List<String> alternatives) {}
}
