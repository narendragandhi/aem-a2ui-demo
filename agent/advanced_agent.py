"""
Advanced A2UI Agent demonstrating powerful features:
1. Dynamic template-based lists (data-driven UI)
2. Multi-step wizards (progressive disclosure)
3. Rich cards with multiple actions
4. Tabs for organized content
5. Forms with validation context
6. Real-time data updates
"""

import json
import uuid
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional
import asyncio

app = FastAPI(title="Advanced AEM A2UI Agent")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class MessagePart(BaseModel):
    text: Optional[str] = None


class Message(BaseModel):
    role: str = "user"
    parts: list[MessagePart]


class TaskRequest(BaseModel):
    message: Message
    context: Optional[dict] = None


# =============================================================================
# DEMO 1: Dynamic Template-Based List (Data-Driven UI)
# Shows how A2UI can render lists from data without hardcoding components
# =============================================================================

def demo_dynamic_list() -> list[dict]:
    """
    Demonstrates A2UI's template feature - define ONE card template,
    and it automatically repeats for each item in the data array.
    This is the killer feature for dynamic content!
    """
    surface_id = f"asset_browser_{uuid.uuid4().hex[:8]}"

    return [
        {
            "beginRendering": {
                "surfaceId": surface_id,
                "root": "root",
                "styles": {
                    "primaryColor": "#1473e6",
                    "borderRadius": "8px"
                }
            }
        },
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    # Root layout
                    {
                        "id": "root",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["header", "search_bar", "asset_list", "pagination"]}
                            }
                        }
                    },
                    # Header
                    {
                        "id": "header",
                        "component": {
                            "Text": {
                                "text": {"literalString": "DAM Asset Browser"},
                                "usageHint": "h1"
                            }
                        }
                    },
                    # Search bar
                    {
                        "id": "search_bar",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["search_input", "filter_btn"]}
                            }
                        }
                    },
                    {
                        "id": "search_input",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Search assets..."},
                                "value": {"path": "/search/query"}
                            }
                        }
                    },
                    {
                        "id": "filter_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Filter"},
                                "action": {"name": "open_filters"}
                            }
                        }
                    },
                    # === THE MAGIC: Template-based list ===
                    # This single component definition renders ALL items in /assets array
                    {
                        "id": "asset_list",
                        "component": {
                            "List": {
                                "children": {
                                    "template": {
                                        "dataBinding": "/assets",  # Points to array in data model
                                        "componentId": "asset_card"  # Template to repeat
                                    }
                                }
                            }
                        }
                    },
                    # Template card - rendered once per item in /assets
                    {
                        "id": "asset_card",
                        "component": {
                            "Card": {
                                "children": {"explicitList": ["card_content"]}
                            }
                        }
                    },
                    {
                        "id": "card_content",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["asset_thumb", "asset_info", "asset_actions"]}
                            }
                        }
                    },
                    {
                        "id": "asset_thumb",
                        "component": {
                            "Image": {
                                "url": {"path": "thumbnail"},  # Relative to current item
                                "altText": {"path": "title"}
                            }
                        }
                    },
                    {
                        "id": "asset_info",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["asset_title", "asset_meta"]}
                            }
                        }
                    },
                    {
                        "id": "asset_title",
                        "component": {
                            "Text": {
                                "text": {"path": "title"},
                                "usageHint": "subtitle"
                            }
                        }
                    },
                    {
                        "id": "asset_meta",
                        "component": {
                            "Text": {
                                "text": {"path": "metadata"},
                                "usageHint": "caption"
                            }
                        }
                    },
                    {
                        "id": "asset_actions",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["select_btn", "preview_btn"]}
                            }
                        }
                    },
                    {
                        "id": "select_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Select"},
                                "action": {
                                    "name": "select_asset",
                                    "context": [
                                        {"key": "path", "valuePath": "path"},
                                        {"key": "title", "valuePath": "title"}
                                    ]
                                }
                            }
                        }
                    },
                    {
                        "id": "preview_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Preview"},
                                "action": {
                                    "name": "preview_asset",
                                    "context": [
                                        {"key": "path", "valuePath": "path"}
                                    ]
                                }
                            }
                        }
                    },
                    # Pagination
                    {
                        "id": "pagination",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["prev_btn", "page_info", "next_btn"]}
                            }
                        }
                    },
                    {
                        "id": "prev_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Previous"},
                                "action": {"name": "prev_page"}
                            }
                        }
                    },
                    {
                        "id": "page_info",
                        "component": {
                            "Text": {
                                "text": {"path": "/pagination/info"},
                                "usageHint": "body"
                            }
                        }
                    },
                    {
                        "id": "next_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Next"},
                                "action": {"name": "next_page"}
                            }
                        }
                    }
                ]
            }
        },
        # Data model - the list renders from this array
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "path": "",
                "contents": [
                    {
                        "key": "search",
                        "valueMap": [
                            {"key": "query", "valueString": ""}
                        ]
                    },
                    {
                        "key": "pagination",
                        "valueMap": [
                            {"key": "info", "valueString": "Page 1 of 5 (47 assets)"}
                        ]
                    },
                    {
                        "key": "assets",
                        "valueList": [
                            {
                                "valueMap": [
                                    {"key": "path", "valueString": "/content/dam/mysite/hero-banner.jpg"},
                                    {"key": "title", "valueString": "Hero Banner"},
                                    {"key": "thumbnail", "valueString": "https://picsum.photos/120/80?random=1"},
                                    {"key": "metadata", "valueString": "JPG • 1920x1080 • 2.4 MB"}
                                ]
                            },
                            {
                                "valueMap": [
                                    {"key": "path", "valueString": "/content/dam/mysite/product-shot.jpg"},
                                    {"key": "title", "valueString": "Product Photography"},
                                    {"key": "thumbnail", "valueString": "https://picsum.photos/120/80?random=2"},
                                    {"key": "metadata", "valueString": "JPG • 2400x1600 • 3.1 MB"}
                                ]
                            },
                            {
                                "valueMap": [
                                    {"key": "path", "valueString": "/content/dam/mysite/team-photo.jpg"},
                                    {"key": "title", "valueString": "Team Photo"},
                                    {"key": "thumbnail", "valueString": "https://picsum.photos/120/80?random=3"},
                                    {"key": "metadata", "valueString": "JPG • 3000x2000 • 4.2 MB"}
                                ]
                            },
                            {
                                "valueMap": [
                                    {"key": "path", "valueString": "/content/dam/mysite/icon-set.svg"},
                                    {"key": "title", "valueString": "Icon Set"},
                                    {"key": "thumbnail", "valueString": "https://picsum.photos/120/80?random=4"},
                                    {"key": "metadata", "valueString": "SVG • Vector • 45 KB"}
                                ]
                            }
                        ]
                    }
                ]
            }
        }
    ]


# =============================================================================
# DEMO 2: Multi-Step Wizard (Progressive Disclosure)
# Shows how A2UI can guide users through complex workflows
# =============================================================================

def demo_wizard(step: int = 1) -> list[dict]:
    """
    Multi-step component configuration wizard.
    Each step reveals more options based on previous selections.
    """
    surface_id = f"wizard_{uuid.uuid4().hex[:8]}"

    steps = {
        1: {
            "title": "Step 1: Choose Component Type",
            "components": [
                {
                    "id": "step_content",
                    "component": {
                        "MultipleChoice": {
                            "label": {"literalString": "What type of component do you need?"},
                            "options": [
                                {"label": {"literalString": "Hero Banner"}, "value": "hero"},
                                {"label": {"literalString": "Content Teaser"}, "value": "teaser"},
                                {"label": {"literalString": "Product Card"}, "value": "product"},
                                {"label": {"literalString": "Call to Action"}, "value": "cta"}
                            ],
                            "selectedValue": {"path": "/wizard/componentType"}
                        }
                    }
                }
            ],
            "data": [
                {"key": "componentType", "valueString": ""}
            ]
        },
        2: {
            "title": "Step 2: Configure Layout",
            "components": [
                {
                    "id": "step_content",
                    "component": {
                        "Column": {
                            "children": {"explicitList": ["layout_choice", "alignment_choice"]}
                        }
                    }
                },
                {
                    "id": "layout_choice",
                    "component": {
                        "MultipleChoice": {
                            "label": {"literalString": "Layout Style"},
                            "options": [
                                {"label": {"literalString": "Full Width"}, "value": "full"},
                                {"label": {"literalString": "Contained"}, "value": "contained"},
                                {"label": {"literalString": "Split (50/50)"}, "value": "split"}
                            ],
                            "selectedValue": {"path": "/wizard/layout"}
                        }
                    }
                },
                {
                    "id": "alignment_choice",
                    "component": {
                        "MultipleChoice": {
                            "label": {"literalString": "Content Alignment"},
                            "options": [
                                {"label": {"literalString": "Left"}, "value": "left"},
                                {"label": {"literalString": "Center"}, "value": "center"},
                                {"label": {"literalString": "Right"}, "value": "right"}
                            ],
                            "selectedValue": {"path": "/wizard/alignment"}
                        }
                    }
                }
            ],
            "data": [
                {"key": "layout", "valueString": "contained"},
                {"key": "alignment", "valueString": "center"}
            ]
        },
        3: {
            "title": "Step 3: Add Content",
            "components": [
                {
                    "id": "step_content",
                    "component": {
                        "Column": {
                            "children": {"explicitList": ["title_input", "desc_input", "image_picker", "cta_input"]}
                        }
                    }
                },
                {
                    "id": "title_input",
                    "component": {
                        "TextField": {
                            "label": {"literalString": "Headline"},
                            "value": {"path": "/wizard/content/title"}
                        }
                    }
                },
                {
                    "id": "desc_input",
                    "component": {
                        "TextField": {
                            "label": {"literalString": "Description"},
                            "value": {"path": "/wizard/content/description"},
                            "lines": 3
                        }
                    }
                },
                {
                    "id": "image_picker",
                    "component": {
                        "Button": {
                            "label": {"literalString": "Select Image from DAM"},
                            "action": {"name": "open_dam_picker"}
                        }
                    }
                },
                {
                    "id": "cta_input",
                    "component": {
                        "TextField": {
                            "label": {"literalString": "Button Text"},
                            "value": {"path": "/wizard/content/ctaText"}
                        }
                    }
                }
            ],
            "data": [
                {
                    "key": "content",
                    "valueMap": [
                        {"key": "title", "valueString": "Discover Something Amazing"},
                        {"key": "description", "valueString": "Experience the difference with our premium solutions."},
                        {"key": "ctaText", "valueString": "Learn More"}
                    ]
                }
            ]
        }
    }

    current = steps.get(step, steps[1])

    # Build progress indicator
    progress_items = []
    for i in range(1, 4):
        status = "completed" if i < step else ("current" if i == step else "pending")
        progress_items.append(f"step_{i}_indicator")

    base_components = [
        {
            "id": "root",
            "component": {
                "Column": {
                    "children": {"explicitList": ["progress", "title", "step_content", "nav_buttons"]}
                }
            }
        },
        # Progress bar
        {
            "id": "progress",
            "component": {
                "Row": {
                    "children": {"explicitList": ["step_1_indicator", "step_2_indicator", "step_3_indicator"]}
                }
            }
        },
        {
            "id": "step_1_indicator",
            "component": {
                "Text": {
                    "text": {"literalString": f"{'✓' if step > 1 else '1'} Type"},
                    "usageHint": "caption"
                }
            }
        },
        {
            "id": "step_2_indicator",
            "component": {
                "Text": {
                    "text": {"literalString": f"{'✓' if step > 2 else '2'} Layout"},
                    "usageHint": "caption"
                }
            }
        },
        {
            "id": "step_3_indicator",
            "component": {
                "Text": {
                    "text": {"literalString": f"{'✓' if step > 3 else '3'} Content"},
                    "usageHint": "caption"
                }
            }
        },
        # Title
        {
            "id": "title",
            "component": {
                "Text": {
                    "text": {"literalString": current["title"]},
                    "usageHint": "h2"
                }
            }
        },
        # Navigation
        {
            "id": "nav_buttons",
            "component": {
                "Row": {
                    "children": {"explicitList": ["back_btn", "next_btn"] if step > 1 else ["next_btn"]}
                }
            }
        },
        {
            "id": "back_btn",
            "component": {
                "Button": {
                    "label": {"literalString": "Back"},
                    "action": {
                        "name": "wizard_navigate",
                        "context": [{"key": "step", "valueNumber": step - 1}]
                    }
                }
            }
        },
        {
            "id": "next_btn",
            "component": {
                "Button": {
                    "label": {"literalString": "Create Component" if step == 3 else "Next"},
                    "action": {
                        "name": "wizard_navigate" if step < 3 else "wizard_complete",
                        "context": [{"key": "step", "valueNumber": step + 1}]
                    }
                }
            }
        }
    ]

    all_components = base_components + current["components"]

    return [
        {"beginRendering": {"surfaceId": surface_id, "root": "root"}},
        {"surfaceUpdate": {"surfaceId": surface_id, "components": all_components}},
        {"dataModelUpdate": {
            "surfaceId": surface_id,
            "path": "wizard",
            "contents": current["data"]
        }}
    ]


# =============================================================================
# DEMO 3: Tabbed Content Experience
# Shows how A2UI can organize complex information
# =============================================================================

def demo_tabs() -> list[dict]:
    """
    Tabbed interface for component properties.
    Great for organizing complex configuration panels.
    """
    surface_id = f"tabs_{uuid.uuid4().hex[:8]}"

    return [
        {"beginRendering": {"surfaceId": surface_id, "root": "root"}},
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    {
                        "id": "root",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["header", "tab_container"]}
                            }
                        }
                    },
                    {
                        "id": "header",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Component Properties"},
                                "usageHint": "h2"
                            }
                        }
                    },
                    {
                        "id": "tab_container",
                        "component": {
                            "Tabs": {
                                "tabs": [
                                    {
                                        "label": {"literalString": "Content"},
                                        "content": "content_tab"
                                    },
                                    {
                                        "label": {"literalString": "Styling"},
                                        "content": "styling_tab"
                                    },
                                    {
                                        "label": {"literalString": "Advanced"},
                                        "content": "advanced_tab"
                                    }
                                ],
                                "selectedIndex": {"path": "/selectedTab"}
                            }
                        }
                    },
                    # Content Tab
                    {
                        "id": "content_tab",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["content_title", "content_desc", "content_image"]}
                            }
                        }
                    },
                    {
                        "id": "content_title",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Title"},
                                "value": {"path": "/content/title"}
                            }
                        }
                    },
                    {
                        "id": "content_desc",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Description"},
                                "value": {"path": "/content/description"},
                                "lines": 4
                            }
                        }
                    },
                    {
                        "id": "content_image",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["image_preview", "image_select"]}
                            }
                        }
                    },
                    {
                        "id": "image_preview",
                        "component": {
                            "Image": {
                                "url": {"path": "/content/imageUrl"},
                                "altText": {"literalString": "Selected image"}
                            }
                        }
                    },
                    {
                        "id": "image_select",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Change Image"},
                                "action": {"name": "select_image"}
                            }
                        }
                    },
                    # Styling Tab
                    {
                        "id": "styling_tab",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["style_theme", "style_spacing", "style_animation"]}
                            }
                        }
                    },
                    {
                        "id": "style_theme",
                        "component": {
                            "MultipleChoice": {
                                "label": {"literalString": "Color Theme"},
                                "options": [
                                    {"label": {"literalString": "Light"}, "value": "light"},
                                    {"label": {"literalString": "Dark"}, "value": "dark"},
                                    {"label": {"literalString": "Brand"}, "value": "brand"}
                                ],
                                "selectedValue": {"path": "/styling/theme"}
                            }
                        }
                    },
                    {
                        "id": "style_spacing",
                        "component": {
                            "Slider": {
                                "label": {"literalString": "Padding"},
                                "min": 0,
                                "max": 100,
                                "step": 10,
                                "value": {"path": "/styling/padding"}
                            }
                        }
                    },
                    {
                        "id": "style_animation",
                        "component": {
                            "CheckBox": {
                                "label": {"literalString": "Enable entrance animation"},
                                "checked": {"path": "/styling/animated"}
                            }
                        }
                    },
                    # Advanced Tab
                    {
                        "id": "advanced_tab",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["adv_id", "adv_analytics", "adv_cache"]}
                            }
                        }
                    },
                    {
                        "id": "adv_id",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Component ID"},
                                "value": {"path": "/advanced/componentId"}
                            }
                        }
                    },
                    {
                        "id": "adv_analytics",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Analytics Event Name"},
                                "value": {"path": "/advanced/analyticsEvent"}
                            }
                        }
                    },
                    {
                        "id": "adv_cache",
                        "component": {
                            "CheckBox": {
                                "label": {"literalString": "Enable dispatcher caching"},
                                "checked": {"path": "/advanced/cacheable"}
                            }
                        }
                    }
                ]
            }
        },
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "path": "",
                "contents": [
                    {"key": "selectedTab", "valueNumber": 0},
                    {
                        "key": "content",
                        "valueMap": [
                            {"key": "title", "valueString": "Welcome to Our Site"},
                            {"key": "description", "valueString": "Discover amazing content and experiences."},
                            {"key": "imageUrl", "valueString": "https://picsum.photos/400/200"}
                        ]
                    },
                    {
                        "key": "styling",
                        "valueMap": [
                            {"key": "theme", "valueString": "light"},
                            {"key": "padding", "valueNumber": 40},
                            {"key": "animated", "valueBoolean": True}
                        ]
                    },
                    {
                        "key": "advanced",
                        "valueMap": [
                            {"key": "componentId", "valueString": "hero-banner-1"},
                            {"key": "analyticsEvent", "valueString": "hero_view"},
                            {"key": "cacheable", "valueBoolean": True}
                        ]
                    }
                ]
            }
        }
    ]


# =============================================================================
# DEMO 4: AI Content Generation with Preview
# Shows the power of AI + A2UI for content authoring
# =============================================================================

def demo_ai_content_generator() -> list[dict]:
    """
    AI-powered content generator with live preview.
    Demonstrates the real value proposition of A2UI for AEM.
    """
    surface_id = f"ai_gen_{uuid.uuid4().hex[:8]}"

    return [
        {"beginRendering": {"surfaceId": surface_id, "root": "root"}},
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    {
                        "id": "root",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["header", "main_content"]}
                            }
                        }
                    },
                    {
                        "id": "header",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["title", "ai_badge"]}
                            }
                        }
                    },
                    {
                        "id": "title",
                        "component": {
                            "Text": {
                                "text": {"literalString": "AI Content Assistant"},
                                "usageHint": "h1"
                            }
                        }
                    },
                    {
                        "id": "ai_badge",
                        "component": {
                            "Text": {
                                "text": {"literalString": "✨ Powered by Gemini"},
                                "usageHint": "caption"
                            }
                        }
                    },
                    {
                        "id": "main_content",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["input_panel", "preview_panel"]}
                            }
                        }
                    },
                    # Input Panel
                    {
                        "id": "input_panel",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["prompt_section", "options_section", "generate_btn"]}
                            }
                        }
                    },
                    {
                        "id": "prompt_section",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["prompt_label", "prompt_input"]}
                            }
                        }
                    },
                    {
                        "id": "prompt_label",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Describe the content you need:"},
                                "usageHint": "subtitle"
                            }
                        }
                    },
                    {
                        "id": "prompt_input",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "E.g., 'Hero banner for summer sale with beach theme'"},
                                "value": {"path": "/input/prompt"},
                                "lines": 3
                            }
                        }
                    },
                    {
                        "id": "options_section",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["tone_select", "length_select"]}
                            }
                        }
                    },
                    {
                        "id": "tone_select",
                        "component": {
                            "MultipleChoice": {
                                "label": {"literalString": "Tone"},
                                "options": [
                                    {"label": {"literalString": "Professional"}, "value": "professional"},
                                    {"label": {"literalString": "Casual"}, "value": "casual"},
                                    {"label": {"literalString": "Playful"}, "value": "playful"},
                                    {"label": {"literalString": "Urgent"}, "value": "urgent"}
                                ],
                                "selectedValue": {"path": "/input/tone"}
                            }
                        }
                    },
                    {
                        "id": "length_select",
                        "component": {
                            "MultipleChoice": {
                                "label": {"literalString": "Content Length"},
                                "options": [
                                    {"label": {"literalString": "Short"}, "value": "short"},
                                    {"label": {"literalString": "Medium"}, "value": "medium"},
                                    {"label": {"literalString": "Long"}, "value": "long"}
                                ],
                                "selectedValue": {"path": "/input/length"}
                            }
                        }
                    },
                    {
                        "id": "generate_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "✨ Generate Content"},
                                "action": {
                                    "name": "generate_content",
                                    "context": [
                                        {"key": "prompt", "valuePath": "/input/prompt"},
                                        {"key": "tone", "valuePath": "/input/tone"},
                                        {"key": "length", "valuePath": "/input/length"}
                                    ]
                                }
                            }
                        }
                    },
                    # Preview Panel
                    {
                        "id": "preview_panel",
                        "component": {
                            "Card": {
                                "children": {"explicitList": ["preview_content"]}
                            }
                        }
                    },
                    {
                        "id": "preview_content",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["preview_image", "preview_title", "preview_desc", "preview_cta", "preview_actions"]}
                            }
                        }
                    },
                    {
                        "id": "preview_image",
                        "component": {
                            "Image": {
                                "url": {"path": "/preview/imageUrl"},
                                "altText": {"path": "/preview/title"}
                            }
                        }
                    },
                    {
                        "id": "preview_title",
                        "component": {
                            "Text": {
                                "text": {"path": "/preview/title"},
                                "usageHint": "h2"
                            }
                        }
                    },
                    {
                        "id": "preview_desc",
                        "component": {
                            "Text": {
                                "text": {"path": "/preview/description"},
                                "usageHint": "body"
                            }
                        }
                    },
                    {
                        "id": "preview_cta",
                        "component": {
                            "Button": {
                                "label": {"path": "/preview/ctaText"},
                                "action": {"name": "preview_cta_click"}
                            }
                        }
                    },
                    {
                        "id": "preview_actions",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["apply_btn", "regenerate_btn", "copy_btn"]}
                            }
                        }
                    },
                    {
                        "id": "apply_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Apply to Component"},
                                "action": {
                                    "name": "apply_content",
                                    "context": [
                                        {"key": "title", "valuePath": "/preview/title"},
                                        {"key": "description", "valuePath": "/preview/description"},
                                        {"key": "ctaText", "valuePath": "/preview/ctaText"},
                                        {"key": "imageUrl", "valuePath": "/preview/imageUrl"}
                                    ]
                                }
                            }
                        }
                    },
                    {
                        "id": "regenerate_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Try Again"},
                                "action": {"name": "regenerate"}
                            }
                        }
                    },
                    {
                        "id": "copy_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Copy to Clipboard"},
                                "action": {"name": "copy_content"}
                            }
                        }
                    }
                ]
            }
        },
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "path": "",
                "contents": [
                    {
                        "key": "input",
                        "valueMap": [
                            {"key": "prompt", "valueString": ""},
                            {"key": "tone", "valueString": "professional"},
                            {"key": "length", "valueString": "medium"}
                        ]
                    },
                    {
                        "key": "preview",
                        "valueMap": [
                            {"key": "title", "valueString": "Summer Sale Extravaganza"},
                            {"key": "description", "valueString": "Dive into savings with our biggest summer sale yet! Enjoy up to 50% off on selected items. Limited time offer - don't miss out on these sizzling deals!"},
                            {"key": "ctaText", "valueString": "Shop Now"},
                            {"key": "imageUrl", "valueString": "https://picsum.photos/600/300?random=summer"}
                        ]
                    }
                ]
            }
        }
    ]


# =============================================================================
# DEMO 5: Real-time Collaboration Indicator
# Shows how A2UI can handle dynamic updates
# =============================================================================

def demo_collaboration() -> list[dict]:
    """
    Real-time collaboration features.
    Shows presence, comments, and live updates.
    """
    surface_id = f"collab_{uuid.uuid4().hex[:8]}"

    return [
        {"beginRendering": {"surfaceId": surface_id, "root": "root"}},
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    {
                        "id": "root",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["header", "presence_bar", "comments_section"]}
                            }
                        }
                    },
                    {
                        "id": "header",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Collaboration Panel"},
                                "usageHint": "h2"
                            }
                        }
                    },
                    # Presence indicators
                    {
                        "id": "presence_bar",
                        "component": {
                            "Card": {
                                "children": {"explicitList": ["presence_content"]}
                            }
                        }
                    },
                    {
                        "id": "presence_content",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["presence_label", "presence_list"]}
                            }
                        }
                    },
                    {
                        "id": "presence_label",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Currently Editing"},
                                "usageHint": "caption"
                            }
                        }
                    },
                    {
                        "id": "presence_list",
                        "component": {
                            "List": {
                                "children": {
                                    "template": {
                                        "dataBinding": "/users",
                                        "componentId": "user_item"
                                    }
                                }
                            }
                        }
                    },
                    {
                        "id": "user_item",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["user_avatar", "user_info"]}
                            }
                        }
                    },
                    {
                        "id": "user_avatar",
                        "component": {
                            "Icon": {
                                "name": {"literalString": "person"},
                                "color": {"path": "color"}
                            }
                        }
                    },
                    {
                        "id": "user_info",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["user_name", "user_status"]}
                            }
                        }
                    },
                    {
                        "id": "user_name",
                        "component": {
                            "Text": {
                                "text": {"path": "name"},
                                "usageHint": "body"
                            }
                        }
                    },
                    {
                        "id": "user_status",
                        "component": {
                            "Text": {
                                "text": {"path": "status"},
                                "usageHint": "caption"
                            }
                        }
                    },
                    # Comments section
                    {
                        "id": "comments_section",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["comments_header", "comments_list", "add_comment"]}
                            }
                        }
                    },
                    {
                        "id": "comments_header",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Comments"},
                                "usageHint": "subtitle"
                            }
                        }
                    },
                    {
                        "id": "comments_list",
                        "component": {
                            "List": {
                                "children": {
                                    "template": {
                                        "dataBinding": "/comments",
                                        "componentId": "comment_item"
                                    }
                                }
                            }
                        }
                    },
                    {
                        "id": "comment_item",
                        "component": {
                            "Card": {
                                "children": {"explicitList": ["comment_content"]}
                            }
                        }
                    },
                    {
                        "id": "comment_content",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["comment_author", "comment_text", "comment_time"]}
                            }
                        }
                    },
                    {
                        "id": "comment_author",
                        "component": {
                            "Text": {
                                "text": {"path": "author"},
                                "usageHint": "subtitle"
                            }
                        }
                    },
                    {
                        "id": "comment_text",
                        "component": {
                            "Text": {
                                "text": {"path": "text"},
                                "usageHint": "body"
                            }
                        }
                    },
                    {
                        "id": "comment_time",
                        "component": {
                            "Text": {
                                "text": {"path": "time"},
                                "usageHint": "caption"
                            }
                        }
                    },
                    {
                        "id": "add_comment",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["comment_input", "send_btn"]}
                            }
                        }
                    },
                    {
                        "id": "comment_input",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Add a comment..."},
                                "value": {"path": "/newComment"}
                            }
                        }
                    },
                    {
                        "id": "send_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Send"},
                                "action": {
                                    "name": "add_comment",
                                    "context": [
                                        {"key": "text", "valuePath": "/newComment"}
                                    ]
                                }
                            }
                        }
                    }
                ]
            }
        },
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "path": "",
                "contents": [
                    {"key": "newComment", "valueString": ""},
                    {
                        "key": "users",
                        "valueList": [
                            {
                                "valueMap": [
                                    {"key": "name", "valueString": "Alice Chen"},
                                    {"key": "status", "valueString": "Editing hero section"},
                                    {"key": "color", "valueString": "#4CAF50"}
                                ]
                            },
                            {
                                "valueMap": [
                                    {"key": "name", "valueString": "Bob Smith"},
                                    {"key": "status", "valueString": "Reviewing"},
                                    {"key": "color", "valueString": "#2196F3"}
                                ]
                            }
                        ]
                    },
                    {
                        "key": "comments",
                        "valueList": [
                            {
                                "valueMap": [
                                    {"key": "author", "valueString": "Alice Chen"},
                                    {"key": "text", "valueString": "Can we make the headline more impactful?"},
                                    {"key": "time", "valueString": "2 minutes ago"}
                                ]
                            },
                            {
                                "valueMap": [
                                    {"key": "author", "valueString": "Bob Smith"},
                                    {"key": "text", "valueString": "Good idea! How about adding an emoji?"},
                                    {"key": "time", "valueString": "Just now"}
                                ]
                            }
                        ]
                    }
                ]
            }
        }
    ]


# =============================================================================
# API Endpoints
# =============================================================================

DEMOS = {
    "list": ("Dynamic Template List", demo_dynamic_list),
    "wizard": ("Multi-Step Wizard", lambda: demo_wizard(1)),
    "wizard_2": ("Wizard Step 2", lambda: demo_wizard(2)),
    "wizard_3": ("Wizard Step 3", lambda: demo_wizard(3)),
    "tabs": ("Tabbed Properties", demo_tabs),
    "ai": ("AI Content Generator", demo_ai_content_generator),
    "collab": ("Collaboration Panel", demo_collaboration),
}


@app.get("/")
async def root():
    return {
        "status": "ok",
        "name": "Advanced AEM A2UI Agent",
        "version": "2.0.0",
        "demos": {k: v[0] for k, v in DEMOS.items()}
    }


@app.get("/.well-known/agent-card.json")
async def agent_card():
    return {
        "name": "Advanced AEM Content Assistant",
        "description": "Advanced A2UI demos for AEM authoring",
        "url": "http://localhost:10004",
        "version": "2.0.0",
        "capabilities": {"a2ui": {"version": "0.8"}}
    }


@app.post("/tasks")
async def create_task(request: TaskRequest):
    user_text = ""
    for part in request.message.parts:
        if part.text:
            user_text += part.text.lower()

    # Select demo based on input
    if "list" in user_text or "asset" in user_text or "dam" in user_text or "browse" in user_text:
        messages = demo_dynamic_list()
    elif "wizard" in user_text or "step" in user_text or "create" in user_text:
        messages = demo_wizard(1)
    elif "tab" in user_text or "properties" in user_text or "config" in user_text:
        messages = demo_tabs()
    elif "ai" in user_text or "generate" in user_text or "content" in user_text:
        messages = demo_ai_content_generator()
    elif "collab" in user_text or "comment" in user_text or "team" in user_text:
        messages = demo_collaboration()
    else:
        # Default to AI content generator
        messages = demo_ai_content_generator()

    return {
        "id": str(uuid.uuid4()),
        "status": "completed",
        "messages": messages
    }


@app.get("/demo/{demo_name}")
async def get_demo(demo_name: str):
    """Direct access to specific demos."""
    if demo_name in DEMOS:
        return {
            "id": str(uuid.uuid4()),
            "status": "completed",
            "messages": DEMOS[demo_name][1]()
        }
    return {"error": f"Unknown demo: {demo_name}", "available": list(DEMOS.keys())}


@app.post("/actions/{action_name}")
async def handle_action(action_name: str, context: dict = None):
    """Handle user actions."""
    if action_name == "wizard_navigate":
        step = context.get("step", 1) if context else 1
        return {
            "success": True,
            "messages": demo_wizard(step)
        }
    elif action_name == "generate_content":
        # Simulate AI generation
        return {
            "success": True,
            "messages": demo_ai_content_generator()
        }
    return {"success": True, "action": action_name, "context": context}


if __name__ == "__main__":
    import uvicorn
    print("Starting Advanced AEM A2UI Agent on http://localhost:10004")
    print("\nAvailable demos:")
    for key, (name, _) in DEMOS.items():
        print(f"  - {key}: {name}")
    print("\nTry: curl -X POST http://localhost:10004/tasks -H 'Content-Type: application/json' -d '{\"message\":{\"parts\":[{\"text\":\"show me the asset browser\"}]}}'")
    uvicorn.run(app, host="0.0.0.0", port=10004)
