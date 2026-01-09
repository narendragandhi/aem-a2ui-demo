package com.example.aema2ui.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Advanced A2UI demos showcasing powerful features:
 * 1. Dynamic template-based lists (data-driven UI)
 * 2. Multi-step wizards (progressive disclosure)
 * 3. Tabbed interfaces
 * 4. AI content generator with live preview
 * 5. Real-time collaboration
 */
@Service
@RequiredArgsConstructor
public class AdvancedA2UIService {

    private final A2UIMessageBuilder builder;

    /**
     * Get available demos.
     */
    public Map<String, String> getAvailableDemos() {
        return Map.of(
            "list", "Dynamic Template List",
            "wizard", "Multi-Step Wizard",
            "tabs", "Tabbed Properties",
            "ai", "AI Content Generator",
            "collab", "Collaboration Panel"
        );
    }

    /**
     * Select demo based on user input.
     */
    public List<Map<String, Object>> selectDemo(String userInput) {
        String input = userInput != null ? userInput.toLowerCase() : "";

        if (input.contains("list") || input.contains("asset") || input.contains("dam") || input.contains("browse")) {
            return demoDynamicList();
        } else if (input.contains("wizard") || input.contains("step") || input.contains("create")) {
            return demoWizard(1);
        } else if (input.contains("tab") || input.contains("properties") || input.contains("config")) {
            return demoTabs();
        } else if (input.contains("collab") || input.contains("comment") || input.contains("team")) {
            return demoCollaboration();
        } else {
            // Default to AI content generator
            return demoAIContentGenerator();
        }
    }

    // =========================================================================
    // DEMO 1: Dynamic Template-Based List
    // =========================================================================

    public List<Map<String, Object>> demoDynamicList() {
        String surfaceId = "asset_browser_" + uuid();

        List<Map<String, Object>> components = new ArrayList<>();

        // Root layout
        components.add(builder.column("root", List.of("header", "search_bar", "asset_list", "pagination")));

        // Header
        components.add(builder.text("header", "DAM Asset Browser", "h1"));

        // Search bar
        components.add(builder.row("search_bar", List.of("search_input", "filter_btn")));
        components.add(textFieldWithPath("search_input", "Search assets...", "/search/query"));
        components.add(builder.button("filter_btn", "Filter", "open_filters"));

        // Template-based list - THE MAGIC!
        components.add(templateList("asset_list", "/assets", "asset_card"));

        // Asset card template
        components.add(card("asset_card", List.of("card_content")));
        components.add(builder.row("card_content", List.of("asset_thumb", "asset_info", "asset_actions")));
        components.add(imageWithPath("asset_thumb", "thumbnail", "title"));
        components.add(builder.column("asset_info", List.of("asset_title", "asset_meta")));
        components.add(textWithPath("asset_title", "title", "subtitle"));
        components.add(textWithPath("asset_meta", "metadata", "caption"));
        components.add(builder.column("asset_actions", List.of("select_btn", "preview_btn")));
        components.add(buttonWithContext("select_btn", "Select", "select_asset",
            List.of(contextPath("path", "path"), contextPath("title", "title"))));
        components.add(buttonWithContext("preview_btn", "Preview", "preview_asset",
            List.of(contextPath("path", "path"))));

        // Pagination
        components.add(builder.row("pagination", List.of("prev_btn", "page_info", "next_btn")));
        components.add(builder.button("prev_btn", "Previous", "prev_page"));
        components.add(textWithPath("page_info", "/pagination/info", "body"));
        components.add(builder.button("next_btn", "Next", "next_page"));

        // Data model with asset list
        List<Map<String, Object>> assets = List.of(
            assetData("/content/dam/mysite/hero-banner.jpg", "Hero Banner",
                "https://picsum.photos/120/80?random=1", "JPG • 1920x1080 • 2.4 MB"),
            assetData("/content/dam/mysite/product-shot.jpg", "Product Photography",
                "https://picsum.photos/120/80?random=2", "JPG • 2400x1600 • 3.1 MB"),
            assetData("/content/dam/mysite/team-photo.jpg", "Team Photo",
                "https://picsum.photos/120/80?random=3", "JPG • 3000x2000 • 4.2 MB"),
            assetData("/content/dam/mysite/icon-set.svg", "Icon Set",
                "https://picsum.photos/120/80?random=4", "SVG • Vector • 45 KB")
        );

        return List.of(
            builder.beginRendering(surfaceId, "root"),
            builder.surfaceUpdate(surfaceId, components),
            dataModelUpdate(surfaceId, "", List.of(
                valueMap("search", List.of(builder.dataString("query", ""))),
                valueMap("pagination", List.of(builder.dataString("info", "Page 1 of 5 (47 assets)"))),
                valueList("assets", assets)
            ))
        );
    }

    // =========================================================================
    // DEMO 2: Multi-Step Wizard
    // =========================================================================

    public List<Map<String, Object>> demoWizard(int step) {
        String surfaceId = "wizard_" + uuid();

        List<Map<String, Object>> components = new ArrayList<>();

        // Progress indicator
        String step1Icon = step > 1 ? "✓" : "1";
        String step2Icon = step > 2 ? "✓" : "2";
        String step3Icon = step > 3 ? "✓" : "3";

        components.add(builder.column("root", List.of("progress", "title", "step_content", "nav_buttons")));
        components.add(builder.row("progress", List.of("step_1_ind", "step_2_ind", "step_3_ind")));
        components.add(builder.text("step_1_ind", step1Icon + " Type", "caption"));
        components.add(builder.text("step_2_ind", step2Icon + " Layout", "caption"));
        components.add(builder.text("step_3_ind", step3Icon + " Content", "caption"));

        // Step-specific content
        String title;
        List<Map<String, Object>> stepData;

        switch (step) {
            case 1 -> {
                title = "Step 1: Choose Component Type";
                components.add(multipleChoice("step_content", "What type of component do you need?",
                    List.of(
                        option("Hero Banner", "hero"),
                        option("Content Teaser", "teaser"),
                        option("Product Card", "product"),
                        option("Call to Action", "cta")
                    ), "/wizard/componentType"));
                stepData = List.of(builder.dataString("componentType", ""));
            }
            case 2 -> {
                title = "Step 2: Configure Layout";
                components.add(builder.column("step_content", List.of("layout_choice", "alignment_choice")));
                components.add(multipleChoice("layout_choice", "Layout Style",
                    List.of(
                        option("Full Width", "full"),
                        option("Contained", "contained"),
                        option("Split (50/50)", "split")
                    ), "/wizard/layout"));
                components.add(multipleChoice("alignment_choice", "Content Alignment",
                    List.of(
                        option("Left", "left"),
                        option("Center", "center"),
                        option("Right", "right")
                    ), "/wizard/alignment"));
                stepData = List.of(
                    builder.dataString("layout", "contained"),
                    builder.dataString("alignment", "center")
                );
            }
            case 3 -> {
                title = "Step 3: Add Content";
                components.add(builder.column("step_content", List.of("title_input", "desc_input", "image_picker", "cta_input")));
                components.add(textFieldWithPath("title_input", "Headline", "/wizard/content/title"));
                components.add(textFieldMultiline("desc_input", "Description", "/wizard/content/description", 3));
                components.add(builder.button("image_picker", "Select Image from DAM", "open_dam_picker"));
                components.add(textFieldWithPath("cta_input", "Button Text", "/wizard/content/ctaText"));
                stepData = List.of(
                    valueMap("content", List.of(
                        builder.dataString("title", "Discover Something Amazing"),
                        builder.dataString("description", "Experience the difference with our premium solutions."),
                        builder.dataString("ctaText", "Learn More")
                    ))
                );
            }
            default -> {
                title = "Step 1: Choose Component Type";
                stepData = List.of();
            }
        }

        components.add(builder.text("title", title, "h2"));

        // Navigation buttons
        List<String> navButtons = step > 1 ? List.of("back_btn", "next_btn") : List.of("next_btn");
        components.add(builder.row("nav_buttons", navButtons));

        if (step > 1) {
            components.add(buttonWithContext("back_btn", "Back", "wizard_navigate",
                List.of(contextNumber("step", step - 1))));
        }

        String nextLabel = step == 3 ? "Create Component" : "Next";
        String nextAction = step == 3 ? "wizard_complete" : "wizard_navigate";
        components.add(buttonWithContext("next_btn", nextLabel, nextAction,
            List.of(contextNumber("step", step + 1))));

        return List.of(
            builder.beginRendering(surfaceId, "root"),
            builder.surfaceUpdate(surfaceId, components),
            dataModelUpdate(surfaceId, "wizard", stepData)
        );
    }

    // =========================================================================
    // DEMO 3: Tabbed Interface
    // =========================================================================

    public List<Map<String, Object>> demoTabs() {
        String surfaceId = "tabs_" + uuid();

        List<Map<String, Object>> components = new ArrayList<>();

        components.add(builder.column("root", List.of("header", "tab_container")));
        components.add(builder.text("header", "Component Properties", "h2"));

        // Tabs component
        components.add(tabs("tab_container", List.of(
            tab("Content", "content_tab"),
            tab("Styling", "styling_tab"),
            tab("Advanced", "advanced_tab")
        ), "/selectedTab"));

        // Content Tab
        components.add(builder.column("content_tab", List.of("content_title", "content_desc", "content_image")));
        components.add(textFieldWithPath("content_title", "Title", "/content/title"));
        components.add(textFieldMultiline("content_desc", "Description", "/content/description", 4));
        components.add(builder.row("content_image", List.of("image_preview", "image_select")));
        components.add(imageWithPath("image_preview", "/content/imageUrl", "Selected image"));
        components.add(builder.button("image_select", "Change Image", "select_image"));

        // Styling Tab
        components.add(builder.column("styling_tab", List.of("style_theme", "style_spacing", "style_animation")));
        components.add(multipleChoice("style_theme", "Color Theme",
            List.of(option("Light", "light"), option("Dark", "dark"), option("Brand", "brand")),
            "/styling/theme"));
        components.add(slider("style_spacing", "Padding", 0, 100, 10, "/styling/padding"));
        components.add(checkbox("style_animation", "Enable entrance animation", "/styling/animated"));

        // Advanced Tab
        components.add(builder.column("advanced_tab", List.of("adv_id", "adv_analytics", "adv_cache")));
        components.add(textFieldWithPath("adv_id", "Component ID", "/advanced/componentId"));
        components.add(textFieldWithPath("adv_analytics", "Analytics Event Name", "/advanced/analyticsEvent"));
        components.add(checkbox("adv_cache", "Enable dispatcher caching", "/advanced/cacheable"));

        return List.of(
            builder.beginRendering(surfaceId, "root"),
            builder.surfaceUpdate(surfaceId, components),
            dataModelUpdate(surfaceId, "", List.of(
                dataNumber("selectedTab", 0),
                valueMap("content", List.of(
                    builder.dataString("title", "Welcome to Our Site"),
                    builder.dataString("description", "Discover amazing content and experiences."),
                    builder.dataString("imageUrl", "https://picsum.photos/400/200")
                )),
                valueMap("styling", List.of(
                    builder.dataString("theme", "light"),
                    builder.dataNumber("padding", 40),
                    builder.dataBoolean("animated", true)
                )),
                valueMap("advanced", List.of(
                    builder.dataString("componentId", "hero-banner-1"),
                    builder.dataString("analyticsEvent", "hero_view"),
                    builder.dataBoolean("cacheable", true)
                ))
            ))
        );
    }

    // =========================================================================
    // DEMO 4: AI Content Generator
    // =========================================================================

    public List<Map<String, Object>> demoAIContentGenerator() {
        String surfaceId = "ai_gen_" + uuid();

        List<Map<String, Object>> components = new ArrayList<>();

        components.add(builder.column("root", List.of("header", "main_content")));
        components.add(builder.row("header", List.of("title", "ai_badge")));
        components.add(builder.text("title", "AI Content Assistant", "h1"));
        components.add(builder.text("ai_badge", "Powered by Gemini", "caption"));

        components.add(builder.row("main_content", List.of("input_panel", "preview_panel")));

        // Input Panel
        components.add(builder.column("input_panel", List.of("prompt_section", "options_section", "generate_btn")));
        components.add(builder.column("prompt_section", List.of("prompt_label", "prompt_input")));
        components.add(builder.text("prompt_label", "Describe the content you need:", "subtitle"));
        components.add(textFieldMultiline("prompt_input", "E.g., 'Hero banner for summer sale'", "/input/prompt", 3));

        components.add(builder.column("options_section", List.of("tone_select", "length_select")));
        components.add(multipleChoice("tone_select", "Tone",
            List.of(
                option("Professional", "professional"),
                option("Casual", "casual"),
                option("Playful", "playful"),
                option("Urgent", "urgent")
            ), "/input/tone"));
        components.add(multipleChoice("length_select", "Content Length",
            List.of(
                option("Short", "short"),
                option("Medium", "medium"),
                option("Long", "long")
            ), "/input/length"));

        components.add(buttonWithContext("generate_btn", "Generate Content", "generate_content",
            List.of(
                contextPath("prompt", "/input/prompt"),
                contextPath("tone", "/input/tone"),
                contextPath("length", "/input/length")
            )));

        // Preview Panel
        components.add(card("preview_panel", List.of("preview_content")));
        components.add(builder.column("preview_content", List.of("preview_image", "preview_title", "preview_desc", "preview_cta", "preview_actions")));
        components.add(imageWithPath("preview_image", "/preview/imageUrl", "/preview/title"));
        components.add(textWithPath("preview_title", "/preview/title", "h2"));
        components.add(textWithPath("preview_desc", "/preview/description", "body"));
        components.add(buttonWithPathLabel("preview_cta", "/preview/ctaText", "preview_cta_click"));

        components.add(builder.row("preview_actions", List.of("apply_btn", "regenerate_btn", "copy_btn")));
        components.add(buttonWithContext("apply_btn", "Apply to Component", "apply_content",
            List.of(
                contextPath("title", "/preview/title"),
                contextPath("description", "/preview/description"),
                contextPath("ctaText", "/preview/ctaText"),
                contextPath("imageUrl", "/preview/imageUrl")
            )));
        components.add(builder.button("regenerate_btn", "Try Again", "regenerate"));
        components.add(builder.button("copy_btn", "Copy to Clipboard", "copy_content"));

        return List.of(
            builder.beginRendering(surfaceId, "root"),
            builder.surfaceUpdate(surfaceId, components),
            dataModelUpdate(surfaceId, "", List.of(
                valueMap("input", List.of(
                    builder.dataString("prompt", ""),
                    builder.dataString("tone", "professional"),
                    builder.dataString("length", "medium")
                )),
                valueMap("preview", List.of(
                    builder.dataString("title", "Summer Sale Extravaganza"),
                    builder.dataString("description", "Dive into savings with our biggest summer sale yet! Enjoy up to 50% off on selected items."),
                    builder.dataString("ctaText", "Shop Now"),
                    builder.dataString("imageUrl", "https://picsum.photos/600/300?random=summer")
                ))
            ))
        );
    }

    // =========================================================================
    // DEMO 5: Collaboration Panel
    // =========================================================================

    public List<Map<String, Object>> demoCollaboration() {
        String surfaceId = "collab_" + uuid();

        List<Map<String, Object>> components = new ArrayList<>();

        components.add(builder.column("root", List.of("header", "presence_bar", "comments_section")));
        components.add(builder.text("header", "Collaboration Panel", "h2"));

        // Presence bar
        components.add(card("presence_bar", List.of("presence_content")));
        components.add(builder.column("presence_content", List.of("presence_label", "presence_list")));
        components.add(builder.text("presence_label", "Currently Editing", "caption"));
        components.add(templateList("presence_list", "/users", "user_item"));

        components.add(builder.row("user_item", List.of("user_avatar", "user_info")));
        components.add(icon("user_avatar", "person", "color"));
        components.add(builder.column("user_info", List.of("user_name", "user_status")));
        components.add(textWithPath("user_name", "name", "body"));
        components.add(textWithPath("user_status", "status", "caption"));

        // Comments section
        components.add(builder.column("comments_section", List.of("comments_header", "comments_list", "add_comment")));
        components.add(builder.text("comments_header", "Comments", "subtitle"));
        components.add(templateList("comments_list", "/comments", "comment_item"));

        components.add(card("comment_item", List.of("comment_content")));
        components.add(builder.column("comment_content", List.of("comment_author", "comment_text", "comment_time")));
        components.add(textWithPath("comment_author", "author", "subtitle"));
        components.add(textWithPath("comment_text", "text", "body"));
        components.add(textWithPath("comment_time", "time", "caption"));

        components.add(builder.row("add_comment", List.of("comment_input", "send_btn")));
        components.add(textFieldWithPath("comment_input", "Add a comment...", "/newComment"));
        components.add(buttonWithContext("send_btn", "Send", "add_comment",
            List.of(contextPath("text", "/newComment"))));

        // Data
        List<Map<String, Object>> users = List.of(
            userMap("Alice Chen", "Editing hero section", "#4CAF50"),
            userMap("Bob Smith", "Reviewing", "#2196F3")
        );

        List<Map<String, Object>> comments = List.of(
            commentMap("Alice Chen", "Can we make the headline more impactful?", "2 minutes ago"),
            commentMap("Bob Smith", "Good idea! How about adding an emoji?", "Just now")
        );

        return List.of(
            builder.beginRendering(surfaceId, "root"),
            builder.surfaceUpdate(surfaceId, components),
            dataModelUpdate(surfaceId, "", List.of(
                builder.dataString("newComment", ""),
                valueList("users", users),
                valueList("comments", comments)
            ))
        );
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> textFieldWithPath(String id, String label, String path) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("value", builder.path(path));
        return builder.component(id, "TextField", props);
    }

    private Map<String, Object> textFieldMultiline(String id, String label, String path, int lines) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("value", builder.path(path));
        props.put("lines", lines);
        return builder.component(id, "TextField", props);
    }

    private Map<String, Object> textWithPath(String id, String path, String usageHint) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", builder.path(path));
        if (usageHint != null) props.put("usageHint", usageHint);
        return builder.component(id, "Text", props);
    }

    private Map<String, Object> imageWithPath(String id, String urlPath, String altPath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("url", builder.path(urlPath));
        props.put("altText", altPath.startsWith("/") ? builder.path(altPath) : builder.literalString(altPath));
        return builder.component(id, "Image", props);
    }

    private Map<String, Object> templateList(String id, String dataBinding, String componentId) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("dataBinding", dataBinding);
        template.put("componentId", componentId);

        Map<String, Object> children = new LinkedHashMap<>();
        children.put("template", template);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", children);

        return builder.component(id, "List", props);
    }

    private Map<String, Object> card(String id, List<String> childIds) {
        Map<String, Object> children = new LinkedHashMap<>();
        children.put("explicitList", childIds);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", children);

        return builder.component(id, "Card", props);
    }

    private Map<String, Object> multipleChoice(String id, String label, List<Map<String, Object>> options, String valuePath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("options", options);
        props.put("selectedValue", builder.path(valuePath));
        return builder.component(id, "MultipleChoice", props);
    }

    private Map<String, Object> option(String label, String value) {
        return Map.of(
            "label", builder.literalString(label),
            "value", value
        );
    }

    private Map<String, Object> slider(String id, String label, int min, int max, int step, String valuePath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("min", min);
        props.put("max", max);
        props.put("step", step);
        props.put("value", builder.path(valuePath));
        return builder.component(id, "Slider", props);
    }

    private Map<String, Object> checkbox(String id, String label, String checkedPath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("checked", builder.path(checkedPath));
        return builder.component(id, "CheckBox", props);
    }

    private Map<String, Object> icon(String id, String name, String colorPath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", builder.literalString(name));
        props.put("color", builder.path(colorPath));
        return builder.component(id, "Icon", props);
    }

    private Map<String, Object> tabs(String id, List<Map<String, Object>> tabList, String selectedPath) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("tabs", tabList);
        props.put("selectedIndex", builder.path(selectedPath));
        return builder.component(id, "Tabs", props);
    }

    private Map<String, Object> tab(String label, String contentId) {
        return Map.of(
            "label", builder.literalString(label),
            "content", contentId
        );
    }

    private Map<String, Object> buttonWithContext(String id, String label, String actionName, List<Map<String, Object>> context) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("name", actionName);
        action.put("context", context);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.literalString(label));
        props.put("action", action);

        return builder.component(id, "Button", props);
    }

    private Map<String, Object> buttonWithPathLabel(String id, String labelPath, String actionName) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("name", actionName);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", builder.path(labelPath));
        props.put("action", action);

        return builder.component(id, "Button", props);
    }

    private Map<String, Object> contextPath(String key, String path) {
        return Map.of("key", key, "valuePath", path);
    }

    private Map<String, Object> contextNumber(String key, int value) {
        return Map.of("key", key, "valueNumber", value);
    }

    private Map<String, Object> dataModelUpdate(String surfaceId, String path, List<Map<String, Object>> contents) {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("surfaceId", surfaceId);
        update.put("path", path);
        update.put("contents", contents);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("dataModelUpdate", update);
        return message;
    }

    private Map<String, Object> valueMap(String key, List<Map<String, Object>> contents) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueMap", contents);
        return entry;
    }

    private Map<String, Object> valueList(String key, List<Map<String, Object>> items) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueList", items);
        return entry;
    }

    private Map<String, Object> dataNumber(String key, Number value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueNumber", value);
        return entry;
    }

    private Map<String, Object> assetData(String path, String title, String thumbnail, String metadata) {
        return Map.of("valueMap", List.of(
            builder.dataString("path", path),
            builder.dataString("title", title),
            builder.dataString("thumbnail", thumbnail),
            builder.dataString("metadata", metadata)
        ));
    }

    private Map<String, Object> userMap(String name, String status, String color) {
        return Map.of("valueMap", List.of(
            builder.dataString("name", name),
            builder.dataString("status", status),
            builder.dataString("color", color)
        ));
    }

    private Map<String, Object> commentMap(String author, String text, String time) {
        return Map.of("valueMap", List.of(
            builder.dataString("author", author),
            builder.dataString("text", text),
            builder.dataString("time", time)
        ));
    }
}
