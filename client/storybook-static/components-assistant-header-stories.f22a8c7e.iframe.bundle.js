/*! For license information please see components-assistant-header-stories.f22a8c7e.iframe.bundle.js.LICENSE.txt */
"use strict";(self.webpackChunkaem_a2ui_client=self.webpackChunkaem_a2ui_client||[]).push([[902],{"./src/components/assistant-header.stories.ts"(__unused_webpack_module,__webpack_exports__,__webpack_require__){__webpack_require__.r(__webpack_exports__),__webpack_require__.d(__webpack_exports__,{Default:()=>Default,__namedExportsOrder:()=>__namedExportsOrder,default:()=>assistant_header_stories});var lit=__webpack_require__("./node_modules/lit/index.js");var reactive_element=__webpack_require__("./node_modules/@lit/reactive-element/reactive-element.js");const o={attribute:!0,type:String,converter:reactive_element.W3,reflect:!1,hasChanged:reactive_element.Ec},r=(t=o,e,r)=>{const{kind:n,metadata:i}=r;let s=globalThis.litPropertyMetadata.get(i);if(void 0===s&&globalThis.litPropertyMetadata.set(i,s=new Map),"setter"===n&&((t=Object.create(t)).wrapped=!0),s.set(r.name,t),"accessor"===n){const{name:o}=r;return{set(r){const n=e.get.call(this);e.set.call(this,r),this.requestUpdate(o,n,t,!0,r)},init(e){return void 0!==e&&this.C(o,void 0,t,e),e}}}if("setter"===n){const{name:o}=r;return function(r){const n=this[o];e.call(this,r),this.requestUpdate(o,n,t,!0,r)}}throw Error("Unsupported decorator location: "+n)};function n(t){return(e,o)=>"object"==typeof o?r(t,e,o):((t,e,o)=>{const r=e.hasOwnProperty(o);return e.constructor.createProperty(o,t),r?Object.getOwnPropertyDescriptor(e,o):void 0})(t,e,o)}var __decorate=function(decorators,target,key,desc){var d,c=arguments.length,r=c<3?target:null===desc?desc=Object.getOwnPropertyDescriptor(target,key):desc;if("object"==typeof Reflect&&"function"==typeof Reflect.decorate)r=Reflect.decorate(decorators,target,key,desc);else for(var i=decorators.length-1;i>=0;i--)(d=decorators[i])&&(r=(c<3?d(r):c>3?d(target,key,r):d(target,key))||r);return c>3&&r&&Object.defineProperty(target,key,r),r};let AssistantHeader=class AssistantHeader extends lit.WF{constructor(){super(...arguments),this.agents=[],this.agentUrl="",this.isAI=!0}render(){return lit.qy`
      <div class="header">
        <h1>
          <span class="header-logo">A</span>
          AEM Content Assistant
        </h1>
        <div class="agent-selector">
          <select @change=${this.handleAgentChange}>
            ${this.agents.map(agent=>lit.qy`
              <option value=${agent.url} ?selected=${agent.url===this.agentUrl}>
                ${agent.name}
              </option>
            `)}
          </select>
          <span class="ai-badge">${this.isAI?"AI Powered":"Template Mode"}</span>
        </div>
      </div>
    `}handleAgentChange(e){const select=e.target,agent=this.agents.find(a=>a.url===select.value);agent&&this.dispatchEvent(new CustomEvent("agent-changed",{detail:{agent},bubbles:!0,composed:!0}))}};AssistantHeader.styles=lit.AH`
    .header {
      background: linear-gradient(135deg, #1473e6 0%, #0d66d0 100%);
      color: white;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }

    .header h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .header-logo {
      width: 28px;
      height: 28px;
      background: white;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      color: #1473e6;
      font-size: 14px;
    }

    .agent-selector {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .agent-selector select {
      padding: 8px 12px;
      border: none;
      border-radius: 4px;
      font-size: 13px;
      background: rgba(255,255,255,0.2);
      color: white;
      cursor: pointer;
    }

    .agent-selector select option {
      color: #333;
    }

    .ai-badge {
      font-size: 11px;
      padding: 4px 8px;
      border-radius: 12px;
      background: rgba(255,255,255,0.25);
      font-weight: 500;
    }
  `,__decorate([n({type:Array})],AssistantHeader.prototype,"agents",void 0),__decorate([n({type:String})],AssistantHeader.prototype,"agentUrl",void 0),__decorate([n({type:Boolean})],AssistantHeader.prototype,"isAI",void 0),AssistantHeader=__decorate([(t=>(e,o)=>{void 0!==o?o.addInitializer(()=>{customElements.define(t,e)}):customElements.define(t,e)})("assistant-header")],AssistantHeader);const assistant_header_stories={title:"Components/AssistantHeader",component:"assistant-header",tags:["autodocs"],argTypes:{agentUrl:{control:"text",description:"The URL of the selected agent."},isAI:{control:"boolean",description:"Whether the selected agent is AI-powered."},agents:{control:"object",description:"Array of available agents."}},args:{agentUrl:"http://localhost:10003",isAI:!0,agents:[{name:"Java Agent + Ollama",url:"http://localhost:10003",port:10003,hasAI:!0},{name:"Python Agent",url:"http://localhost:10002",port:10002,hasAI:!1}]}},Default={render:args=>lit.qy`
    <assistant-header
      .agentUrl=${args.agentUrl}
      .isAI=${args.isAI}
      .agents=${args.agents}
    ></assistant-header>
  `},__namedExportsOrder=["Default"];Default.parameters={...Default.parameters,docs:{...Default.parameters?.docs,source:{originalSource:"{\n  render: args => html`\n    <assistant-header\n      .agentUrl=${args.agentUrl}\n      .isAI=${args.isAI}\n      .agents=${args.agents}\n    ></assistant-header>\n  `\n}",...Default.parameters?.docs?.source}}}}}]);
//# sourceMappingURL=components-assistant-header-stories.f22a8c7e.iframe.bundle.js.map