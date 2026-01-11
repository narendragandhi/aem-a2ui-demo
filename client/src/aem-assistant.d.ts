import { LitElement } from 'lit';
import './components/assistant-header.js';
import './components/assistant-input.js';
import './components/assistant-suggestions.js';
import './components/assistant-preview.js';
import './components/error-message.js';
export declare class AemAssistant extends LitElement {
    agentUrl: string;
    private loading;
    private selectedAgent;
    private error;
    private suggestions;
    private selectedSuggestion;
    private appliedContent;
    prompt: string;
    private showCopiedToast;
    private refinementMode;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
    private handlePromptChange;
    handleAgentChange(e: CustomEvent): void;
    private generateContent;
    private parseAgentResponse;
    private extractFromA2UI;
    private createMockSuggestion;
    detectComponentType(prompt: string): string;
    private getImageForType;
    private handleCopyContent;
    private handleSuggestionSelected;
    private handleSuggestionApplied;
    private handleCopySuggestion;
    private copyToClipboard;
    private generateHTML;
    private refineContent;
}
declare global {
    interface HTMLElementTagNameMap {
        'aem-assistant': AemAssistant;
    }
}
//# sourceMappingURL=aem-assistant.d.ts.map