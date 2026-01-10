import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { v0_8 } from '@a2ui/lit';

// Import the data processor and UI components
const { Data, UI } = v0_8;

// Register the A2UI UI components (this defines custom elements like a2ui-surface, a2ui-root, etc.)
// The Surface and Root components need to be imported to register themselves
const { Surface, Root } = UI;

// Available agents
const AGENTS = [
  { name: 'Python Agent', url: 'http://localhost:10002', port: 10002 },
  { name: 'Java Agent + Ollama', url: 'http://localhost:10003', port: 10003 },
  { name: 'Python Advanced', url: 'http://localhost:10004', port: 10004 },
];

@customElement('aem-assistant')
export class AemAssistant extends LitElement {
  @property({ type: String }) agentUrl = 'http://localhost:10003'; // Default to Java agent

  @state() private loading = false;
  @state() private selectedAgent = AGENTS[1]; // Java agent by default
  @state() private error = '';
  @state() private debugMessages: any[] = [];
  @state() private surfaceData: any = null;

  private processor = Data.createSignalA2uiMessageProcessor();

  static styles = css`
    :host {
      display: block;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      max-width: 700px;
      margin: 0 auto;
      padding: 20px;
    }

    h1 {
      color: #1473e6;
      margin-bottom: 20px;
    }

    .input-area {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
    }

    input {
      flex: 1;
      padding: 12px 16px;
      border: 1px solid #d0d0d0;
      border-radius: 4px;
      font-size: 14px;
    }

    input:focus {
      outline: none;
      border-color: #1473e6;
      box-shadow: 0 0 0 2px rgba(20, 115, 230, 0.2);
    }

    button {
      padding: 12px 24px;
      background: #1473e6;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
    }

    button:hover {
      background: #0d66d0;
    }

    button:disabled {
      background: #b0b0b0;
      cursor: not-allowed;
    }

    .surface-container {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 20px;
      background: #fafafa;
      min-height: 200px;
    }

    .error {
      color: #d7373f;
      padding: 12px;
      background: #ffebee;
      border-radius: 4px;
      margin-bottom: 16px;
    }

    .loading {
      text-align: center;
      padding: 40px;
      color: #666;
    }

    .placeholder {
      color: #666;
      text-align: center;
      padding: 40px;
    }

    .debug {
      margin-top: 20px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 4px;
      font-family: monospace;
      font-size: 11px;
      white-space: pre-wrap;
      max-height: 300px;
      overflow-y: auto;
      border: 1px solid #ddd;
    }

    .debug-title {
      font-weight: bold;
      margin-bottom: 8px;
      color: #333;
      font-family: sans-serif;
    }

    /* Style the A2UI components */
    a2ui-surface {
      display: block;
    }

    .agent-selector {
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .agent-selector label {
      font-size: 14px;
      color: #666;
    }

    .agent-selector select {
      padding: 8px 12px;
      border: 1px solid #d0d0d0;
      border-radius: 4px;
      font-size: 14px;
      background: white;
      cursor: pointer;
      min-width: 200px;
    }

    .agent-selector select:focus {
      outline: none;
      border-color: #1473e6;
      box-shadow: 0 0 0 2px rgba(20, 115, 230, 0.2);
    }

    .agent-status {
      font-size: 12px;
      padding: 4px 8px;
      border-radius: 4px;
      background: #e8f5e9;
      color: #2e7d32;
    }

    .agent-status.ollama {
      background: #fff3e0;
      color: #e65100;
    }
  `;

  render() {
    const isOllama = this.selectedAgent.name.toLowerCase().includes('ollama');

    return html`
      <h1>AEM Content Assistant</h1>

      <div class="agent-selector">
        <label>Agent:</label>
        <select @change=${this.handleAgentChange}>
          ${AGENTS.map(agent => html`
            <option value=${agent.url} ?selected=${agent.url === this.agentUrl}>
              ${agent.name}
            </option>
          `)}
        </select>
        <span class="agent-status ${isOllama ? 'ollama' : ''}">
          ${isOllama ? '🦙 Local LLM' : '☁️ Template Mode'}
        </span>
      </div>

      <div class="input-area">
        <input
          type="text"
          placeholder="Describe what content you need (e.g., 'hero banner', 'product card')"
          @keydown=${this.handleKeydown}
          .disabled=${this.loading}
        />
        <button @click=${this.sendMessage} .disabled=${this.loading}>
          ${this.loading ? 'Generating...' : 'Generate'}
        </button>
      </div>

      ${this.error ? html`<div class="error">${this.error}</div>` : ''}

      <div class="surface-container">
        ${this.loading
          ? html`<div class="loading">Generating content suggestion...</div>`
          : this.renderSurfaces()
        }
      </div>

      ${this.debugMessages.length > 0 ? html`
        <div class="debug">
          <div class="debug-title">A2UI Messages (Debug):</div>
          ${JSON.stringify(this.debugMessages, null, 2)}
        </div>
      ` : ''}
    `;
  }

  private renderSurfaces() {
    const surfaces = this.processor.getSurfaces();

    if (surfaces.size === 0) {
      return html`<div class="placeholder">Enter a prompt above to generate content suggestions</div>`;
    }

    const surfaceElements: any[] = [];

    surfaces.forEach((surface: any, surfaceId: string) => {
      console.log('Rendering surface:', surfaceId, surface);

      // The surface contains: rootComponentId, componentTree, dataModel, components
      if (surface.componentTree) {
        surfaceElements.push(html`
          <a2ui-root
            .node=${surface.componentTree}
            .processor=${this.processor}
            .surfaceId=${surfaceId}
            @a2ui-action=${this.handleAction}
          ></a2ui-root>
        `);
      }
    });

    return surfaceElements.length > 0
      ? surfaceElements
      : html`<div class="placeholder">No surfaces to render</div>`;
  }

  private handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') this.sendMessage();
  }

  private handleAgentChange(e: Event) {
    const select = e.target as HTMLSelectElement;
    const newUrl = select.value;
    const agent = AGENTS.find(a => a.url === newUrl);
    if (agent) {
      this.selectedAgent = agent;
      this.agentUrl = newUrl;
      console.log('Switched to agent:', agent.name, 'at', newUrl);
      // Clear previous results when switching agents
      this.processor.clearSurfaces();
      this.debugMessages = [];
      this.error = '';
      this.requestUpdate();
    }
  }

  private async sendMessage() {
    const input = this.shadowRoot?.querySelector('input') as HTMLInputElement;
    const message = input.value.trim();
    if (!message) return;

    this.loading = true;
    this.error = '';
    this.debugMessages = [];

    // Clear previous surfaces
    this.processor.clearSurfaces();

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
            parts: [{ text: message }],
          },
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('Agent response:', data);

      // Store for debug display
      this.debugMessages = data.messages || [];

      // Process A2UI messages
      if (data.messages && Array.isArray(data.messages)) {
        try {
          this.processor.processMessages(data.messages);
          console.log('Surfaces after processing:', this.processor.getSurfaces());
        } catch (e) {
          console.error('Error processing messages:', e);
          this.error = `Error processing A2UI messages: ${e}`;
        }
      }

      this.requestUpdate();
    } catch (error) {
      console.error('Agent error:', error);
      this.error = `Failed to connect to agent: ${error}`;
    } finally {
      this.loading = false;
    }
  }

  private handleAction(e: CustomEvent) {
    const { action, context } = e.detail || {};
    const actionName = action?.name || e.detail?.name;

    console.log('Action triggered:', actionName, e.detail);

    if (actionName === 'apply_suggestion') {
      // In a real AEM integration, this would call Granite APIs
      const surfaces = this.processor.getSurfaces();
      let suggestionData: any = {};

      surfaces.forEach((surface: any) => {
        const suggestion = surface.dataModel?.get?.('suggestion');
        if (suggestion) {
          suggestionData = {
            title: suggestion.get?.('title'),
            description: suggestion.get?.('description'),
            imageUrl: suggestion.get?.('imageUrl'),
          };
        }
      });

      alert('Would apply to AEM component:\n\n' + JSON.stringify(suggestionData, null, 2));
    } else if (actionName === 'regenerate') {
      // Get input and trigger new generation
      const input = this.shadowRoot?.querySelector('input') as HTMLInputElement;
      if (input.value) {
        this.sendMessage();
      }
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'aem-assistant': AemAssistant;
  }
}
