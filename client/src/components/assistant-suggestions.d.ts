import { LitElement } from 'lit';
import { ContentSuggestion } from '../lib/types.js';
export declare class AssistantSuggestions extends LitElement {
    suggestions: ContentSuggestion[];
    selectedSuggestion: ContentSuggestion | null;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
    private selectSuggestion;
    private applySuggestion;
    private copyToClipboard;
}
declare global {
    interface HTMLElementTagNameMap {
        'assistant-suggestions': AssistantSuggestions;
    }
}
//# sourceMappingURL=assistant-suggestions.d.ts.map