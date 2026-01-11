package com.example.aema2ui.controller;

import com.example.aema2ui.model.TaskRequest;
import com.example.aema2ui.model.TaskResponse;
import com.example.aema2ui.service.ContentSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the A2UI agent endpoints.
 * Implements A2A-compatible endpoints for agent discovery and task handling.
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final ContentSuggestionService suggestionService;

    /**
     * Health check endpoint.
     */
    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "status", "ok",
            "name", "AEM Content Assistant (Java)",
            "version", "1.0.0"
        );
    }

    /**
     * A2A Agent Card for discovery.
     * This allows A2A clients to discover the agent's capabilities.
     */
    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> agentCard() {
        return Map.of(
            "name", "AEM Content Assistant",
            "description", "AI assistant for AEM content authoring (Java implementation)",
            "url", "http://localhost:10003",
            "version", "1.0.0",
            "capabilities", Map.of(
                "a2ui", Map.of("version", "0.8")
            )
        );
    }

    /**
     * Handle A2A task requests and return A2UI messages.
     */
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request) {
        // Extract user text from request
        String userText = extractUserText(request);

        // Generate multiple variations for the client
        var suggestionsResult = suggestionService.generateMultipleSuggestions(userText, 3);

        TaskResponse response = TaskResponse.builder()
            .id(UUID.randomUUID().toString())
            .status("completed")
            .messages(suggestionsResult.messages())
            .artifacts(suggestionsResult.artifacts())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Handle user actions from the UI.
     */
    @PostMapping("/actions/{actionName}")
    public ResponseEntity<Map<String, Object>> handleAction(
            @PathVariable String actionName,
            @RequestBody(required = false) Map<String, Object> context) {

        return switch (actionName) {
            case "apply_suggestion" -> ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Content applied to component"
            ));
            case "regenerate" -> ResponseEntity.ok(Map.of(
                "success", true,
                "messages", suggestionService.generateSuggestion("random")
            ));
            default -> ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Unknown action: " + actionName
            ));
        };
    }

    /**
     * Extracts user text from the task request.
     */
    private String extractUserText(TaskRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().getParts() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (TaskRequest.MessagePart part : request.getMessage().getParts()) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }
}
