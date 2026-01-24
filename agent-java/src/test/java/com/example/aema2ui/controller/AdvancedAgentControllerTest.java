package com.example.aema2ui.controller;

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
@org.springframework.test.context.ActiveProfiles("test")
class AdvancedAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAdvancedRoot() throws Exception {
        mockMvc.perform(get("/advanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.name").value("Advanced AEM A2UI Agent (Java)"))
                .andExpect(jsonPath("$.demos").exists());
    }

    @Test
    void testAdvancedAgentCard() throws Exception {
        mockMvc.perform(get("/advanced/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities.a2ui.version").value("0.8"));
    }

    // Dynamic List Demo Tests
    @Test
    void testDemoList() throws Exception {
        mockMvc.perform(get("/advanced/demo/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    void testListHasTemplateComponent() throws Exception {
        mockMvc.perform(get("/advanced/demo/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].surfaceUpdate.components[?(@.component.List)]").exists());
    }

    // Wizard Demo Tests
    @Test
    void testDemoWizardStep1() throws Exception {
        mockMvc.perform(get("/advanced/demo/wizard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testDemoWizardStep2() throws Exception {
        mockMvc.perform(get("/advanced/demo/wizard_2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testDemoWizardStep3() throws Exception {
        mockMvc.perform(get("/advanced/demo/wizard_3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testWizardNavigationAction() throws Exception {
        mockMvc.perform(post("/advanced/actions/wizard_navigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"step\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messages").isArray());
    }

    // Tabs Demo Tests
    @Test
    void testDemoTabs() throws Exception {
        mockMvc.perform(get("/advanced/demo/tabs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testTabsHasTabsComponent() throws Exception {
        mockMvc.perform(get("/advanced/demo/tabs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].surfaceUpdate.components[?(@.component.Tabs)]").exists());
    }

    // AI Demo Tests
    @Test
    void testDemoAI() throws Exception {
        mockMvc.perform(get("/advanced/demo/ai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testGenerateContentAction() throws Exception {
        mockMvc.perform(post("/advanced/actions/generate_content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // Collaboration Demo Tests
    @Test
    void testDemoCollab() throws Exception {
        mockMvc.perform(get("/advanced/demo/collab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    // Tasks Endpoint Tests
    @Test
    void testTaskWithListKeyword() throws Exception {
        String requestBody = """
            {
                "message": {
                    "parts": [{"text": "show me the asset browser"}]
                }
            }
            """;

        mockMvc.perform(post("/advanced/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void testTaskWithWizardKeyword() throws Exception {
        String requestBody = """
            {
                "message": {
                    "parts": [{"text": "help me create component step by step"}]
                }
            }
            """;

        mockMvc.perform(post("/advanced/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    // Unknown Demo Test
    @Test
    void testUnknownDemo() throws Exception {
        mockMvc.perform(get("/advanced/demo/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].error").value(containsString("Unknown demo")));
    }
}
