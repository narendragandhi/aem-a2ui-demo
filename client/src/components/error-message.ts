import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

@customElement('error-message')
export class ErrorMessage extends LitElement {
  @property({ type: String }) message = '';

  static styles = css`
    .error-message {
      background: #ffebee;
      color: #c62828;
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      position: absolute; /* Position relative to its closest positioned ancestor */
      top: 20px; /* Adjust as needed */
      left: 50%;
      transform: translateX(-50%);
      z-index: 100;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      width: fit-content;
      white-space: nowrap;
    }

    .error-icon {
      font-size: 20px;
    }
  `;

  render() {
    return html`
      ${this.message ? html`
        <div class="error-message">
          <span class="error-icon">!</span>
          <span>${this.message}</span>
        </div>
      ` : ''}
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'error-message': ErrorMessage;
  }
}
