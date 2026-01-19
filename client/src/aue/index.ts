/**
 * AEM Universal Editor Integration
 *
 * This module provides integration between Lit Web Components
 * and AEM Universal Editor for inline content authoring.
 *
 * ## Quick Start
 *
 * 1. Initialize Universal Editor in your app:
 * ```typescript
 * import { initUniversalEditor } from './aue';
 *
 * initUniversalEditor({
 *   aemAuthorUrl: 'http://localhost:4502',
 *   contentPath: '/content/my-site',
 *   enabled: true,
 * });
 * ```
 *
 * 2. Add editability to components:
 * ```typescript
 * import { getEditableAttributes, getFieldAttributes } from './aue';
 *
 * // In your Lit component render():
 * const componentAttrs = getEditableAttributes(content, 'hero');
 * const titleAttrs = getFieldAttributes('title');
 * ```
 *
 * ## Component Definitions
 *
 * The following JSON files define component models for Universal Editor:
 * - component-models.json - Field definitions for the properties panel
 * - component-definitions.json - Component registry with icons and grouping
 * - component-filters.json - Controls which components appear in different contexts
 */

export {
  initUniversalEditor,
  getEditableAttributes,
  getFieldAttributes,
  getRichTextFieldAttributes,
  getMediaFieldAttributes,
  getContainerAttributes,
  spreadAttributes,
  isUEEnabled,
  getUEConfig,
  onUEEvent,
  notifyContentChange,
  type UEConfig,
} from './universal-editor.js';
