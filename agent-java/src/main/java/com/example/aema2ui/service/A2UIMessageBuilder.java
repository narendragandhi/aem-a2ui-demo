package com.example.aema2ui.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builder for A2UI protocol messages.
 * Generates valid A2UI JSON structures for the Lit renderer.
 */
@Service
public class A2UIMessageBuilder {

    /**
     * Creates a beginRendering message.
     */
    public Map<String, Object> beginRendering(String surfaceId, String rootComponentId) {
        Map<String, Object> beginRendering = new LinkedHashMap<>();
        beginRendering.put("surfaceId", surfaceId);
        beginRendering.put("root", rootComponentId);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("beginRendering", beginRendering);
        return message;
    }

    /**
     * Creates a surfaceUpdate message with components.
     */
    public Map<String, Object> surfaceUpdate(String surfaceId, List<Map<String, Object>> components) {
        Map<String, Object> surfaceUpdate = new LinkedHashMap<>();
        surfaceUpdate.put("surfaceId", surfaceId);
        surfaceUpdate.put("components", components);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("surfaceUpdate", surfaceUpdate);
        return message;
    }

    /**
     * Creates a dataModelUpdate message.
     */
    public Map<String, Object> dataModelUpdate(String surfaceId, String path, List<Map<String, Object>> contents) {
        Map<String, Object> dataModelUpdate = new LinkedHashMap<>();
        dataModelUpdate.put("surfaceId", surfaceId);
        dataModelUpdate.put("path", path);
        dataModelUpdate.put("contents", contents);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("dataModelUpdate", dataModelUpdate);
        return message;
    }

    // ========== Component Builders ==========

    /**
     * Creates a component definition.
     */
    public Map<String, Object> component(String id, String type, Map<String, Object> properties) {
        Map<String, Object> componentType = new LinkedHashMap<>();
        componentType.put(type, properties);

        Map<String, Object> component = new LinkedHashMap<>();
        component.put("id", id);
        component.put("component", componentType);
        return component;
    }

    /**
     * Creates a Text component.
     */
    public Map<String, Object> text(String id, String text, String usageHint) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", literalString(text));
        if (usageHint != null) {
            props.put("usageHint", usageHint);
        }
        return component(id, "Text", props);
    }

    /**
     * Creates an Image component with data binding.
     */
    public Map<String, Object> image(String id, String urlPath, String altText) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("url", path(urlPath));
        props.put("altText", literalString(altText));
        return component(id, "Image", props);
    }

    /**
     * Creates a TextField component.
     */
    public Map<String, Object> textField(String id, String label, String valuePath, Integer lines) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", literalString(label));
        props.put("value", path(valuePath));
        if (lines != null && lines > 1) {
            props.put("lines", lines);
        }
        return component(id, "TextField", props);
    }

    /**
     * Creates a Button component.
     */
    public Map<String, Object> button(String id, String label, String actionName) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("name", actionName);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("label", literalString(label));
        props.put("action", action);
        return component(id, "Button", props);
    }

    /**
     * Creates a Column layout component.
     */
    public Map<String, Object> column(String id, List<String> childIds) {
        Map<String, Object> children = new LinkedHashMap<>();
        children.put("explicitList", childIds);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", children);
        return component(id, "Column", props);
    }

    /**
     * Creates a Row layout component.
     */
    public Map<String, Object> row(String id, List<String> childIds) {
        Map<String, Object> children = new LinkedHashMap<>();
        children.put("explicitList", childIds);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("children", children);
        return component(id, "Row", props);
    }

    // ========== Value Helpers ==========

    /**
     * Creates a literal string value.
     */
    public Map<String, Object> literalString(String value) {
        Map<String, Object> literal = new LinkedHashMap<>();
        literal.put("literalString", value);
        return literal;
    }

    /**
     * Creates a path reference for data binding.
     */
    public Map<String, Object> path(String pathValue) {
        Map<String, Object> pathRef = new LinkedHashMap<>();
        pathRef.put("path", pathValue);
        return pathRef;
    }

    // ========== Data Model Helpers ==========

    /**
     * Creates a data string entry for data model updates.
     */
    public Map<String, Object> dataString(String key, String value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueString", value);
        return entry;
    }

    /**
     * Creates a data number entry for data model updates.
     */
    public Map<String, Object> dataNumber(String key, Number value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueNumber", value);
        return entry;
    }

    /**
     * Creates a data boolean entry for data model updates.
     */
    public Map<String, Object> dataBoolean(String key, Boolean value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("valueBoolean", value);
        return entry;
    }
}
