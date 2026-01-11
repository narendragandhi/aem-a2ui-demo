# AEM A2UI Demo - Claude Development Notes

This file documents key decisions, architecture choices, and updates made during development.

## Project Overview

An AI-powered content assistant for Adobe Experience Manager (AEM) using:
- **A2UI Protocol v0.8** - Google's Agent-to-User Interface protocol
- **Embabel Agent Framework** - Rod Johnson's AI agent framework for JVM
- **Multi-LLM Support** - OpenAI, Anthropic, and Ollama (local)

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
├── components/           # Reusable UI components
│   ├── assistant-header.ts
│   ├── assistant-input.ts
│   ├── assistant-preview.ts
│   ├── assistant-suggestions.ts
│   └── error-message.ts
├── lib/
│   └── types.ts          # TypeScript interfaces
└── services/
    └── history-service.ts
```

**Features**:
- Two-pane layout (suggestions left, preview right)
- 4 component types: Hero, Product, Teaser, Banner
- Copy to clipboard (JSON and HTML formats)
- Refinement input for iterating on suggestions
- Quick prompt buttons for common use cases

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

## Component Types

| Type | Use Case | Visual |
|------|----------|--------|
| Hero | Landing page banners | Full-width image with overlay text |
| Product | E-commerce cards | Image + price + CTA |
| Teaser | Content previews | Side-by-side image + text |
| Banner | Announcements | Gradient background + CTA |

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
2. **Consistency**: AI-generated content follows brand guidelines
3. **Flexibility**: Multiple variations to choose from
4. **Export Ready**: Copy JSON/HTML directly into AEM

## Future Enhancements

- [ ] Direct AEM integration via Granite APIs
- [ ] Brand voice training
- [ ] Asset library integration
- [ ] Workflow integration
- [ ] Multi-language support

## Troubleshooting

### Rollup Module Error
```bash
rm -rf node_modules package-lock.json && npm install
```

### Embabel Bytecode Error
Use `embabel-agent-api` instead of `embabel-agent-starter`.

### Ollama Connection Failed
Ensure Ollama is running: `ollama serve`
