package com.example.aema2ui.service.aem;

import com.example.aema2ui.config.AemConfig;
import com.example.aema2ui.model.WorkflowModel;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Client for AEM Workflow API.
 * Integrates with real AEM workflow engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AemWorkflowClient {

    private final AemHttpClient httpClient;
    private final AemConfig config;

    /**
     * Get all available workflow models from AEM
     */
    public List<WorkflowModel> getWorkflowModels() {
        if (!httpClient.isConnected()) {
            log.warn("AEM not connected, returning empty workflow models");
            return Collections.emptyList();
        }

        try {
            JsonNode response = httpClient.get("/etc/workflow/models.json");
            List<WorkflowModel> models = new ArrayList<>();

            if (response.isArray()) {
                for (JsonNode node : response) {
                    WorkflowModel model = parseWorkflowModel(node);
                    if (model != null) {
                        models.add(model);
                    }
                }
            }

            log.info("Loaded {} workflow models from AEM", models.size());
            return models;
        } catch (Exception e) {
            log.error("Failed to load workflow models from AEM", e);
            return Collections.emptyList();
        }
    }

    /**
     * Start a new workflow instance
     */
    public Map<String, Object> startWorkflow(String modelPath, String payloadPath, Map<String, String> metadata) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("model", modelPath);
            formData.put("payload", payloadPath);
            formData.put("payloadType", "JCR_PATH");

            if (metadata != null) {
                metadata.forEach((key, value) -> formData.put("metaData." + key, value));
            }

            String response = httpClient.postForm("/etc/workflow/instances", formData);
            log.info("Started workflow: model={}, payload={}", modelPath, payloadPath);

            // Parse response to get instance ID
            // AEM returns HTML or redirect, extract instance path
            return Map.of(
                "success", true,
                "message", "Workflow started successfully",
                "payload", payloadPath
            );
        } catch (Exception e) {
            log.error("Failed to start workflow", e);
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Get workflow instance status
     */
    public Map<String, Object> getWorkflowStatus(String instanceId) {
        if (!httpClient.isConnected()) {
            return Map.of("error", "AEM not connected");
        }

        try {
            JsonNode response = httpClient.get("/etc/workflow/instances/" + instanceId + ".json");

            return Map.of(
                "id", instanceId,
                "status", getTextValue(response, "status", "UNKNOWN"),
                "state", getTextValue(response, "state", "UNKNOWN"),
                "startTime", getTextValue(response, "startTime", ""),
                "model", getTextValue(response, "model", "")
            );
        } catch (Exception e) {
            log.error("Failed to get workflow status: {}", instanceId, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get user's workflow inbox (pending work items)
     */
    public List<Map<String, Object>> getInbox() {
        if (!httpClient.isConnected()) {
            return Collections.emptyList();
        }

        try {
            JsonNode response = httpClient.get("/bin/workflow/inbox");
            List<Map<String, Object>> items = new ArrayList<>();

            if (response.has("workflows") && response.get("workflows").isArray()) {
                for (JsonNode item : response.get("workflows")) {
                    items.add(Map.of(
                        "id", getTextValue(item, "uri", ""),
                        "title", getTextValue(item, "currentAssignee/item/title", "Work Item"),
                        "payload", getTextValue(item, "payload", ""),
                        "model", getTextValue(item, "model", ""),
                        "startTime", getTextValue(item, "startTime", "")
                    ));
                }
            }

            return items;
        } catch (Exception e) {
            log.error("Failed to get workflow inbox", e);
            return Collections.emptyList();
        }
    }

    /**
     * Complete a work item (advance workflow)
     */
    public void completeWorkItem(String workItemPath, String comment) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("route", "complete");
            if (comment != null && !comment.isEmpty()) {
                formData.put("comment", comment);
            }

            httpClient.postForm(workItemPath, formData);
            log.info("Completed work item: {}", workItemPath);
        } catch (Exception e) {
            log.error("Failed to complete work item: {}", workItemPath, e);
            throw new RuntimeException("Failed to complete work item: " + e.getMessage(), e);
        }
    }

    /**
     * Terminate a workflow instance
     */
    public void terminateWorkflow(String instancePath) {
        if (!httpClient.isConnected()) {
            throw new IllegalStateException("AEM not connected");
        }

        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("state", "ABORTED");
            formData.put("terminateComment", "Terminated via API");

            httpClient.postForm(instancePath, formData);
            log.info("Terminated workflow: {}", instancePath);
        } catch (Exception e) {
            log.error("Failed to terminate workflow: {}", instancePath, e);
            throw new RuntimeException("Failed to terminate workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Parse workflow model from JSON response
     */
    private WorkflowModel parseWorkflowModel(JsonNode node) {
        try {
            String path = getTextValue(node, "path", null);
            if (path == null) return null;

            String title = getTextValue(node, "title", path);
            String description = getTextValue(node, "description", "");

            // Map AEM workflow model to our WorkflowModel
            return WorkflowModel.builder()
                .id(path)
                .name(title)
                .description(description)
                .path(path)
                .requiresApproval(title.toLowerCase().contains("approv") ||
                                  title.toLowerCase().contains("review"))
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse workflow model", e);
            return null;
        }
    }

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node == null) return defaultValue;

        // Handle nested paths like "currentAssignee/item/title"
        String[] parts = field.split("/");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || !current.has(part)) {
                return defaultValue;
            }
            current = current.get(part);
        }

        return current != null && current.isTextual() ? current.asText() : defaultValue;
    }
}
