# AEM A2UI Demo - Claude Development Notes

This file documents key decisions, architecture choices, and updates made during development.

## Project Overview

An AI-powered content assistant for Adobe Experience Manager (AEM) using:
- **A2UI Protocol v0.8** - Google's Agent-to-User Interface protocol
- **Embabel Agent Framework** - Rod Johnson's AI agent framework for JVM
- **Multi-LLM Support** - OpenAI, Anthropic, and Ollama (local)
- **Adobe Spectrum** - Adobe's official design system for professional UI
- **Brand-Aware AI** - Content generation that follows brand guidelines

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Web Client (Lit)                        │
│  - Two-pane layout: Suggestions | Live Preview              │
│  - Component-based architecture                             │
│  - Copy JSON/HTML export                                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Java Agent (Spring Boot 3.2)                   │
│  - Embabel Agent Framework integration                      │
│  - Multi-LLM provider (OpenAI/Anthropic/Ollama)            │
│  - A2UI message generation                                  │
│  - Multiple variation generation                            │
└─────────────────────────────────────────────────────────────┘
```

## Key Technical Decisions

### 1. Embabel Framework Integration

**Problem**: `embabel-agent-starter` had bytecode incompatibility with Spring Boot's ASM library.

**Solution**: Use `embabel-agent-api` (v0.3.1) instead of the full starter.
- Provides annotations (@Agent, @Action, @AchievesGoal) without problematic auto-configuration
- Works cleanly with Spring Boot 3.2.0

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-api</artifactId>
    <version>0.3.1</version>
</dependency>
```

### 2. Multi-LLM Provider Architecture

**Design**: Pluggable LLM providers with graceful fallback.

```java
LlmService.generate(prompt) -> switch(provider) {
    case "openai" -> generateWithOpenAI(prompt);
    case "anthropic" -> generateWithAnthropic(prompt);
    case "ollama" -> generateWithOllama(prompt);
}
```

**Configuration** (via environment variables):
- `AI_ENABLED=true` - Enable AI mode
- `LLM_PROVIDER=ollama` - Choose provider
- `OLLAMA_MODEL=llama3.2` - Model selection

**Fallback**: When LLM fails or is disabled, falls back to template-based generation.

### 3. Client Architecture Refactoring

**Structure**:
```
client/src/
├── aem-assistant.ts      # Main component
├── spectrum-imports.ts   # Adobe Spectrum Web Components
├── components/
│   ├── assistant-header.ts
│   ├── assistant-input.ts
│   ├── assistant-preview.ts
│   ├── assistant-suggestions.ts
│   ├── content-wizard.ts    # Guided wizard (20 component types)
│   ├── brand-panel.ts       # Brand guidelines display
│   ├── brand-score.ts       # Content alignment scoring
│   ├── page-builder.ts      # Multi-page builder with drag-drop
│   ├── aem-preview.ts       # AEM authoring chrome preview
│   └── error-message.ts
├── data/
│   └── brand-config.json    # Brand guidelines configuration
├── lib/
│   └── types.ts
└── services/
    └── history-service.ts
```

**Features**:
- Adobe Spectrum design system integration
- Three input modes: Guided Wizard, Quick Mode, Page Builder
- 20 component types across 7 categories
- Brand guidelines panel with live status
- Brand alignment scoring for generated content
- Multi-page builder with drag-drop reordering
- AEM authoring preview with Edit/Preview/Structure modes
- Responsive device preview (Desktop/Tablet/Mobile)
- Copy to clipboard (JSON and HTML formats)
- Refinement input for iterating on suggestions

### 6. Intuitive Content Wizard

**Problem**: Users found the free-form text input unintuitive.

**Solution**: Added a guided 3-step wizard for content creation.

**Step 1 - Choose Type**: Visual cards with icons for each component type
- Hero Banner, Product Card, Teaser, Promo Banner
- Each card shows description and use case

**Step 2 - Customize**:
- Tone selector (Professional, Playful, Urgent, Elegant)
- Image style selector (Photography, Illustration, Abstract, Minimal)
- Description input with quick example chips

**Step 3 - Review & Generate**: Summary of selections before generation

**Implementation**:
```typescript
// content-wizard.ts
const COMPONENT_TYPES = [
  { id: 'hero', name: 'Hero Banner', icon: 'icon', description: '...' },
  // ...
];

const TONES = [
  { id: 'professional', name: 'Professional', icon: 'icon' },
  // ...
];
```

### 7. Inline Editing in Preview

**Feature**: Click-to-edit functionality in the live preview.

**Implementation**:
- Toggle "Edit" mode in preview header
- Click any text field (title, subtitle, description, CTA)
- Inline input appears for editing
- Press Enter to save, Escape to cancel
- Changes update the content model in real-time

```typescript
// assistant-preview.ts
private renderEditableField(field: string, value: string, tag: string) {
  if (this.editMode && this.editingField === field) {
    return html`<input class="editable-input" ... />`;
  }
  return html`<${tag} class="editable" @click=${...}>${value}</${tag}>`;
}
```

### 8. View Mode Toggle

**Design**: Users can switch between three input modes:
- **Guided Mode**: Step-by-step wizard for structured input
- **Quick Mode**: Free-form text input for experienced users
- **Page Builder Mode**: Multi-page layout builder with drag-drop

Toggle buttons at the top of the left panel allow instant switching.

### 13. Multi-Page Builder

**Feature**: Build complete page layouts with multiple components using drag-drop.

**Page Templates**:
- Landing Page: navigation → hero → 3 teasers → cta → footer
- Product Page: navigation → hero → product → tabs → quote → cta → footer
- Blog Article: navigation → hero → teaser → accordion → social → footer
- Custom Page: Start from scratch

**Implementation** (`page-builder.ts`):
```typescript
const PAGE_TEMPLATES = [
  { id: 'landing', name: 'Landing Page', icon: '🚀',
    sections: ['navigation', 'hero', 'teaser', 'teaser', 'teaser', 'cta', 'footer'] },
  // ...
];

// Drag and drop support
private handleDragStart(e: DragEvent, index: number)
private handleDrop(e: DragEvent, dropIndex: number)

// Generate all content in sequence
private async generateAllContent() {
  for (section of sections) {
    this.dispatchEvent(new CustomEvent('generate-section', {
      detail: { sectionId, componentType, prompt, pageContext }
    }));
  }
}
```

**Features**:
- 4 pre-built page templates
- 17 component types available
- Drag-and-drop section reordering
- Page description context for AI generation
- Sequential content generation with progress indicator

### 14. AEM Authoring Preview

**Feature**: Realistic AEM authoring environment simulation.

**View Modes**:
- **Preview**: Clean page preview
- **Edit**: Shows AEM component chrome with edit actions
- **Structure**: JCR content tree visualization

**Implementation** (`aem-preview.ts`):
```typescript
@customElement('aem-preview')
export class AemPreview extends LitElement {
  @property({ type: Array }) sections: PageSection[] = [];
  @property({ type: String }) viewMode: 'preview' | 'edit' | 'structure' = 'preview';
  @state() private deviceMode: 'desktop' | 'tablet' | 'mobile' = 'desktop';

  // AEM toolbar with logo, view modes, device toggle
  private renderToolbar()

  // Component with edit chrome showing path and actions
  private renderComponent(section: PageSection)

  // JCR tree structure view
  private renderStructureView()
}
```

**Features**:
- AEM-style dark toolbar with logo
- Preview/Edit/Structure mode toggle
- Desktop/Tablet/Mobile responsive preview
- Component chrome with edit/configure/delete actions
- JCR path display (/content/site/page/jcr:content/...)
- Publish button simulation

### 4. Multiple Variations Generation

**Approach**: Generate 3 variations per request with different styles:
1. Original (from user prompt)
2. "Bold and impactful" style
3. "Friendly and conversational" style

**Implementation**:
```java
SuggestionsResult generateMultipleSuggestions(String userInput, int count) {
    // Parse intent once
    UserInput parsed = contentAgent.parseUserIntent(userInput);

    // Generate variations with different styles
    for (style : styles) {
        variations.add(contentAgent.generateContent(variantInput));
    }

    return new SuggestionsResult(messages, artifacts);
}
```

### 5. Vite Configuration

**Issue**: `xdg-open ENOENT` errors in containerized environments.

**Solution**: Disable auto-browser opening:
```typescript
// vite.config.ts
server: {
    port: 5173,
    open: false,  // Prevents xdg-open errors in Docker
}
```

## Component Types (20 Total)

| Category | Components |
|----------|------------|
| **Marketing** | Hero Banner, Promo Banner, Carousel |
| **Content** | Teaser, Quote/Testimonial, Accordion, Tabs |
| **Commerce** | Product Card, Product List, Pricing Table |
| **Media** | Video Player, Image Gallery |
| **Navigation** | Navigation Menu, Footer, Breadcrumb |
| **Interactive** | Form, Search, Call to Action |
| **Social** | Social Share, Team Grid |

### Category Filtering
Users can filter components by category using chips at the top of the wizard.
Grid layout adapts (3 columns) with scrollable area for easy navigation.

### 9. Adobe Spectrum Design System

**Integration**: Full Adobe Spectrum Web Components for professional look.

```typescript
// spectrum-imports.ts
import '@spectrum-web-components/theme/sp-theme.js';
import '@spectrum-web-components/button/sp-button.js';
import '@spectrum-web-components/action-button/sp-action-button.js';
import '@spectrum-web-components/textfield/sp-textfield.js';
import '@spectrum-web-components/progress-circle/sp-progress-circle.js';
// ... more components
```

**Usage**:
- `<sp-theme>` wrapper for theming
- `<sp-button>` for actions
- `<sp-action-group>` for mode toggles
- Spectrum design tokens (--spectrum-*) for colors

### 10. Brand-Aware AI Content Generation

**Problem**: Generated content lacks brand consistency.

**Solution**: Comprehensive brand guidelines injected into every LLM prompt.

**Brand Config** (`client/src/data/brand-config.json`):
```json
{
  "brand": { "name": "Acme Corp", "tagline": "Innovation for Tomorrow" },
  "voice": {
    "tone": ["Professional", "Innovative", "Trustworthy"],
    "avoid": ["Jargon", "Passive voice", "Superlatives"]
  },
  "messaging": {
    "valuePillars": ["Speed & Efficiency", "Enterprise Security", "Seamless Integration"],
    "targetAudience": "Enterprise IT decision-makers"
  },
  "examples": {
    "goodHeadlines": ["Transform Your Workflow in Minutes", "Security That Scales With You"]
  }
}
```

**LLM Prompt Injection** (`AemContentAgent.java`):
```java
public static final String BRAND_GUIDELINES = """
    === BRAND GUIDELINES FOR ACME CORP ===
    BRAND VOICE: Professional, Innovative, Trustworthy
    WRITING RULES:
    - Headlines: Bold, concise, action-oriented (max 6 words)
    - Use action verbs: Transform, Discover, Unlock, Accelerate
    - AVOID: Jargon, passive voice, superlatives
    VALUE PILLARS: Speed & Efficiency, Enterprise Security, Seamless Integration
    === END BRAND GUIDELINES ===
    """;
```

### 11. Brand Alignment Scoring

**Feature**: Visual indicator showing how well content aligns with brand.

**Score Calculation** (`brand-score.ts`):
```typescript
public static calculateScore(content) {
  let score = 70; // Base score

  // Check for action-oriented headline
  if (actionWords.some(word => content.title.includes(word))) {
    score += 8;
    factors.push('Action-oriented headline');
  }

  // Check for value-focused messaging
  if (valueWords.some(word => content.description.includes(word))) {
    score += 7;
    factors.push('Value pillar messaging');
  }

  return { score, factors };
}
```

**Display**:
- 85%+ = "Excellent" (green)
- 65-84% = "Good" (yellow)
- <65% = "Needs Work" (red)
- Lists matched brand factors

### 12. Brand Panel

**Component**: Collapsible panel showing loaded brand guidelines.

**Features**:
- "Active & Loaded" status indicator
- Voice & tone tags
- Color swatches from brand config
- Value pillars list
- "Avoid" warnings
- Example headlines

## API Endpoints

### Java Agent (port 10003)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check |
| `/.well-known/agent-card.json` | GET | A2A agent discovery |
| `/tasks` | POST | Generate content suggestions |
| `/actions/{name}` | POST | Handle UI actions |

### Request Format
```json
{
  "message": {
    "role": "user",
    "parts": [{ "text": "Hero banner for summer sale" }]
  }
}
```

### Response Format
```json
{
  "id": "task-uuid",
  "status": "completed",
  "messages": [...],  // A2UI messages
  "artifacts": [...]  // Raw suggestion data
}
```

## Running the Demo

### Quick Start (Template Mode)
```bash
# Start Java agent
cd agent-java && mvn spring-boot:run

# Start client
cd client && npm run dev
```

### With Ollama (AI Mode)
```bash
# Start Ollama with a model
ollama run llama3.2

# Start Java agent with AI enabled
cd agent-java
AI_ENABLED=true LLM_PROVIDER=ollama mvn spring-boot:run

# Start client
cd client && npm run dev
```

## Testing

### Client Tests
```bash
cd client && npm test
```

### Java Tests
```bash
cd agent-java && mvn test
```

### Storybook (Component Development)
```bash
cd client && npm run storybook
```

## Value Proposition

1. **Speed**: Generate AEM content in seconds vs hours of manual creation
2. **Brand Consistency**: AI follows brand guidelines with real-time scoring
3. **Professional Design**: Adobe Spectrum design system for polished UI
4. **Flexibility**: 20 component types, multiple variations per request
5. **Export Ready**: Copy JSON/HTML directly into AEM

## Future Enhancements

- [ ] Direct AEM integration via Granite APIs
- [x] Brand voice training (implemented via brand-config.json)
- [x] Asset library integration (DAM browser with search, filter, and brand alignment)
- [ ] Workflow integration
- [ ] Multi-language support
- [ ] Custom brand config upload
- [ ] A/B testing for content variations

## Troubleshooting

### Rollup Module Error
```bash
rm -rf node_modules package-lock.json && npm install
```

### Embabel Bytecode Error
Use `embabel-agent-api` instead of `embabel-agent-starter`.

### Ollama Connection Failed
Ensure Ollama is running: `ollama serve`
