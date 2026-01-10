# AEM A2UI Demo

A working demonstration of Google's [A2UI (Agent-to-User Interface)](https://a2ui.org/) protocol integrated with Adobe Experience Manager (AEM) authoring.

## What is A2UI?

A2UI is an open-source protocol from Google that allows AI agents to generate rich, interactive user interfaces through declarative JSON. Key benefits:

- **Safe**: Declarative format, not executable code - agents can only use pre-approved components
- **Framework-agnostic**: Same payload renders on React, Angular, Flutter, Lit, etc.
- **LLM-friendly**: Flat JSON structure designed for easy generation by language models
- **Progressive rendering**: Stream UI updates as they're generated

## Project Structure

```
aem-a2ui-demo/
├── README.md                 # This file
├── agent/                    # Python agent (FastAPI)
│   ├── simple_agent.py
│   └── pyproject.toml
├── agent-java/               # Java agent (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/...
└── client/                   # Web client (Lit + @a2ui/lit)
    ├── src/aem-assistant.ts
    ├── index.html
    └── package.json
```

## Quick Start

### Prerequisites

- Python 3.10+ with `uv` (for Python agent)
- Java 21+ with Maven (for Java agent with Embabel)
- Node.js 18+ (for client)

### 1. Start an Agent

**Option A: Python Agent (port 10002)**

```bash
cd agent
uv venv && uv pip install fastapi uvicorn pydantic
source .venv/bin/activate
python simple_agent.py
```

**Option B: Java Agent (port 10003)**

```bash
cd agent-java
mvn spring-boot:run
```

### 2. Start the Client

```bash
cd client
npm install
npm run dev
```

### 3. Open in Browser

Navigate to http://localhost:5173

Enter prompts like:
- "hero banner"
- "product card"
- "teaser"

## How It Works

### A2UI Message Flow

```
┌─────────────┐     POST /tasks      ┌─────────────┐
│   Client    │ ──────────────────▶  │    Agent    │
│  (Lit Web)  │                      │ (Python/Java)│
│             │  ◀──────────────────  │             │
└─────────────┘   A2UI JSON Messages └─────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│  @a2ui/lit Renderer                             │
│  - Parses beginRendering, surfaceUpdate,        │
│    dataModelUpdate messages                     │
│  - Builds component tree                        │
│  - Renders native Lit components                │
└─────────────────────────────────────────────────┘
```

### A2UI Message Types

The agent returns three message types:

#### 1. beginRendering
Signals the client to start rendering a surface.

```json
{
  "beginRendering": {
    "surfaceId": "suggestion_abc123",
    "root": "root"
  }
}
```

#### 2. surfaceUpdate
Defines the component tree structure.

```json
{
  "surfaceUpdate": {
    "surfaceId": "suggestion_abc123",
    "components": [
      {
        "id": "root",
        "component": {
          "Column": {
            "children": { "explicitList": ["header", "form"] }
          }
        }
      },
      {
        "id": "header",
        "component": {
          "Text": {
            "text": { "literalString": "Content Suggestion" },
            "usageHint": "h2"
          }
        }
      }
    ]
  }
}
```

#### 3. dataModelUpdate
Populates dynamic data that components bind to.

```json
{
  "dataModelUpdate": {
    "surfaceId": "suggestion_abc123",
    "path": "suggestion",
    "contents": [
      { "key": "title", "valueString": "Welcome" },
      { "key": "description", "valueString": "Hello world" }
    ]
  }
}
```

### Data Binding

Components reference data using JSON Pointer paths:

```json
{
  "TextField": {
    "label": { "literalString": "Title" },
    "value": { "path": "/suggestion/title" }
  }
}
```

The `path` references data from the `dataModelUpdate` message.

## Available Components

The A2UI standard catalog includes:

| Component | Description |
|-----------|-------------|
| `Text` | Text display with usageHint (h1, h2, body, etc.) |
| `Image` | Image display with url and altText |
| `Button` | Clickable button with action |
| `TextField` | Text input field |
| `Column` | Vertical layout container |
| `Row` | Horizontal layout container |
| `Card` | Card container |
| `Divider` | Visual separator |
| `Checkbox` | Boolean input |
| `Slider` | Numeric range input |
| `DateTimeInput` | Date/time picker |
| `MultipleChoice` | Selection from options |

## AEM Integration

### Embedding in AEM Authoring

To embed the A2UI assistant in AEM's authoring interface:

1. **Build the client**
   ```bash
   cd client
   npm run build
   ```

2. **Create AEM clientlib**
   ```
   ui.apps/src/main/content/jcr_root/apps/mysite/clientlibs/assistant/
   ├── .content.xml
   ├── js.txt
   └── js/
       └── aem-assistant.js  # Built output
   ```

3. **Register in authoring** (js/init.js)
   ```javascript
   (function(document, Granite, $) {
     $(document).on('cq-layer-activated', function(e) {
       if (e.layer === 'Edit') {
         const assistant = document.createElement('aem-assistant');
         assistant.agentUrl = '/api/a2ui-agent';
         document.querySelector('.sidepanel-content').appendChild(assistant);
       }
     });
   })(document, Granite, jQuery);
   ```

### Applying Suggestions to Components

The client's `handleAction` method shows how to apply suggestions:

```javascript
if (actionName === 'apply_suggestion') {
  // Get the current editable
  const editable = Granite.author.editables.getActive();

  // Apply properties via Granite API
  Granite.author.edit.EditableActions.CONFIGURE.execute(editable, {
    'jcr:title': suggestionData.title,
    'jcr:description': suggestionData.description,
    'fileReference': suggestionData.imageUrl
  });
}
```

## API Reference

### Agent Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check |
| `/.well-known/agent-card.json` | GET | A2A agent discovery |
| `/tasks` | POST | Generate A2UI response |
| `/actions/{name}` | POST | Handle user actions |

### Task Request

```json
{
  "message": {
    "role": "user",
    "parts": [{ "text": "hero banner" }]
  },
  "context": {
    "componentPath": "/content/mysite/page/jcr:content/root/container/teaser"
  }
}
```

### Task Response

```json
{
  "id": "uuid",
  "status": "completed",
  "messages": [
    { "beginRendering": { ... } },
    { "surfaceUpdate": { ... } },
    { "dataModelUpdate": { ... } }
  ]
}
```

## Extending the Agent

### Adding LLM Integration (Python)

```python
from google.adk.agents import LlmAgent

agent = LlmAgent(
    model="gemini/gemini-2.5-flash",
    name="aem_content_assistant",
    instruction="""Generate A2UI JSON for content suggestions.
    Response format: [text]---a2ui_JSON---[json array]""",
    tools=[get_component_context, search_dam]
)
```

### Adding LLM Integration (Java with Embabel)

The Java agent includes [Embabel](https://embabel.com) AI agent framework integration. Embabel is Rod Johnson's (creator of Spring) AI agent framework for the JVM.

```java
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;

@Agent(description = "AI agent that generates content suggestions for AEM components")
public class AemContentAgent {

    @Action
    public UserInput parseUserIntent(String rawInput) {
        // Parse user intent using LLM or templates
    }

    @AchievesGoal(description = "Generate content suggestion for AEM component")
    @Action
    public ContentSuggestion generateContent(UserInput input) {
        // Generate content using LLM or templates
    }
}
```

**Current Status:**
- Uses `embabel-agent-api` 0.3.1 for agent annotations
- Template-based content generation (fallback)
- Ready for full LLM integration when API keys are configured

**To enable AI-powered generation:**
1. Set `OPENAI_API_KEY` or `ANTHROPIC_API_KEY` environment variable
2. Set `AI_ENABLED=true` in `.env`
3. The agent will use LLM prompts defined in `AemContentAgent.java`

## Advanced Demos

The `advanced_agent.py` showcases A2UI's most powerful features:

### 1. Dynamic Template Lists (Data-Driven UI)

**The killer feature!** Define ONE template component, and A2UI automatically repeats it for each item in a data array.

```json
{
  "id": "asset_list",
  "component": {
    "List": {
      "children": {
        "template": {
          "dataBinding": "/assets",      // Points to array in data model
          "componentId": "asset_card"    // Template to repeat for each item
        }
      }
    }
  }
}
```

**Why this matters for AEM:**
- Render DAM search results dynamically
- Display content fragments without hardcoding
- Build data-driven component galleries

### 2. Multi-Step Wizards (Progressive Disclosure)

Guide authors through complex workflows step-by-step:

```
Step 1: Choose Component Type  →  Step 2: Configure Layout  →  Step 3: Add Content
   [Hero/Teaser/Product]           [Full/Split/Contained]        [Title/Desc/Image]
```

**Why this matters for AEM:**
- Simplify component configuration
- Enforce content governance rules
- Reduce author training time

### 3. Tabbed Interfaces

Organize complex properties without overwhelming authors:

```
┌─────────┬──────────┬──────────┐
│ Content │ Styling  │ Advanced │
├─────────┴──────────┴──────────┤
│  Title: [____________]        │
│  Description: [______]        │
│  Image: [Select...]           │
└───────────────────────────────┘
```

### 4. AI Content Generator with Live Preview

```
┌─────────────────────┬─────────────────────┐
│  INPUT PANEL        │  LIVE PREVIEW       │
│                     │                     │
│  Prompt: [_____]    │  ┌─────────────┐   │
│  Tone: Professional │  │   IMAGE     │   │
│  Length: Medium     │  │             │   │
│                     │  │  Headline   │   │
│  [✨ Generate]      │  │  Body text  │   │
│                     │  │  [CTA]      │   │
│                     │  └─────────────┘   │
│                     │                     │
│                     │  [Apply] [Retry]    │
└─────────────────────┴─────────────────────┘
```

### 5. Real-time Collaboration

Show who's editing and enable comments:

```
Currently Editing:
  🟢 Alice Chen - Editing hero section
  🔵 Bob Smith - Reviewing

Comments:
  Alice: "Can we make the headline more impactful?"
  Bob: "Good idea! How about adding an emoji?"

  [Add a comment...] [Send]
```

### Running Advanced Demos

```bash
# Start advanced agent (port 10004)
cd agent
source .venv/bin/activate
python advanced_agent.py

# Test specific demos
curl http://localhost:10004/demo/list      # DAM browser
curl http://localhost:10004/demo/wizard    # Multi-step wizard
curl http://localhost:10004/demo/tabs      # Tabbed properties
curl http://localhost:10004/demo/ai        # AI generator
curl http://localhost:10004/demo/collab    # Collaboration

# Or via natural language
curl -X POST http://localhost:10004/tasks \
  -H "Content-Type: application/json" \
  -d '{"message":{"parts":[{"text":"show me the asset browser"}]}}'
```

## Value Proposition of A2UI for AEM

| Traditional Approach | A2UI Approach |
|---------------------|---------------|
| Build custom React/Angular dialogs | Agent generates UI dynamically |
| Hardcode every component variation | One template serves all data |
| Deploy clientlib for each feature | Update agent, UI updates instantly |
| Limited to predefined workflows | AI adapts UI to context |
| Static forms | Progressive, conversational UX |

### Key Benefits

1. **Faster Iteration**: Change agent prompts, not frontend code
2. **Consistent UX**: Standard catalog ensures design system compliance
3. **AI-Native**: LLMs can generate valid UI without custom training
4. **Safe**: Declarative JSON, no code execution risks
5. **Cross-Platform**: Same agent works on web, mobile, desktop

## Resources

- [A2UI Official Site](https://a2ui.org/)
- [A2UI GitHub](https://github.com/google/A2UI)
- [A2UI Specification v0.8](https://a2ui.org/specification/v0.8-a2ui/)
- [Google ADK (Agent Development Kit)](https://google.github.io/adk-docs/)
- [@a2ui/lit npm package](https://www.npmjs.com/package/@a2ui/lit)
- [Embabel AI Agent Framework](https://embabel.com) - JVM agent framework by Rod Johnson

## License

Apache 2.0
