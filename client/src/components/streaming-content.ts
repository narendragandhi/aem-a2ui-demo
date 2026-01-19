import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { ContentSuggestion } from '../lib/types';

/**
 * AG-UI Event Types from the backend
 */
type AgUiEventType =
  | 'RUN_STARTED'
  | 'TEXT_MESSAGE_START'
  | 'TEXT_MESSAGE_DELTA'
  | 'TEXT_MESSAGE_END'
  | 'STATE_DELTA'
  | 'RUN_FINISHED'
  | 'RUN_ERROR';

interface AgUiEvent {
  type: AgUiEventType;
  timestamp: number;
  data: {
    runId: string;
    field?: string;
    delta?: string | { content?: ContentSuggestion; componentType?: string };
    content?: string | ContentSuggestion;
    error?: string;
    status?: string;
  };
}

/**
 * Streaming Content Component
 *
 * Displays content generation with real-time streaming updates.
 * Implements AG-UI protocol for progressive content display.
 *
 * Features:
 * - Real-time text streaming (word by word)
 * - Typing cursor animation
 * - Field-by-field progressive rendering
 * - Error handling with retry
 * - Cancel stream support
 */
@customElement('streaming-content')
export class StreamingContent extends LitElement {
  static override styles = css`
    :host {
      display: block;
      font-family: var(--spectrum-font-family-base, 'Adobe Clean', sans-serif);
    }

    .streaming-container {
      background: var(--spectrum-gray-50, #ffffff);
      border-radius: 8px;
      overflow: hidden;
    }

    .stream-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      background: var(--spectrum-gray-100, #f5f5f5);
      border-bottom: 1px solid var(--spectrum-gray-200, #e1e1e1);
    }

    .stream-status {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--spectrum-gray-700, #464646);
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--spectrum-gray-400);
    }

    .status-dot.streaming {
      background: var(--spectrum-green-500, #12805c);
      animation: pulse 1.5s ease-in-out infinite;
    }

    .status-dot.completed {
      background: var(--spectrum-green-500, #12805c);
    }

    .status-dot.error {
      background: var(--spectrum-red-500, #d7373f);
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .cancel-btn {
      background: transparent;
      border: 1px solid var(--spectrum-gray-400);
      border-radius: 4px;
      padding: 4px 12px;
      font-size: 12px;
      cursor: pointer;
      color: var(--spectrum-gray-700);
    }

    .cancel-btn:hover {
      background: var(--spectrum-gray-200);
    }

    .content-preview {
      padding: 20px;
    }

    .field-group {
      margin-bottom: 16px;
    }

    .field-label {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--spectrum-gray-600, #6e6e6e);
      margin-bottom: 4px;
    }

    .field-value {
      font-size: 14px;
      line-height: 1.5;
      color: var(--spectrum-gray-900, #1d1d1d);
      min-height: 20px;
    }

    .field-value.title {
      font-size: 24px;
      font-weight: 700;
      line-height: 1.2;
    }

    .field-value.subtitle {
      font-size: 16px;
      color: var(--spectrum-gray-700);
    }

    .field-value.streaming {
      position: relative;
    }

    .field-value.streaming::after {
      content: '|';
      animation: blink 0.7s infinite;
      color: var(--spectrum-blue-500, #0d66d0);
      font-weight: normal;
    }

    @keyframes blink {
      0%, 50% { opacity: 1; }
      51%, 100% { opacity: 0; }
    }

    .field-value.empty {
      color: var(--spectrum-gray-400);
      font-style: italic;
    }

    .image-preview {
      width: 100%;
      max-width: 400px;
      height: 200px;
      background: var(--spectrum-gray-200);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--spectrum-gray-500);
      overflow: hidden;
    }

    .image-preview img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .cta-preview {
      display: inline-flex;
      align-items: center;
      padding: 10px 20px;
      background: var(--spectrum-blue-500, #0d66d0);
      color: white;
      border-radius: 20px;
      font-weight: 600;
      font-size: 14px;
    }

    .progress-bar {
      height: 3px;
      background: var(--spectrum-gray-200);
      overflow: hidden;
    }

    .progress-fill {
      height: 100%;
      background: var(--spectrum-blue-500);
      transition: width 0.3s ease;
    }

    .error-message {
      padding: 16px;
      background: var(--spectrum-red-100);
      color: var(--spectrum-red-800);
      border-radius: 4px;
      margin: 16px;
    }

    .actions {
      display: flex;
      gap: 8px;
      padding: 16px;
      border-top: 1px solid var(--spectrum-gray-200);
    }

    .action-btn {
      flex: 1;
      padding: 10px 16px;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      border: none;
    }

    .action-btn.primary {
      background: var(--spectrum-blue-500);
      color: white;
    }

    .action-btn.primary:hover {
      background: var(--spectrum-blue-600);
    }

    .action-btn.primary:disabled {
      background: var(--spectrum-gray-300);
      cursor: not-allowed;
    }

    .action-btn.secondary {
      background: transparent;
      border: 1px solid var(--spectrum-gray-400);
      color: var(--spectrum-gray-800);
    }

    .action-btn.secondary:hover {
      background: var(--spectrum-gray-100);
    }

    .idle-state {
      text-align: center;
      padding: 40px 20px;
      color: var(--spectrum-gray-600);
    }

    .idle-state svg {
      width: 64px;
      height: 64px;
      margin-bottom: 16px;
      opacity: 0.5;
    }
  `;

  @property({ type: String }) agentUrl = 'http://localhost:10003';
  @property({ type: String }) componentType = '';
  @property({ type: String }) prompt = '';

  @state() private status: 'idle' | 'streaming' | 'completed' | 'error' = 'idle';
  @state() private currentField = '';
  @state() private content: Partial<ContentSuggestion> = {};
  @state() private error = '';
  @state() private progress = 0;
  @state() private runId = '';

  private eventSource: EventSource | null = null;
  private fieldOrder = ['title', 'subtitle', 'description', 'ctaText', 'price', 'imageUrl'];

  override disconnectedCallback() {
    super.disconnectedCallback();
    this.cancelStream();
  }

  /**
   * Start streaming content generation
   */
  public async startStreaming(prompt: string, componentType?: string) {
    this.cancelStream(); // Cancel any existing stream
    this.reset();

    this.prompt = prompt;
    if (componentType) this.componentType = componentType;
    this.status = 'streaming';

    const params = new URLSearchParams({
      input: prompt,
      ...(this.componentType && { componentType: this.componentType }),
    });

    try {
      this.eventSource = new EventSource(`${this.agentUrl}/stream/generate?${params}`);

      // Listen for all AG-UI event types
      const eventTypes: AgUiEventType[] = [
        'RUN_STARTED',
        'TEXT_MESSAGE_START',
        'TEXT_MESSAGE_DELTA',
        'TEXT_MESSAGE_END',
        'STATE_DELTA',
        'RUN_FINISHED',
        'RUN_ERROR',
      ];

      eventTypes.forEach((eventType) => {
        this.eventSource!.addEventListener(eventType, (e: MessageEvent) => {
          this.handleEvent(eventType, JSON.parse(e.data));
        });
      });

      this.eventSource.onerror = (e) => {
        console.error('SSE Error:', e);
        if (this.status === 'streaming') {
          this.status = 'error';
          this.error = 'Connection lost. Please try again.';
        }
        this.eventSource?.close();
      };
    } catch (err) {
      this.status = 'error';
      this.error = err instanceof Error ? err.message : 'Failed to connect';
    }
  }

  /**
   * Handle incoming AG-UI events
   */
  private handleEvent(type: AgUiEventType, event: AgUiEvent) {
    const { data } = event;

    switch (type) {
      case 'RUN_STARTED':
        this.runId = data.runId;
        this.progress = 5;
        break;

      case 'TEXT_MESSAGE_START':
        this.currentField = data.field || '';
        this.updateProgress(data.field);
        break;

      case 'TEXT_MESSAGE_DELTA':
        if (data.field && data.content) {
          this.content = {
            ...this.content,
            [data.field]: data.content,
          };
          this.requestUpdate();
        }
        break;

      case 'TEXT_MESSAGE_END':
        this.currentField = '';
        break;

      case 'STATE_DELTA':
        // Full content update
        if (data.delta && typeof data.delta === 'object') {
          const deltaObj = data.delta as { content?: ContentSuggestion };
          if (deltaObj.content) {
            this.content = deltaObj.content;
          }
        }
        break;

      case 'RUN_FINISHED':
        this.status = 'completed';
        this.progress = 100;
        this.eventSource?.close();

        // Emit content-ready event
        this.dispatchEvent(
          new CustomEvent('content-ready', {
            detail: { content: this.content },
            bubbles: true,
            composed: true,
          })
        );
        break;

      case 'RUN_ERROR':
        this.status = 'error';
        this.error = data.error || 'An error occurred';
        this.eventSource?.close();
        break;
    }
  }

  /**
   * Update progress based on current field
   */
  private updateProgress(field?: string) {
    if (!field) return;
    const index = this.fieldOrder.indexOf(field);
    if (index !== -1) {
      this.progress = Math.min(95, 10 + (index / this.fieldOrder.length) * 85);
    }
  }

  /**
   * Cancel the current stream
   */
  public cancelStream() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.status === 'streaming') {
      this.status = 'idle';
    }
  }

  /**
   * Reset the component state
   */
  private reset() {
    this.status = 'idle';
    this.currentField = '';
    this.content = {};
    this.error = '';
    this.progress = 0;
    this.runId = '';
  }

  /**
   * Retry after error
   */
  private retry() {
    if (this.prompt) {
      this.startStreaming(this.prompt, this.componentType);
    }
  }

  /**
   * Accept the generated content
   */
  private acceptContent() {
    this.dispatchEvent(
      new CustomEvent('content-accepted', {
        detail: { content: this.content },
        bubbles: true,
        composed: true,
      })
    );
  }

  /**
   * Regenerate content
   */
  private regenerate() {
    if (this.prompt) {
      this.startStreaming(this.prompt, this.componentType);
    }
  }

  override render() {
    return html`
      <div class="streaming-container">
        ${this.renderHeader()}
        ${this.status !== 'idle' ? this.renderProgressBar() : ''}
        ${this.status === 'idle' ? this.renderIdleState() : ''}
        ${this.status === 'error' ? this.renderError() : ''}
        ${this.status === 'streaming' || this.status === 'completed'
          ? this.renderContent()
          : ''}
        ${this.status === 'completed' ? this.renderActions() : ''}
      </div>
    `;
  }

  private renderHeader() {
    const statusText = {
      idle: 'Ready',
      streaming: `Generating ${this.currentField || 'content'}...`,
      completed: 'Generation complete',
      error: 'Error',
    };

    return html`
      <div class="stream-header">
        <div class="stream-status">
          <span class="status-dot ${this.status}"></span>
          <span>${statusText[this.status]}</span>
        </div>
        ${this.status === 'streaming'
          ? html`
              <button class="cancel-btn" @click=${this.cancelStream}>Cancel</button>
            `
          : ''}
      </div>
    `;
  }

  private renderProgressBar() {
    return html`
      <div class="progress-bar">
        <div class="progress-fill" style="width: ${this.progress}%"></div>
      </div>
    `;
  }

  private renderIdleState() {
    return html`
      <div class="idle-state">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
          />
        </svg>
        <p>Enter a prompt to start generating content</p>
      </div>
    `;
  }

  private renderError() {
    return html`
      <div class="error-message">
        ${this.error}
        <button class="action-btn secondary" style="margin-top: 8px" @click=${this.retry}>
          Retry
        </button>
      </div>
    `;
  }

  private renderContent() {
    return html`
      <div class="content-preview">
        ${this.renderField('title', 'Title', this.content.title)}
        ${this.renderField('subtitle', 'Subtitle', this.content.subtitle)}
        ${this.renderField('description', 'Description', this.content.description)}
        ${this.content.ctaText ? this.renderCta() : ''}
        ${this.content.price
          ? this.renderField('price', 'Price', this.content.price)
          : ''}
        ${this.content.imageUrl ? this.renderImage() : ''}
      </div>
    `;
  }

  private renderField(fieldName: string, label: string, value?: string) {
    const isStreaming = this.currentField === fieldName;
    const isEmpty = !value;

    return html`
      <div class="field-group">
        <div class="field-label">${label}</div>
        <div
          class="field-value ${fieldName} ${isStreaming ? 'streaming' : ''} ${isEmpty
            ? 'empty'
            : ''}"
        >
          ${value || (isStreaming ? '' : 'Waiting...')}
        </div>
      </div>
    `;
  }

  private renderCta() {
    const isStreaming = this.currentField === 'ctaText';

    return html`
      <div class="field-group">
        <div class="field-label">Call to Action</div>
        <div class="cta-preview ${isStreaming ? 'streaming' : ''}">
          ${this.content.ctaText || 'Button'}
        </div>
      </div>
    `;
  }

  private renderImage() {
    return html`
      <div class="field-group">
        <div class="field-label">Image</div>
        <div class="image-preview">
          ${this.content.imageUrl
            ? html`<img src="${this.content.imageUrl}" alt="Preview" />`
            : 'No image'}
        </div>
      </div>
    `;
  }

  private renderActions() {
    return html`
      <div class="actions">
        <button class="action-btn secondary" @click=${this.regenerate}>Regenerate</button>
        <button class="action-btn primary" @click=${this.acceptContent}>Accept Content</button>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'streaming-content': StreamingContent;
  }
}
