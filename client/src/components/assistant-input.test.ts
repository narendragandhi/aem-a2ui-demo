import { expect } from 'chai';
import sinon from 'sinon';
import { html, fixture, waitUntil } from '@open-wc/testing';
import { AssistantInput } from './assistant-input.js';
import './assistant-input.js';

describe('AssistantInput', () => {
  let element: AssistantInput;

  beforeEach(async () => {
    element = await fixture(html`<assistant-input></assistant-input>`);
  });

  it('should render the input section', () => {
    const h2 = element.shadowRoot?.querySelector('h2');
    expect(h2).to.exist;
    expect(h2?.textContent).to.include('Describe Your Content');
  });

  it('should dispatch a prompt-changed event when the input value changes', async () => {
    const spy = sinon.spy();
    element.addEventListener('prompt-changed', spy);
    const input = element.shadowRoot?.querySelector('input');
    if (input) {
      input.value = 'test prompt';
      input.dispatchEvent(new Event('input'));
    }
    await waitUntil(() => spy.called);
    expect(spy).to.have.been.calledOnce;
    const event = spy.getCall(0).args[0] as CustomEvent;
    expect(event.detail.prompt).to.equal('test prompt');
  });

  it('should dispatch a generate-content event when the generate button is clicked', async () => {
    const spy = sinon.spy();
    element.addEventListener('generate-content', spy);
    const button = element.shadowRoot?.querySelector('.generate-btn');
    (button as HTMLElement)?.click();
    await waitUntil(() => spy.called);
    expect(spy).to.have.been.calledOnce;
  });

  it('should dispatch a generate-content event when Enter is pressed in the input', async () => {
    const spy = sinon.spy();
    element.addEventListener('generate-content', spy);
    const input = element.shadowRoot?.querySelector('input');
    input?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    await waitUntil(() => spy.called);
    expect(spy).to.have.been.calledOnce;
  });
});
