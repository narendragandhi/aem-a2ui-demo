package com.example.aema2ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class A2UIMessageBuilderTest {

    private A2UIMessageBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new A2UIMessageBuilder();
    }

    @Test
    void testBuildBeginRendering() {
        Map<String, Object> message = builder.beginRendering("test-surface", "root");

        assertTrue(message.containsKey("beginRendering"));
        @SuppressWarnings("unchecked")
        Map<String, Object> beginRendering = (Map<String, Object>) message.get("beginRendering");
        assertEquals("test-surface", beginRendering.get("surfaceId"));
        assertEquals("root", beginRendering.get("root"));
    }

    @Test
    void testBuildSurfaceUpdate() {
        List<Map<String, Object>> components = List.of(
                Map.of("id", "root", "component", Map.of("Column", Map.of()))
        );

        Map<String, Object> message = builder.surfaceUpdate("test-surface", components);

        assertTrue(message.containsKey("surfaceUpdate"));
        @SuppressWarnings("unchecked")
        Map<String, Object> surfaceUpdate = (Map<String, Object>) message.get("surfaceUpdate");
        assertEquals("test-surface", surfaceUpdate.get("surfaceId"));
        assertNotNull(surfaceUpdate.get("components"));
    }

    @Test
    void testBuildDataModelUpdate() {
        List<Map<String, Object>> contents = List.of(
                Map.of("key", "title", "valueString", "Test Title")
        );

        Map<String, Object> message = builder.dataModelUpdate("test-surface", "data", contents);

        assertTrue(message.containsKey("dataModelUpdate"));
        @SuppressWarnings("unchecked")
        Map<String, Object> dataModelUpdate = (Map<String, Object>) message.get("dataModelUpdate");
        assertEquals("test-surface", dataModelUpdate.get("surfaceId"));
        assertEquals("data", dataModelUpdate.get("path"));
        assertNotNull(dataModelUpdate.get("contents"));
    }

    @Test
    void testBuildTextComponent() {
        Map<String, Object> component = builder.text("header", "Hello World", "h1");

        assertEquals("header", component.get("id"));
        assertTrue(component.containsKey("component"));
        @SuppressWarnings("unchecked")
        Map<String, Object> textComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("Text");
        assertNotNull(textComp);
        assertEquals("h1", textComp.get("usageHint"));
    }

    @Test
    void testBuildButtonComponent() {
        Map<String, Object> component = builder.button("btn", "Click Me", "do_something");

        assertEquals("btn", component.get("id"));
        assertTrue(component.containsKey("component"));
        @SuppressWarnings("unchecked")
        Map<String, Object> buttonComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("Button");
        assertNotNull(buttonComp);
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) buttonComp.get("action");
        assertEquals("do_something", action.get("name"));
    }

    @Test
    void testBuildTextFieldComponent() {
        Map<String, Object> component = builder.textField("input", "Name", "/data/name", 1);

        assertEquals("input", component.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> tfComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("TextField");
        assertNotNull(tfComp);
        @SuppressWarnings("unchecked")
        Map<String, Object> value = (Map<String, Object>) tfComp.get("value");
        assertEquals("/data/name", value.get("path"));
    }

    @Test
    void testBuildColumnComponent() {
        Map<String, Object> component = builder.column("col", List.of("child1", "child2"));

        assertEquals("col", component.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> colComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("Column");
        assertNotNull(colComp);
        @SuppressWarnings("unchecked")
        Map<String, Object> children = (Map<String, Object>) colComp.get("children");
        assertTrue(children.containsKey("explicitList"));
    }

    @Test
    void testBuildRowComponent() {
        Map<String, Object> component = builder.row("row", List.of("child1", "child2"));

        assertEquals("row", component.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> rowComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("Row");
        assertNotNull(rowComp);
    }

    @Test
    void testBuildImageComponent() {
        Map<String, Object> component = builder.image("img", "/data/imageUrl", "Alt text");

        assertEquals("img", component.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> imgComp = (Map<String, Object>) ((Map<String, Object>) component.get("component")).get("Image");
        assertNotNull(imgComp);
    }

    @Test
    void testDataStringHelper() {
        Map<String, Object> data = builder.dataString("title", "Test Title");

        assertEquals("title", data.get("key"));
        assertEquals("Test Title", data.get("valueString"));
    }

    @Test
    void testDataNumberHelper() {
        Map<String, Object> data = builder.dataNumber("count", 42);

        assertEquals("count", data.get("key"));
        assertEquals(42, data.get("valueNumber"));
    }

    @Test
    void testDataBooleanHelper() {
        Map<String, Object> data = builder.dataBoolean("enabled", true);

        assertEquals("enabled", data.get("key"));
        assertEquals(true, data.get("valueBoolean"));
    }
}
