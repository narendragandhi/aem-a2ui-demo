package com.example.aema2ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRootEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void testAgentCard() throws Exception {
        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AEM Content Assistant"))
                .andExpect(jsonPath("$.capabilities.a2ui.version").value("0.8"));
    }

    @Test
    void testCreateTaskWithHeroKeyword() throws Exception {
        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "create a hero banner"}]
                }
            }
            """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(3));
    }

    @Test
    void testCreateTaskWithProductKeyword() throws Exception {
        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "product card"}]
                }
            }
            """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.messages[2].dataModelUpdate.contents[?(@.key=='title')].valueString",
                        hasItem(containsString("Product"))));
    }

    @Test
    void testA2UIMessageStructure() throws Exception {
        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "test"}]
                }
            }
            """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                // First message: beginRendering
                .andExpect(jsonPath("$.messages[0].beginRendering.surfaceId").exists())
                .andExpect(jsonPath("$.messages[0].beginRendering.root").value("root"))
                // Second message: surfaceUpdate
                .andExpect(jsonPath("$.messages[1].surfaceUpdate.surfaceId").exists())
                .andExpect(jsonPath("$.messages[1].surfaceUpdate.components").isArray())
                // Third message: dataModelUpdate
                .andExpect(jsonPath("$.messages[2].dataModelUpdate.surfaceId").exists())
                .andExpect(jsonPath("$.messages[2].dataModelUpdate.path").value("suggestion"))
                .andExpect(jsonPath("$.messages[2].dataModelUpdate.contents").isArray());
    }

    @Test
    void testSurfaceIdConsistency() throws Exception {
        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "hero"}]
                }
            }
            """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                // All three messages should have surfaceId starting with "suggestion_"
                .andExpect(jsonPath("$.messages[0].beginRendering.surfaceId", startsWith("suggestion_")))
                .andExpect(jsonPath("$.messages[1].surfaceUpdate.surfaceId", startsWith("suggestion_")))
                .andExpect(jsonPath("$.messages[2].dataModelUpdate.surfaceId", startsWith("suggestion_")));
    }

    @Test
    void testApplySuggestionAction() throws Exception {
        mockMvc.perform(post("/actions/apply_suggestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testRegenerateAction() throws Exception {
        mockMvc.perform(post("/actions/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    void testUnknownAction() throws Exception {
        mockMvc.perform(post("/actions/unknown_action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
