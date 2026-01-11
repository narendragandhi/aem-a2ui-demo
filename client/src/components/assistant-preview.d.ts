import { LitElement } from 'lit';
import { ContentSuggestion } from '../lib/types';
export declare class AssistantPreview extends LitElement {
    appliedContent: ContentSuggestion | null;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
    private renderComponentPreview;
    private copyToClipboard;
}
declare global {
    interface HTMLElementTagNameMap {
        'assistant-preview': AssistantPreview;
    }
}
//# sourceMappingURL=assistant-preview.d.ts.map