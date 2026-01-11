import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { ContentSuggestion } from '../lib/types';
import './brand-score.js';
import { BrandScore } from './brand-score.js';

@customElement('assistant-preview')
export class AssistantPreview extends LitElement {
  @property({ type: Object }) appliedContent: ContentSuggestion | null = null;
  @state() private editingField: string | null = null;
  @state() private editMode = false;

  static styles = css`
    .right-panel {
      background: #fff;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .preview-header {
      padding: 16px 20px;
      background: #f8f9fa;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .preview-header h2 {
      margin: 0;
      font-size: 14px;
      color: #666;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .preview-actions {
      display: flex;
      gap: 8px;
    }

    .preview-action-btn {
      padding: 6px 12px;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .preview-action-btn:hover {
      border-color: #1473e6;
      color: #1473e6;
    }

    .preview-container {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background: #f0f0f0;
    }

    .preview-placeholder {
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #999;
      text-align: center;
      padding: 40px;
    }

    .preview-placeholder-icon {
      font-size: 80px;
      margin-bottom: 24px;
      animation: float 3s ease-in-out infinite;
    }

    @keyframes float {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-10px); }
    }

    .preview-placeholder h3 {
      margin: 0 0 12px 0;
      font-size: 24px;
      color: #333;
      font-weight: 600;
    }

    .preview-placeholder p {
      margin: 0 0 24px 0;
      font-size: 15px;
      max-width: 400px;
      color: #666;
      line-height: 1.6;
    }

    .preview-placeholder-steps {
      display: flex;
      gap: 16px;
      margin-top: 24px;
      padding: 20px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    }

    .step-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
    }

    .step-item-number {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #1473e6;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }

    .step-item-label {
      font-size: 12px;
      color: #666;
      text-align: center;
    }

    .step-arrow {
      display: flex;
      align-items: center;
      color: #ccc;
      font-size: 20px;
    }

    /* Component Previews */
    .component-preview {
      background: white;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
    }

    /* Hero Component */
    .hero-preview {
      position: relative;
      min-height: 400px;
    }

    .hero-preview .hero-image {
      width: 100%;
      height: 400px;
      object-fit: cover;
    }

    .hero-preview .hero-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.3) 100%);
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: 40px;
      color: white;
    }

    .hero-preview h2 {
      margin: 0 0 8px 0;
      font-size: 36px;
      font-weight: 700;
      text-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }

    .hero-preview .subtitle {
      font-size: 18px;
      opacity: 0.9;
      margin-bottom: 16px;
    }

    .hero-preview .description {
      font-size: 16px;
      opacity: 0.85;
      max-width: 500px;
      line-height: 1.6;
      margin-bottom: 24px;
    }

    .hero-preview .cta-button {
      display: inline-block;
      padding: 14px 32px;
      background: #1473e6;
      color: white;
      text-decoration: none;
      border-radius: 6px;
      font-weight: 600;
      font-size: 16px;
      transition: background 0.2s;
    }

    /* Product Component */
    .product-preview {
      display: flex;
      flex-direction: column;
    }

    .product-preview .product-image {
      width: 100%;
      height: 300px;
      object-fit: cover;
    }

    .product-preview .product-content {
      padding: 24px;
    }

    .product-preview h3 {
      margin: 0 0 8px 0;
      font-size: 24px;
      color: #333;
    }

    .product-preview .price {
      font-size: 28px;
      font-weight: 700;
      color: #1473e6;
      margin-bottom: 12px;
    }

    .product-preview .description {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      margin-bottom: 20px;
    }

    .product-preview .cta-button {
      display: inline-block;
      padding: 12px 24px;
      background: #1473e6;
      color: white;
      text-decoration: none;
      border-radius: 6px;
      font-weight: 600;
      text-align: center;
    }

    /* Teaser Component */
    .teaser-preview {
      display: grid;
      grid-template-columns: 1fr 1fr;
    }

    .teaser-preview .teaser-image {
      width: 100%;
      height: 250px;
      object-fit: cover;
    }

    .teaser-preview .teaser-content {
      padding: 24px;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .teaser-preview h3 {
      margin: 0 0 8px 0;
      font-size: 22px;
      color: #333;
    }

    .teaser-preview .subtitle {
      font-size: 14px;
      color: #1473e6;
      margin-bottom: 12px;
      font-weight: 500;
    }

    .teaser-preview .description {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      margin-bottom: 20px;
    }

    .teaser-preview .cta-link {
      color: #1473e6;
      text-decoration: none;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Banner Component */
    .banner-preview {
      background: linear-gradient(135deg, #1473e6 0%, #6929c4 100%);
      padding: 32px;
      color: white;
      text-align: center;
    }

    .banner-preview h3 {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 700;
    }

    .banner-preview .subtitle {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: 12px;
    }

    .banner-preview .description {
      font-size: 14px;
      opacity: 0.85;
      max-width: 500px;
      margin: 0 auto 20px auto;
      line-height: 1.6;
    }

    .banner-preview .cta-button {
      display: inline-block;
      padding: 12px 28px;
      background: white;
      color: #1473e6;
      text-decoration: none;
      border-radius: 6px;
      font-weight: 600;
    }

    /* Edit Mode Toggle */
    .edit-toggle {
      padding: 6px 12px;
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .edit-toggle:hover {
      background: #ffc107;
      color: #333;
    }

    .edit-toggle.active {
      background: #28a745;
      border-color: #28a745;
      color: white;
    }

    /* Editable Fields */
    .editable {
      cursor: pointer;
      position: relative;
      transition: all 0.2s;
      border-radius: 4px;
    }

    .edit-mode .editable:hover {
      outline: 2px dashed rgba(255, 193, 7, 0.8);
      outline-offset: 4px;
    }

    .editable.editing {
      outline: 2px solid #28a745 !important;
      outline-offset: 4px;
    }

    .editable-input {
      background: rgba(255, 255, 255, 0.95);
      border: 2px solid #28a745;
      border-radius: 4px;
      padding: 8px;
      font-family: inherit;
      width: 100%;
      box-sizing: border-box;
      resize: vertical;
    }

    .editable-input:focus {
      outline: none;
      box-shadow: 0 0 0 3px rgba(40, 167, 69, 0.25);
    }

    .edit-hint {
      position: fixed;
      bottom: 80px;
      left: 50%;
      transform: translateX(-50%);
      background: #333;
      color: white;
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 12px;
      z-index: 100;
      animation: fadeIn 0.3s;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateX(-50%) translateY(10px); }
      to { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
  `;

  render() {
    return html`
      <div class="right-panel">
        <div class="preview-header">
          <h2>Live Component Preview</h2>
          ${this.appliedContent ? html`
            <div class="preview-actions">
              <button
                class="edit-toggle ${this.editMode ? 'active' : ''}"
                @click=${() => this.toggleEditMode()}
              >
                ${this.editMode ? '&#x2714; Editing' : '&#x270F; Edit'}
              </button>
              <button class="preview-action-btn" @click=${() => this.copyToClipboard('html')}>
                Copy HTML
              </button>
              <button class="preview-action-btn" @click=${() => this.copyToClipboard('json')}>
                Copy JSON
              </button>
            </div>
          ` : ''}
        </div>
        <div class="preview-container ${this.editMode ? 'edit-mode' : ''}">
          ${this.appliedContent
            ? html`
              ${this.renderComponentPreview(this.appliedContent)}
              ${this.renderBrandScore(this.appliedContent)}
            `
            : html`
              <div class="preview-placeholder">
                <div class="preview-placeholder-icon">&#x2728;</div>
                <h3>Ready to Create</h3>
                <p>Use the guided wizard on the left to create stunning AEM components. Your live preview will appear here.</p>
                <div class="preview-placeholder-steps">
                  <div class="step-item">
                    <div class="step-item-number">1</div>
                    <div class="step-item-label">Choose Type</div>
                  </div>
                  <div class="step-arrow">&#x2192;</div>
                  <div class="step-item">
                    <div class="step-item-number">2</div>
                    <div class="step-item-label">Customize</div>
                  </div>
                  <div class="step-arrow">&#x2192;</div>
                  <div class="step-item">
                    <div class="step-item-number">3</div>
                    <div class="step-item-label">Generate</div>
                  </div>
                </div>
              </div>
            `
          }
        </div>
        ${this.editMode ? html`
          <div class="edit-hint">Click on any text to edit it directly</div>
        ` : ''}
      </div>
    `;
  }

  private toggleEditMode() {
    this.editMode = !this.editMode;
    this.editingField = null;
  }

  private renderBrandScore(content: ContentSuggestion) {
    const { score, factors } = BrandScore.calculateScore({
      title: content.title,
      subtitle: content.subtitle,
      description: content.description,
      cta: content.cta
    });

    return html`
      <div style="margin-top: 16px;">
        <brand-score .score=${score} .factors=${factors}></brand-score>
      </div>
    `;
  }

  private renderComponentPreview(content: ContentSuggestion) {
    const type = content.componentType?.toLowerCase() || 'hero';

    switch (type) {
      case 'hero':
        return html`
          <div class="component-preview hero-preview">
            <img class="hero-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="hero-overlay">
              ${this.renderEditableField('title', content.title, 'h2')}
              ${content.subtitle ? this.renderEditableField('subtitle', content.subtitle, 'div', 'subtitle') : ''}
              ${this.renderEditableField('description', content.description, 'div', 'description')}
              ${this.renderEditableField('ctaText', content.ctaText || 'Learn More', 'a', 'cta-button')}
            </div>
          </div>
        `;

      case 'product':
        return html`
          <div class="component-preview product-preview">
            <img class="product-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="product-content">
              ${this.renderEditableField('title', content.title, 'h3')}
              ${content.price ? this.renderEditableField('price', content.price, 'div', 'price') : ''}
              ${this.renderEditableField('description', content.description, 'div', 'description')}
              ${this.renderEditableField('ctaText', content.ctaText || 'Shop Now', 'a', 'cta-button')}
            </div>
          </div>
        `;

      case 'teaser':
        return html`
          <div class="component-preview teaser-preview">
            <img class="teaser-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="teaser-content">
              ${this.renderEditableField('title', content.title, 'h3')}
              ${content.subtitle ? this.renderEditableField('subtitle', content.subtitle, 'div', 'subtitle') : ''}
              ${this.renderEditableField('description', content.description, 'div', 'description')}
              ${this.renderEditableField('ctaText', (content.ctaText || 'Read More') + ' →', 'a', 'cta-link')}
            </div>
          </div>
        `;

      case 'banner':
        return html`
          <div class="component-preview banner-preview">
            ${this.renderEditableField('title', content.title, 'h3')}
            ${content.subtitle ? this.renderEditableField('subtitle', content.subtitle, 'div', 'subtitle') : ''}
            ${this.renderEditableField('description', content.description, 'div', 'description')}
            ${this.renderEditableField('ctaText', content.ctaText || 'Learn More', 'a', 'cta-button')}
          </div>
        `;

      default:
        return html`
          <div class="component-preview hero-preview">
            <img class="hero-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="hero-overlay">
              ${this.renderEditableField('title', content.title, 'h2')}
              ${this.renderEditableField('description', content.description, 'div', 'description')}
              ${this.renderEditableField('ctaText', content.ctaText || 'Learn More', 'a', 'cta-button')}
            </div>
          </div>
        `;
    }
  }

  private renderEditableField(
    field: string,
    value: string,
    tag: string = 'div',
    className: string = ''
  ) {
    const isEditing = this.editingField === field;
    const content = value || '';

    if (this.editMode && isEditing) {
      // Show input when editing
      const isMultiline = field === 'description';
      return isMultiline
        ? html`
          <textarea
            class="editable-input ${className}"
            .value=${content}
            @blur=${(e: Event) => this.finishEditing(field, (e.target as HTMLTextAreaElement).value)}
            @keydown=${(e: KeyboardEvent) => this.handleKeyDown(e, field)}
            rows="3"
          ></textarea>
        `
        : html`
          <input
            type="text"
            class="editable-input ${className}"
            .value=${content}
            @blur=${(e: Event) => this.finishEditing(field, (e.target as HTMLInputElement).value)}
            @keydown=${(e: KeyboardEvent) => this.handleKeyDown(e, field)}
          />
        `;
    }

    // Non-editing mode - just show the element with click handler
    const editableClass = this.editMode ? 'editable' : '';

    switch (tag) {
      case 'h2':
        return html`<h2 class="${editableClass} ${className}" @click=${() => this.startEditing(field)}>${content}</h2>`;
      case 'h3':
        return html`<h3 class="${editableClass} ${className}" @click=${() => this.startEditing(field)}>${content}</h3>`;
      case 'a':
        return html`<a class="${editableClass} ${className}" @click=${(e: Event) => { e.preventDefault(); this.startEditing(field); }}>${content}</a>`;
      default:
        return html`<div class="${editableClass} ${className}" @click=${() => this.startEditing(field)}>${content}</div>`;
    }
  }

  private startEditing(field: string) {
    if (!this.editMode) return;
    this.editingField = field;

    // Focus the input after render
    this.updateComplete.then(() => {
      const input = this.shadowRoot?.querySelector('.editable-input') as HTMLInputElement | HTMLTextAreaElement;
      if (input) {
        input.focus();
        input.select();
      }
    });
  }

  private finishEditing(field: string, value: string) {
    if (!this.appliedContent) return;

    // Update the content
    const updatedContent = { ...this.appliedContent, [field]: value };

    // Dispatch event to parent
    this.dispatchEvent(new CustomEvent('content-updated', {
      detail: { field, value, content: updatedContent },
      bubbles: true,
      composed: true,
    }));

    this.editingField = null;
  }

  private handleKeyDown(e: KeyboardEvent, field: string) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      const target = e.target as HTMLInputElement | HTMLTextAreaElement;
      this.finishEditing(field, target.value);
    } else if (e.key === 'Escape') {
      this.editingField = null;
    }
  }

  private copyToClipboard(format: 'json' | 'html') {
    if (!this.appliedContent) return;
    this.dispatchEvent(new CustomEvent('copy-content', {
      detail: { content: this.appliedContent, format },
      bubbles: true,
      composed: true,
    }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'assistant-preview': AssistantPreview;
  }
}
