# A2UI Quick Start for AEM Developers

## Prerequisites

- AEM running on `localhost:4502` (or configured via `AEM_HOST`)
- Java 21+ and Maven
- Node.js 18+

## 1. Start the Backend

```bash
cd agent-java
mvn spring-boot:run
```

The Java agent will start on `http://localhost:8080`.

**With AI (Ollama for local generation):**

```bash
# First start Ollama
ollama run llama3.2

# Then start the agent with AI enabled
cd agent-java
AI_ENABLED=true LLM_PROVIDER=ollama mvn spring-boot:run
```

## 2. Start the Frontend

```bash
cd client
npm install
npm run dev
```

The client will start on `http://localhost:5173`.

## 3. Open http://localhost:5173

You'll see the A2UI sidebar with the assistant panel.

## Your First Request

1. In the text input box, type: **"Create a hero banner for our summer sale"**
2. Press Enter
3. Watch the brand score calculate in real-time
4. Review the 3 content variations generated
5. Click **[Apply]** to send the content to AEM

## What Just Happened?

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REQUEST FLOW DIAGRAM                                 │
└─────────────────────────────────────────────────────────────────────────────┘

   ┌──────────┐    "Create hero"    ┌──────────┐
   │  BROWSER │ ──────────────────► │  CLIENT  │
   └──────────┘                     └────┬─────┘
                                        │
                                        │ Build A2UI Request
                                        ▼
                              ┌─────────────────────┐
                              │  POST /tasks/run    │
                              └──────────┬──────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │  JAVA AGENT         │
                              │  • Load brand.json  │
                              │  • Generate content│
                              │  • Calculate score │
                              └──────────┬──────────┘
                                         │
                                         │ A2UI Response
                                         ▼
                              ┌─────────────────────┐
                              │  RENDER PREVIEW     │
                              │  • Hero component   │
                              │  • Brand score      │
                              │  • Action buttons   │
                              └──────────┬──────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │  APPLY TO AEM       │
                              │  POST /content/...  │
                              └─────────────────────┘
```

### Step-by-Step Breakdown

| Step | Component | What Happens |
|------|-----------|--------------|
| 1 | Browser | User types request in input field |
| 2 | Client | Builds A2UI JSON request with message |
| 3 | Java Agent | Receives request, loads brand guidelines |
| 4 | LLM/Templates | Generates 3 content variations |
| 5 | Brand Scorer | Evaluates each variation (0-100%) |
| 6 | Client | Renders interactive preview |
| 7 | User | Clicks [Apply] button |
| 8 | Agent | POSTs content to AEM endpoint |

## Try These Requests

| Request | What You Get |
|---------|--------------|
| `"Create a hero banner"` | Basic hero with title, subtitle, CTA |
| `"Product card for premium widget"` | Product card with image, price, description |
| `"Teaser for summer sale"` | Teaser component with countdown |
| `"Footer with social links"` | Footer with icon placeholders |

## Explore the UI

### DAM Browser

Click the **image icon** in any component preview to open the DAM browser. The agent will:
1. Query AEM's Sling JSON export for assets
2. Filter by brand visual guidelines
3. Display brand-aligned suggestions

### Brand Panel

Click **"Brand Guidelines"** to see:
- Voice and tone rules
- Value pillars
- Headline examples
- Content to avoid

### Workflow Submit

After applying content, click **[Submit for Review]** to:
1. POST content to AEM workflow
2. Initiate "Review & Approve" process
3. Track status in the UI

## Next Steps

- **Try variations:** Click "Bold" or "Friendly" to see alternative tones
- **Edit inline:** Click any text field to edit directly
- **Change brand:** Edit `client/src/data/brand-config.json` and restart
- **Add components:** See `PLAN.md` for planned features

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Client won't start | Run `npm install` first |
| Agent returns 500 | Check AEM is running on port 4502 |
| No brand score | Ensure `brand-config.json` exists |
| AI not generating | Verify Ollama is running |

## File Locations

```
aem-a2ui-demo/
├── agent-java/
│   ├── src/main/java/.../AemContentAgent.java   # Main agent logic
│   └── src/main/resources/brand-config.json     # Brand guidelines
├── client/
│   ├── src/aem-assistant.ts                     # Main component
│   ├── src/components/                          # UI components
│   └── src/data/brand-config.json               # Brand config (frontend)
└── docs/
    ├── A2UI_STORY.md                           # Narrative guide
    └── PROTOCOL.md                              # A2UI spec
```

## Learn More

- **[A2UI_STORY.md](docs/A2UI_STORY.md)** - Full narrative with diagrams
- **[CONCEPTS.md](CONCEPTS.md)** - AEM-to-A2UI concept mapping
- **[PLAN.md](PLAN.md)** - Future features and roadmap
