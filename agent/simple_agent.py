"""
Simple A2UI agent that returns static content suggestions.
This doesn't require a Gemini API key - it's for testing the A2UI format.
"""

import json
import uuid
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional

app = FastAPI(title="AEM Content Assistant")

# Enable CORS for local development
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


def generate_a2ui_response(user_input: str) -> list[dict]:
    """Generate A2UI messages based on user input."""

    # Generate suggestion based on input
    if "hero" in user_input.lower():
        title = "Unleash Your Potential"
        description = "Discover innovative solutions designed to transform your digital experience."
        image = "https://picsum.photos/800/400"
    elif "product" in user_input.lower():
        title = "Premium Quality Products"
        description = "Crafted with precision and care for the discerning customer."
        image = "https://picsum.photos/800/400?random=2"
    else:
        title = "Welcome to Our World"
        description = "Experience excellence in everything we do. Your journey starts here."
        image = "https://picsum.photos/800/400?random=3"

    surface_id = f"suggestion_{uuid.uuid4().hex[:8]}"

    return [
        {
            "beginRendering": {
                "surfaceId": surface_id,
                "root": "root"
            }
        },
        {
            "surfaceUpdate": {
                "surfaceId": surface_id,
                "components": [
                    {
                        "id": "root",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["header", "preview", "form", "actions"]}
                            }
                        }
                    },
                    {
                        "id": "header",
                        "component": {
                            "Text": {
                                "text": {"literalString": "Content Suggestion"},
                                "usageHint": "h2"
                            }
                        }
                    },
                    {
                        "id": "preview",
                        "component": {
                            "Image": {
                                "url": {"path": "/suggestion/imageUrl"},
                                "altText": {"literalString": "Preview image"}
                            }
                        }
                    },
                    {
                        "id": "form",
                        "component": {
                            "Column": {
                                "children": {"explicitList": ["title_field", "desc_field"]}
                            }
                        }
                    },
                    {
                        "id": "title_field",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Title"},
                                "value": {"path": "/suggestion/title"}
                            }
                        }
                    },
                    {
                        "id": "desc_field",
                        "component": {
                            "TextField": {
                                "label": {"literalString": "Description"},
                                "value": {"path": "/suggestion/description"},
                                "lines": 3
                            }
                        }
                    },
                    {
                        "id": "actions",
                        "component": {
                            "Row": {
                                "children": {"explicitList": ["apply_btn", "regenerate_btn"]}
                            }
                        }
                    },
                    {
                        "id": "apply_btn",
                        "component": {
                            "Button": {
                                "label": {"literalString": "Apply to Component"},
                                "action": {"name": "apply_suggestion"}
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
                    }
                ]
            }
        },
        {
            "dataModelUpdate": {
                "surfaceId": surface_id,
                "path": "suggestion",
                "contents": [
                    {"key": "title", "valueString": title},
                    {"key": "description", "valueString": description},
                    {"key": "imageUrl", "valueString": image}
                ]
            }
        }
    ]


@app.get("/")
async def root():
    return {"status": "ok", "name": "AEM Content Assistant", "version": "0.1.0"}


@app.get("/.well-known/agent-card.json")
async def agent_card():
    """A2A agent card for discovery."""
    return {
        "name": "AEM Content Assistant",
        "description": "AI assistant for AEM content authoring",
        "url": "http://localhost:10002",
        "version": "0.1.0",
        "capabilities": {
            "a2ui": {"version": "0.8"}
        }
    }


@app.post("/tasks")
async def create_task(request: TaskRequest):
    """Handle A2A task requests and return A2UI messages."""
    user_text = ""
    for part in request.message.parts:
        if part.text:
            user_text += part.text

    messages = generate_a2ui_response(user_text)

    return {
        "id": str(uuid.uuid4()),
        "status": "completed",
        "messages": messages
    }


@app.post("/actions/{action_name}")
async def handle_action(action_name: str, context: dict = None):
    """Handle user actions from the UI."""
    if action_name == "apply_suggestion":
        return {"success": True, "message": "Content applied to component"}
    elif action_name == "regenerate":
        return {"success": True, "messages": generate_a2ui_response("random")}
    return {"success": False, "message": f"Unknown action: {action_name}"}


if __name__ == "__main__":
    import uvicorn
    print("Starting AEM Content Assistant on http://localhost:10002")
    print("Agent card: http://localhost:10002/.well-known/agent-card.json")
    uvicorn.run(app, host="0.0.0.0", port=10002)
