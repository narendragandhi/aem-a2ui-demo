import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

@customElement('assistant-input')
export class AssistantInput extends LitElement {
  @property({ type: String }) prompt = '';
  @property({ type: Boolean }) loading = false;

  static styles = css`
    .input-section {
      padding: 20px;
      background: var(--card-background);
      border-bottom: 1px solid var(--border-color);
    }

    .input-section h2 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: var(--text-color-light);
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .input-group {
      display: flex;
      gap: 8px;
    }

    .input-group input {
      flex: 1;
      padding: 14px 16px;
      border: 2px solid var(--input-border);
      border-radius: 8px;
      font-size: 15px;
      transition: border-color 0.2s, box-shadow 0.2s;
      background: var(--input-bg);
      color: var(--text-color);
    }

    .input-group input:focus {
      outline: none;
      border-color: var(--primary-color);
      box-shadow: 0 0 0 3px var(--input-focus-shadow);
    }

    .input-group input::placeholder {
      color: var(--text-color-light);
    }

    .generate-btn {
      padding: 14px 28px;
      background: var(--button-primary-bg);
      color: var(--header-text);
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-size: 15px;
      font-weight: 600;
      transition: background 0.2s, transform 0.1s;
      white-space: nowrap;
    }

    .generate-btn:hover:not(:disabled) {
      background: var(--button-primary-hover-bg);
    }

    .generate-btn:active:not(:disabled) {
      transform: scale(0.98);
    }

    .generate-btn:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    /* Quick prompts */
    .quick-prompts {
      margin-top: 12px;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .quick-prompt {
      padding: 6px 12px;
      background: var(--button-secondary-bg);
      border: 1px solid var(--border-color);
      border-radius: 16px;
      font-size: 12px;
      color: var(--text-color-light);
      cursor: pointer;
      transition: all 0.2s;
    }

    .quick-prompt:hover {
      background: var(--primary-color);
      border-color: var(--primary-color);
      color: var(--header-text);
    }
  `;

  render() {
    return html`
      <div class="input-section">
        <h2>Describe Your Content</h2>
        <div class="input-group">
          <input
            type="text"
            placeholder="e.g., Hero banner for summer sale with beach theme"
            .value=${this.prompt}
            @input=${this.handleInput}
            @keydown=${this.handleKeydown}
            .disabled=${this.loading}
          />
          <button
            class="generate-btn"
            @click=${this.generateContent}
            .disabled=${this.loading}
          >
            ${this.loading ? '' : 'Generate'}
          </button>
        </div>
        <div class="quick-prompts">
          <span class="quick-prompt" @click=${() => this.setPrompt('Hero banner for product launch')}>Hero Banner</span>
          <span class="quick-prompt" @click=${() => this.setPrompt('Product card for electronics')}>Product Card</span>
          <span class="quick-prompt" @click=${() => this.setPrompt('Promotional teaser for holiday sale')}>Promo Teaser</span>
          <span class="quick-prompt" @click=${() => this.setPrompt('Announcement banner for new feature')}>Banner</span>
        </div>
      </div>
    `;
  }

  private handleInput(e: Event) {
    this.prompt = (e.target as HTMLInputElement).value;
    this.dispatchEvent(new CustomEvent('prompt-changed', {
      detail: { prompt: this.prompt },
      bubbles: true,
      composed: true,
    }));
  }

  private handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !this.loading) {
      this.generateContent();
    }
  }

  private setPrompt(prompt: string) {
    this.prompt = prompt;
    this.dispatchEvent(new CustomEvent('prompt-changed', {
      detail: { prompt: this.prompt },
      bubbles: true,
      composed: true,
    }));
    this.generateContent();
  }

  private generateContent() {
    this.dispatchEvent(new CustomEvent('generate-content', {
      bubbles: true,
      composed: true,
    }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'assistant-input': AssistantInput;
  }
}
