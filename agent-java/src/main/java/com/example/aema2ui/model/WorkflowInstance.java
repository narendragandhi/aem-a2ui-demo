package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a running or completed workflow instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {

    private String id;
    private String workflowModelId;
    private String workflowModelName;
    private String contentId;
    private String contentPath;
    private WorkflowStatus status;

    private String currentStep;
    private int currentStepIndex;
    @Builder.Default
    private List<WorkflowStep> steps = new ArrayList<>();

    private String initiatedBy;
    private Instant startedAt;
    private Instant completedAt;

    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStep {
        private String id;
        private String name;
        private String status;  // pending, active, completed, skipped
        private String assignee;
        private Instant startedAt;
        private Instant completedAt;
        private String comment;
    }
}
