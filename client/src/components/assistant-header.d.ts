import { LitElement } from 'lit';
interface Agent {
    name: string;
    url: string;
    port: number;
    hasAI: boolean;
}
export declare class AssistantHeader extends LitElement {
    agents: Agent[];
    agentUrl: string;
    isAI: boolean;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
    private handleAgentChange;
}
declare global {
    interface HTMLElementTagNameMap {
        'assistant-header': AssistantHeader;
    }
    interface Agent {
        name: string;
        url: string;
        port: number;
        hasAI: boolean;
    }
}
export {};
//# sourceMappingURL=assistant-header.d.ts.map