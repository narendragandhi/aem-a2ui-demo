import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import './components/assistant-header.js';
import './components/assistant-input.js';
import './components/assistant-suggestions.js';
import './components/assistant-preview.js';
import './components/error-message.js';

import { ContentSuggestion } from './lib/types.js';
import { HistoryService } from './services/history-service.js';

const AGENTS = [
  { name: 'Java Agent + Ollama', url: 'http://localhost:10003', port: 10003, hasAI: true },
  { name: 'Python Agent', url: 'http://localhost:10002', port: 10002, hasAI: false },
];

@customElement('aem-assistant')
export class AemAssistant extends LitElement {
  @property({ type: String }) agentUrl = 'http://localhost:10003';

  @state() private loading = false;
  @state() private selectedAgent = AGENTS[0];
  @state() private error = '';
  @state() private suggestions: ContentSuggestion[] = [];
  @state() private selectedSuggestion: ContentSuggestion | null = null;
  @state() private appliedContent: ContentSuggestion | null = null;
  @state() public prompt = '';
  @state() private showCopiedToast = false;
  @state() private refinementMode = false;
  @state() private history: ContentSuggestion[] = [];
  @state() private theme: 'light' | 'dark' = 'light';

  static styles = css`
    :host {
      display: block;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      height: 100vh;
      overflow: hidden;
      background-color: var(--background-color);
      color: var(--text-color);
    }

    :root {
      --primary-color: #1473e6;
      --primary-hover-color: #0d66d0;
      --text-color: #333;
      --text-color-light: #666;
      --background-color: #f5f5f5;
      --card-background: white;
      --border-color: #e0e0e0;
      --header-bg: linear-gradient(135deg, #1473e6 0%, #0d66d0 100%);
      --header-text: white;
      --input-bg: white;
      --input-border: #e0e0e0;
      --input-focus-shadow: rgba(20, 115, 230, 0.1);
      --button-primary-bg: #1473e6;
      --button-primary-hover-bg: #0d66d0;
      --button-secondary-bg: #f0f0f0;
      --button-secondary-hover-bg: #e0e0e0;
      --button-secondary-text: #333;
      --error-bg: #ffebee;
      --error-text: #c62828;
      --toast-bg: #333;
      --toast-text: white;
      --empty-icon-color: #999;
      --refinement-bg: #fff8e1;
      --refinement-border: #ffe082;
      --refinement-button-bg: #ff8f00;
    }

    :host([data-theme='dark']) {
      --primary-color: #008cff;
      --primary-hover-color: #0077e6;
      --text-color: #e0e0e0;
      --text-color-light: #b0b0b0;
      --background-color: #2c2c2c;
      --card-background: #3c3c3c;
      --border-color: #444;
      --header-bg: linear-gradient(135deg, #333 0%, #111 100%);
      --header-text: white;
      --input-bg: #444;
      --input-border: #555;
      --input-focus-shadow: rgba(0, 140, 255, 0.2);
      --button-primary-bg: #008cff;
      --button-primary-hover-bg: #0077e6;
      --button-secondary-bg: #555;
      --button-secondary-hover-bg: #666;
      --button-secondary-text: #e0e0e0;
      --error-bg: #5c1414;
      --error-text: #ff8080;
      --toast-bg: #555;
      --toast-text: white;
      --empty-icon-color: #777;
      --refinement-bg: #4a4a4a;
      --refinement-border: #6a6a6a;
      --refinement-button-bg: #d48200;
    }

    :host([data-theme='dark']) .agent-selector select option {
      color: var(--text-color);
    }

    /* Global Loading Indicator */
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 10000;
    }

    .spinner {
      border: 4px solid rgba(0, 0, 0, 0.1);
      border-left-color: var(--primary-color);
      border-radius: 50%;
      width: 40px;
      height: 40px;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Main Layout */
    .main-layout {
      display: grid;
      grid-template-columns: 1fr 1fr;
      height: calc(100vh - 60px);
      overflow: hidden;
    }

    /* Left Panel - Input & Suggestions */
    /* Toast */
    .toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: var(--toast-bg);
      color: var(--toast-text);
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 14px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
      animation: slideIn 0.3s ease;
      z-index: 1000;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Empty state */
    .empty-suggestions {
      text-align: center;
      padding: 40px;
      color: var(--empty-icon-color);
    }

    .empty-suggestions-icon {
      font-size: 48px;
      margin-bottom: 16px;
      opacity: 0.5;
    }

    /* Refinement section */
    .refinement-section {
      padding: 16px 20px;
      background: var(--refinement-bg);
      border-top: 1px solid var(--refinement-border);
    }

    .refinement-input {
      display: flex;
      gap: 8px;
    }

    .refinement-input input {
      flex: 1;
      padding: 10px 14px;
      border: 1px solid var(--refinement-border);
      border-radius: 6px;
      font-size: 13px;
      background: var(--input-bg);
      color: var(--text-color);
    }

    .refinement-input button {
      padding: 10px 16px;
      background: var(--refinement-button-bg);
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 13px;
      cursor: pointer;
    }

    /* History Section */
    .history-section {
      padding: 20px;
      border-top: 1px solid var(--border-color);
      background: var(--background-color);
      max-height: 200px; /* Adjust as needed */
      overflow-y: auto;
    }

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .history-header h2 {
      margin: 0;
      font-size: 14px;
      color: var(--text-color-light);
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .clear-history-btn {
      padding: 6px 12px;
      background: var(--button-secondary-bg);
      color: var(--button-secondary-text);
      border: none;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
      transition: background 0.2s;
    }

    .clear-history-btn:hover {
      background: var(--button-secondary-hover-bg);
    }

    .history-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .history-item {
      background: var(--card-background);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 12px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .history-item:hover {
      border-color: var(--primary-color);
      box-shadow: 0 2px 8px rgba(20, 115, 230, 0.1);
    }

    .history-item h3 {
      margin: 0 0 4px 0;
      font-size: 14px;
      color: var(--text-color);
    }

    .history-item p {
      margin: 0;
      font-size: 12px;
      color: var(--text-color-light);
      line-height: 1.4;
    }

    .empty-history {
      font-size: 13px;
      color: var(--text-color-light);
      text-align: center;
      padding: 10px 0;
    }
  `;

  render() {
    const isAI = this.selectedAgent.hasAI;

    return html`
      <assistant-header
        .agents=${AGENTS}
        .agentUrl=${this.agentUrl}
        .isAI=${this.selectedAgent.hasAI}
        @agent-changed=${this.handleAgentChange}
        @toggle-theme=${this.toggleTheme}
      ></assistant-header>

      <div class="main-layout">
        <!-- Left Panel: Input & Suggestions -->
        <div class="left-panel">
          <assistant-input
            .prompt=${this.prompt}
            .loading=${this.loading}
            @prompt-changed=${this.handlePromptChange}
            @generate-content=${this.generateContent}
          ></assistant-input>

          ${this.error ? html`<error-message .message=${this.error}></error-message>` : ''}

          <assistant-suggestions
            .suggestions=${this.suggestions}
            .selectedSuggestion=${this.selectedSuggestion}
            @suggestion-selected=${this.handleSuggestionSelected}
            @suggestion-applied=${this.handleSuggestionApplied}
            @copy-suggestion=${this.handleCopySuggestion}
          ></assistant-suggestions>

          <!-- History Section -->
          <div class="history-section">
            <div class="history-header">
              <h2>History</h2>
              <button class="clear-history-btn" @click=${this.clearHistory}>Clear History</button>
            </div>
            <div class="history-list">
              ${this.history.length === 0 ? html`
                <p class="empty-history">No history yet.</p>
              ` : html`
                ${this.history.map(item => html`
                  <div class="history-item" @click=${() => this.handleHistoryItemClick(item)}>
                    <h3>${item.title}</h3>
                    <p>${item.description}</p>
                  </div>
                `)}
              `}
            </div>
          </div>

          ${this.appliedContent ? html`
            <div class="refinement-section">
              <div class="refinement-input">
                <input type="text" placeholder="Refine: Make it more playful, shorter, add urgency..." id="refinement-input" />
                <button @click=${this.refineContent}>Refine</button>
              </div>
            </div>
          ` : ''}
        </div>

        <assistant-preview
            .appliedContent=${this.appliedContent}
            @copy-content=${this.handleCopyContent}
          ></assistant-preview>
      </div>

      ${this.showCopiedToast ? html`
        <div class="toast">Copied to clipboard!</div>
      ` : ''}

      ${this.loading ? html`
        <div class="loading-overlay">
          <div class="spinner"></div>
        </div>
      ` : ''}
    `;
  }

  firstUpdated() {
    this.history = HistoryService.getHistory();
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
      this.theme = 'dark';
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      this.theme = 'light';
      document.documentElement.removeAttribute('data-theme');
    }
  }

  private toggleTheme() {
    this.theme = this.theme === 'light' ? 'dark' : 'light';
    if (this.theme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
      localStorage.setItem('theme', 'light');
    }
  }

  private handleHistoryItemClick(item: ContentSuggestion) {
    this.appliedContent = item;
    this.selectedSuggestion = item;
  }

  private clearHistory() {
    HistoryService.clearHistory();
    this.history = [];
  }

  public handleAgentChange(e: CustomEvent) {
    const agent = e.detail.agent;
    if (agent) {
      this.selectedAgent = agent;
      this.agentUrl = agent.url;
      this.suggestions = [];
      this.selectedSuggestion = null;
      this.appliedContent = null;
      this.error = '';
    }
  }

  private async generateContent() {
    if (!this.prompt.trim()) return;

    this.loading = true;
    this.error = '';
    this.suggestions = [];
    this.selectedSuggestion = null;

    try {
      const response = await fetch(`${this.agentUrl}/tasks`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({
          message: {
            role: 'user',
            parts: [{ text: this.prompt }],
          },
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('Agent response:', data);

      // Parse the response to extract content suggestions
      const suggestions = this.parseAgentResponse(data);

      if (suggestions.length === 0) {
        this.error = 'No suggestions generated. Try a different prompt.';
      } else {
        this.suggestions = suggestions;
        // Auto-select and apply the first suggestion
        this.selectedSuggestion = suggestions[0];
        this.appliedContent = suggestions[0];
        HistoryService.addSuggestion(suggestions[0]);
        this.history = HistoryService.getHistory();
      }
    } catch (error) {
      console.error('Generation error:', error);
      this.error = `Failed to generate content: ${error}`;
    } finally {
      this.loading = false;
    }
  }

  private parseAgentResponse(data: any): ContentSuggestion[] {
    const suggestions: ContentSuggestion[] = [];

    // Try to extract from A2UI messages format
    if (data.messages && Array.isArray(data.messages)) {
      for (const msg of data.messages) {
        if (msg.parts) {
          for (const part of msg.parts) {
            if (part.a2ui) {
              const suggestion = this.extractFromA2UI(part.a2ui);
              if (suggestion) suggestions.push(suggestion);
            }
          }
        }
      }
    }

    // Also check for direct content in artifacts
    if (data.artifacts && Array.isArray(data.artifacts)) {
      for (const artifact of data.artifacts) {
        if (artifact.parts) {
          for (const part of artifact.parts) {
            if (part.data) {
              try {
                const parsed = typeof part.data === 'string' ? JSON.parse(part.data) : part.data;
                if (parsed.title) {
                  suggestions.push({
                    id: `suggestion-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                    ...parsed
                  });
                }
              } catch (e) {
                // Ignore parse errors
              }
            }
          }
        }
      }
    }

    // If no suggestions found, create a mock for demo purposes
    if (suggestions.length === 0 && data.messages) {
      suggestions.push(this.createMockSuggestion('1'));
      suggestions.push(this.createMockSuggestion('2'));
      suggestions.push(this.createMockSuggestion('3'));
    }

    return suggestions;
  }

  private extractFromA2UI(a2ui: any): ContentSuggestion | null {
    try {
      // Navigate A2UI structure to find content
      if (a2ui.surface?.components) {
        for (const comp of a2ui.surface.components) {
          if (comp.dataContext) {
            const suggestion = comp.dataContext.suggestion || comp.dataContext;
            if (suggestion.title) {
              return {
                id: `suggestion-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                title: suggestion.title,
                subtitle: suggestion.subtitle,
                description: suggestion.description,
                ctaText: suggestion.ctaText || 'Learn More',
                ctaUrl: suggestion.ctaUrl || '#',
                imageUrl: suggestion.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=800',
                imageAlt: suggestion.imageAlt,
                componentType: suggestion.componentType || 'hero',
                price: suggestion.price,
              };
            }
          }
        }
      }
    } catch (e) {
      console.error('Error extracting from A2UI:', e);
    }
    return null;
  }

  private createMockSuggestion(variant: string): ContentSuggestion {
    const componentType = this.detectComponentType(this.prompt);
    const variations: Record<string, Partial<ContentSuggestion>> = {
      '1': {
        title: 'Unleash Your Potential',
        subtitle: 'Innovation Awaits',
        description: 'Discover innovative solutions designed to transform your digital experience and drive meaningful results.',
      },
      '2': {
        title: 'Experience Excellence',
        subtitle: 'Quality Redefined',
        description: 'Premium quality meets cutting-edge technology. Elevate your journey with our award-winning solutions.',
      },
      '3': {
        title: 'Transform Today',
        subtitle: 'Future Ready',
        description: 'Stay ahead of the curve with next-generation capabilities designed for modern enterprises.',
      },
    };

    const base = variations[variant] || variations['1'];

    return {
      id: `mock-${variant}-${Date.now()}`,
      title: base.title!,
      subtitle: base.subtitle,
      description: base.description!,
      ctaText: componentType === 'product' ? 'Shop Now' : 'Get Started',
      ctaUrl: '/get-started',
      imageUrl: this.getImageForType(componentType),
      imageAlt: `${componentType} image`,
      componentType,
      price: componentType === 'product' ? '$99.99' : undefined,
    };
  }

  public detectComponentType(prompt: string): string {
    const lower = prompt.toLowerCase();
    if (lower.includes('hero')) return 'hero';
    if (lower.includes('product')) return 'product';
    if (lower.includes('teaser')) return 'teaser';
    if (lower.includes('banner')) return 'banner';
    return 'hero';
  }

  private getImageForType(type: string): string {
    const images: Record<string, string> = {
      hero: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=1200',
      product: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800',
      teaser: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600',
      banner: 'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=1200',
    };
    return images[type] || images.hero;
  }

  private handleCopyContent(e: CustomEvent) {
    this.copyToClipboard(e.detail.content, e.detail.format);
  }

  private handleSuggestionSelected(e: CustomEvent) {
    this.selectedSuggestion = e.detail.suggestion;
  }

  private handleSuggestionApplied(e: CustomEvent) {
    this.appliedContent = e.detail.suggestion;
    this.selectedSuggestion = e.detail.suggestion;
  }

  private handleCopySuggestion(e: CustomEvent) {
    this.copyToClipboard(e.detail.suggestion);
  }

  private async copyToClipboard(content: ContentSuggestion, format: 'json' | 'html' = 'json') {
    let text: string;

    if (format === 'html') {
      text = this.generateHTML(content);
    } else {
      text = JSON.stringify(content, null, 2);
    }

    try {
      await navigator.clipboard.writeText(text);
      this.showCopiedToast = true;
      setTimeout(() => {
        this.showCopiedToast = false;
      }, 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  }

  private generateHTML(content: ContentSuggestion): string {
    const type = content.componentType?.toLowerCase() || 'hero';

    switch (type) {
      case 'hero':
        return `<div class="cmp-hero">
  <div class="cmp-hero__image-container">
    <img src="${content.imageUrl}" alt="${content.imageAlt || content.title}" class="cmp-hero__image" />
  </div>
  <div class="cmp-hero__content">
    <h1 class="cmp-hero__title">${content.title}</h1>
    ${content.subtitle ? `<p class="cmp-hero__subtitle">${content.subtitle}</p>` : ''}
    <p class="cmp-hero__description">${content.description}</p>
    <a href="${content.ctaUrl}" class="cmp-hero__cta">${content.ctaText}</a>
  </div>
</div>`;

      case 'product':
        return `<div class="cmp-product-card">
  <img src="${content.imageUrl}" alt="${content.imageAlt || content.title}" class="cmp-product-card__image" />
  <div class="cmp-product-card__content">
    <h3 class="cmp-product-card__title">${content.title}</h3>
    ${content.price ? `<span class="cmp-product-card__price">${content.price}</span>` : ''}
    <p class="cmp-product-card__description">${content.description}</p>
    <a href="${content.ctaUrl}" class="cmp-product-card__cta">${content.ctaText}</a>
  </div>
</div>`;

      default:
        return `<div class="cmp-${type}">
  <h2>${content.title}</h2>
  <p>${content.description}</p>
  <a href="${content.ctaUrl}">${content.ctaText}</a>
</div>`;
    }
  }

  private refineContent() {
    const input = this.shadowRoot?.querySelector('#refinement-input') as HTMLInputElement;
    if (input?.value && this.prompt) {
      this.prompt = `${this.prompt}. ${input.value}`;
      input.value = '';
      this.generateContent();
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'aem-assistant': AemAssistant;
  }
}
