import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ContentSuggestion } from '../lib/types';

@customElement('assistant-preview')
export class AssistantPreview extends LitElement {
  @property({ type: Object }) appliedContent: ContentSuggestion | null = null;

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
    }

    .preview-placeholder-icon {
      font-size: 48px;
      margin-bottom: 16px;
      opacity: 0.5;
    }

    .preview-placeholder h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      color: #666;
    }

    .preview-placeholder p {
      margin: 0;
      font-size: 14px;
      max-width: 300px;
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
  `;

  render() {
    return html`
      <div class="right-panel">
        <div class="preview-header">
          <h2>Live Component Preview</h2>
          ${this.appliedContent ? html`
            <div class="preview-actions">
              <button class="preview-action-btn" @click=${() => this.copyToClipboard('html')}>
                Copy HTML
              </button>
              <button class="preview-action-btn" @click=${() => this.copyToClipboard('json')}>
                Copy JSON
              </button>
            </div>
          ` : ''}
        </div>
        <div class="preview-container">
          ${this.appliedContent
            ? this.renderComponentPreview(this.appliedContent)
            : html`
              <div class="preview-placeholder">
                <div class="preview-placeholder-icon">&#128444;</div>
                <h3>No Content Applied</h3>
                <p>Generate suggestions and click "Apply to Preview" to see how your component will look</p>
              </div>
            `
          }
        </div>
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
              <h2>${content.title}</h2>
              ${content.subtitle ? html`<div class="subtitle">${content.subtitle}</div>` : ''}
              <div class="description">${content.description}</div>
              <a href="${content.ctaUrl}" class="cta-button">${content.ctaText}</a>
            </div>
          </div>
        `;

      case 'product':
        return html`
          <div class="component-preview product-preview">
            <img class="product-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="product-content">
              <h3>${content.title}</h3>
              ${content.price ? html`<div class="price">${content.price}</div>` : ''}
              <div class="description">${content.description}</div>
              <a href="${content.ctaUrl}" class="cta-button">${content.ctaText}</a>
            </div>
          </div>
        `;

      case 'teaser':
        return html`
          <div class="component-preview teaser-preview">
            <img class="teaser-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="teaser-content">
              <h3>${content.title}</h3>
              ${content.subtitle ? html`<div class="subtitle">${content.subtitle}</div>` : ''}
              <div class="description">${content.description}</div>
              <a href="${content.ctaUrl}" class="cta-link">${content.ctaText} &rarr;</a>
            </div>
          </div>
        `;

      case 'banner':
        return html`
          <div class="component-preview banner-preview">
            <h3>${content.title}</h3>
            ${content.subtitle ? html`<div class="subtitle">${content.subtitle}</div>` : ''}
            <div class="description">${content.description}</div>
            <a href="${content.ctaUrl}" class="cta-button">${content.ctaText}</a>
          </div>
        `;

      default:
        return html`
          <div class="component-preview hero-preview">
            <img class="hero-image" src="${content.imageUrl}" alt="${content.imageAlt || content.title}" />
            <div class="hero-overlay">
              <h2>${content.title}</h2>
              <div class="description">${content.description}</div>
              <a href="${content.ctaUrl}" class="cta-button">${content.ctaText}</a>
            </div>
          </div>
        `;
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
