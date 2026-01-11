import { LitElement } from 'lit';
export declare class AssistantInput extends LitElement {
    prompt: string;
    loading: boolean;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
    private handleInput;
    private handleKeydown;
    private setPrompt;
    private generateContent;
}
declare global {
    interface HTMLElementTagNameMap {
        'assistant-input': AssistantInput;
    }
}
//# sourceMappingURL=assistant-input.d.ts.map