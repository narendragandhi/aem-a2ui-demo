# AEM A2UI Demo

An AI-powered content assistant for Adobe Experience Manager (AEM) that demonstrates the power of a "Brand-Aware AI" to generate on-brand content.

## Project Overview

This project is a sophisticated prototype of an AI-powered authoring assistant for Adobe Experience Manager (AEM). It showcases a "Brand-Aware AI" capable of generating content that strictly adheres to predefined brand guidelines.

**Core Technologies:**

- **A2UI Protocol v0.8:** Google's Agent-to-User Interface protocol for generating rich, interactive UIs.
- **Embabel Agent Framework:** A powerful AI agent framework for the JVM.
- **Multi-LLM Support:** Integrates with OpenAI, Anthropic, and Ollama (for local development).
- **Adobe Spectrum:** Adobe's official design system for a professional and intuitive UI.

## Key Features

### Brand Alignment Scoring (Textual & Visual)

To provide authors with immediate feedback, the application includes a comprehensive brand alignment scoring system. This feature analyzes generated content and provides a score indicating how well it aligns with the brand guidelines, along with a list of factors that contributed to the score. This now includes both textual and visual alignment.

### Visual Brand Alignment

The AI now considers visual brand guidelines when suggesting images. The `brand-config.json` includes `visuals` properties (e.g., `styleKeywords`, `brandColors`). The AI selects images from a predefined library that best match these visual cues. In the live preview, a **"Brand Aligned"** or **"Review Visual"** badge appears on images, indicating their adherence to the brand's visual style.

### SEO Optimization

A new SEO analysis panel provides real-time feedback on the generated content's search engine optimization. For each generated content piece, the panel displays:

-   An overall **SEO Score**.
-   **Suggested Meta Title and Meta Description** based on the content.
-   **Recommended Keywords** relevant to the component type and content.
-   A **Readability Score** to ensure content is easy to digest.
-   Identified **Issues** (e.g., sub-optimal title length, missing keywords).

This feature empowers authors to create content that is not only on-brand but also optimized for search engines.

### Brand-Aware AI

The cornerstone of this demo is its "Brand-Aware AI." The AI's content generation is guided by a `brand-config.json` file, which defines the brand's voice, tone, messaging pillars, and includes examples of on-brand content. This ensures that all generated content is consistent with the brand's identity.

### Advanced Client-Side Features

The client is a feature-rich application built with Adobe's Spectrum design system and includes:

- **Content Wizard:** A guided, 3-step process for creating content.
- **Page Builder:** A multi-page builder with drag-and-drop functionality for creating complex layouts.
- **AEM Authoring Preview:** A realistic simulation of the AEM authoring environment, complete with Edit, Preview, and Structure modes.
- **Multiple Input Modes:** Switch between a guided wizard, a quick-entry text field, and a full page builder.
- **Inline Editing:** Click to edit text directly in the live preview.
- **Component Library:** A rich library of 20 component types across 7 categories.

### Sophisticated Agent Backend

The Java-based agent leverages the Embabel AI framework and provides features such as:

- **Multiple Variation Generation:** Generates three variations for each content request: "original," "bold and impactful," and "friendly and conversational."
- **Pluggable LLM Providers:** Easily switch between different LLM providers with graceful fallback to template-based generation.

## Project Structure

```
aem-a2ui-demo/
├── README.md                 # This file
├── agent/                    # Python agent (FastAPI) - Basic Demo
├── agent-java/               # Java agent (Spring Boot) - Advanced Features
└── client/                   # Web client (Lit + Adobe Spectrum)
```

## Getting Started

### Prerequisites

- Java 21+ and Maven (for the Java agent)
- Node.js 18+ (for the client)
- (Optional) Ollama for local AI generation

### 1. Start the Java Agent

**Without AI (Template-based):**

```bash
cd agent-java
mvn spring-boot:run
```

**With AI (using Ollama):**

First, ensure Ollama is running and has a model available (e.g., `ollama run llama3.2`).

```bash
cd agent-java
AI_ENABLED=true LLM_PROVIDER=ollama mvn spring-boot:run
```

### 2. Start the Client

```bash
cd client
npm install
npm run dev
```

### 3. Open in Browser

Navigate to `http://localhost:5173` to see the application in action.

## Future Enhancements

- Direct AEM integration via Granite APIs
- Integration with AEM's asset library
- Workflow integration for content approval
- Multi-language support
- A UI for managing brand configurations

## License

Apache 2.0