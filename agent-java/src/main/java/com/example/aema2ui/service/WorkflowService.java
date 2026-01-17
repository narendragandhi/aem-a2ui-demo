package com.example.aema2ui.service;

import com.example.aema2ui.config.AemConfig;
import com.example.aema2ui.model.*;
import com.example.aema2ui.service.aem.AemHttpClient;
import com.example.aema2ui.service.aem.AemWorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing AEM workflow operations.
 * Supports both real AEM integration and mock mode for demo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final AemConfig aemConfig;
    private final AemHttpClient aemHttpClient;
    private final AemWorkflowClient aemWorkflowClient;

    private final Map<String, WorkflowInstance> workflows = new ConcurrentHashMap<>();

    // Predefined workflow models (fallback when AEM not connected)
    private final List<WorkflowModel> defaultModels = List.of(
            WorkflowModel.PUBLISH,
            WorkflowModel.ACTIVATE,
            WorkflowModel.REVIEW_APPROVE,
            WorkflowModel.TRANSLATION
    );

    /**
     * Check if using real AEM integration.
     */
    public boolean isUsingRealAem() {
        return aemConfig.isEnabled() && aemHttpClient.isConnected();
    }

    /**
     * Get all available workflow models.
     * Returns real AEM models when connected, or default models in mock mode.
     */
    public List<WorkflowModel> getAvailableWorkflows() {
        if (isUsingRealAem()) {
            try {
                List<WorkflowModel> aemModels = aemWorkflowClient.getWorkflowModels();
                if (!aemModels.isEmpty()) {
                    log.info("Loaded {} workflow models from AEM", aemModels.size());
                    return aemModels;
                }
            } catch (Exception e) {
                log.warn("Failed to load models from AEM, using defaults: {}", e.getMessage());
            }
        }
        return defaultModels;
    }

    /**
     * Get a workflow model by ID.
     */
    public Optional<WorkflowModel> getWorkflowModel(String modelId) {
        return getAvailableWorkflows().stream()
                .filter(m -> m.getId().equals(modelId))
                .findFirst();
    }

    /**
     * Submit content to a workflow.
     * Uses real AEM workflow when connected, otherwise uses mock.
     */
    public WorkflowInstance submitToWorkflow(String contentId, String contentPath,
                                              String workflowModelId, String initiatedBy,
                                              Map<String, Object> metadata) {
        WorkflowModel model = getWorkflowModel(workflowModelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow model: " + workflowModelId));

        // Try real AEM workflow submission
        if (isUsingRealAem() && contentPath != null) {
            try {
                Map<String, String> workflowMetadata = new HashMap<>();
                workflowMetadata.put("initiatedBy", initiatedBy);
                workflowMetadata.put("contentId", contentId);
                if (metadata != null) {
                    metadata.forEach((k, v) -> workflowMetadata.put(k, v != null ? v.toString() : ""));
                }

                Map<String, Object> result = aemWorkflowClient.startWorkflow(
                        model.getId(), // Use model path as ID
                        contentPath,
                        workflowMetadata
                );

                log.info("Started real AEM workflow for content: {}", contentPath);

                // Create local tracking instance
                return createLocalInstance(model, contentId, contentPath, initiatedBy, metadata, true);
            } catch (Exception e) {
                log.warn("Failed to start real AEM workflow, falling back to mock: {}", e.getMessage());
            }
        }

        // Fallback to mock workflow
        return createLocalInstance(model, contentId, contentPath, initiatedBy, metadata, false);
    }

    /**
     * Create a local workflow instance for tracking.
     */
    private WorkflowInstance createLocalInstance(WorkflowModel model, String contentId, String contentPath,
                                                   String initiatedBy, Map<String, Object> metadata,
                                                   boolean isRealAem) {
        List<WorkflowInstance.WorkflowStep> steps = createWorkflowSteps(model);

        Map<String, Object> instanceMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        instanceMetadata.put("realAem", isRealAem);

        WorkflowInstance instance = WorkflowInstance.builder()
                .id(UUID.randomUUID().toString())
                .workflowModelId(model.getId())
                .workflowModelName(model.getName())
                .contentId(contentId)
                .contentPath(contentPath)
                .status(WorkflowStatus.RUNNING)
                .currentStep(steps.get(0).getName())
                .currentStepIndex(0)
                .steps(steps)
                .initiatedBy(initiatedBy)
                .startedAt(Instant.now())
                .metadata(instanceMetadata)
                .build();

        // Mark first step as active
        steps.get(0).setStatus("active");
        steps.get(0).setStartedAt(Instant.now());

        workflows.put(instance.getId(), instance);
        return instance;
    }

    /**
     * Get workflow instance by ID.
     */
    public Optional<WorkflowInstance> getWorkflowInstance(String workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }

    /**
     * Get all workflow instances for content.
     */
    public List<WorkflowInstance> getWorkflowsByContentId(String contentId) {
        return workflows.values().stream()
                .filter(w -> contentId.equals(w.getContentId()))
                .sorted(Comparator.comparing(WorkflowInstance::getStartedAt).reversed())
                .toList();
    }

    /**
     * Get all workflow instances.
     */
    public List<WorkflowInstance> getAllWorkflows() {
        return new ArrayList<>(workflows.values());
    }

    /**
     * Advance workflow to next step.
     */
    public WorkflowInstance advanceWorkflow(String workflowId, String comment) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        if (instance.getStatus() != WorkflowStatus.RUNNING) {
            throw new IllegalStateException("Workflow is not running: " + workflowId);
        }

        List<WorkflowInstance.WorkflowStep> steps = instance.getSteps();
        int currentIndex = instance.getCurrentStepIndex();

        // Complete current step
        WorkflowInstance.WorkflowStep currentStep = steps.get(currentIndex);
        currentStep.setStatus("completed");
        currentStep.setCompletedAt(Instant.now());
        currentStep.setComment(comment);

        // Check if there's a next step
        if (currentIndex + 1 < steps.size()) {
            // Move to next step
            WorkflowInstance.WorkflowStep nextStep = steps.get(currentIndex + 1);
            nextStep.setStatus("active");
            nextStep.setStartedAt(Instant.now());

            instance.setCurrentStep(nextStep.getName());
            instance.setCurrentStepIndex(currentIndex + 1);
        } else {
            // Workflow complete
            instance.setStatus(WorkflowStatus.COMPLETED);
            instance.setCompletedAt(Instant.now());
            instance.setCurrentStep("Completed");
        }

        return instance;
    }

    /**
     * Cancel/abort a workflow.
     */
    public WorkflowInstance cancelWorkflow(String workflowId, String reason) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        instance.setStatus(WorkflowStatus.ABORTED);
        instance.setCompletedAt(Instant.now());

        // Mark current step as skipped
        int currentIndex = instance.getCurrentStepIndex();
        if (currentIndex < instance.getSteps().size()) {
            instance.getSteps().get(currentIndex).setStatus("skipped");
            instance.getSteps().get(currentIndex).setComment("Workflow cancelled: " + reason);
        }

        return instance;
    }

    /**
     * Suspend a workflow.
     */
    public WorkflowInstance suspendWorkflow(String workflowId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        instance.setStatus(WorkflowStatus.SUSPENDED);
        return instance;
    }

    /**
     * Resume a suspended workflow.
     */
    public WorkflowInstance resumeWorkflow(String workflowId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        if (instance.getStatus() != WorkflowStatus.SUSPENDED) {
            throw new IllegalStateException("Workflow is not suspended: " + workflowId);
        }

        instance.setStatus(WorkflowStatus.RUNNING);
        return instance;
    }

    /**
     * Create workflow steps based on model.
     */
    private List<WorkflowInstance.WorkflowStep> createWorkflowSteps(WorkflowModel model) {
        List<WorkflowInstance.WorkflowStep> steps = new ArrayList<>();

        switch (model.getId()) {
            case "publish":
                steps.add(createStep("review", "Content Review"));
                steps.add(createStep("approve", "Publication Approval"));
                steps.add(createStep("publish", "Publish to Live"));
                break;

            case "activate":
                steps.add(createStep("validate", "Content Validation"));
                steps.add(createStep("activate", "Activate Content"));
                break;

            case "review-approve":
                steps.add(createStep("initial-review", "Initial Review"));
                steps.add(createStep("legal-review", "Legal/Compliance Review"));
                steps.add(createStep("final-approval", "Final Approval"));
                steps.add(createStep("activate", "Activate Content"));
                break;

            case "translation":
                steps.add(createStep("extract", "Extract Content"));
                steps.add(createStep("translate", "Translation"));
                steps.add(createStep("review-translation", "Review Translation"));
                steps.add(createStep("publish-translated", "Publish Translated Content"));
                break;

            default:
                steps.add(createStep("process", "Process Content"));
                steps.add(createStep("complete", "Complete"));
        }

        return steps;
    }

    private WorkflowInstance.WorkflowStep createStep(String id, String name) {
        return WorkflowInstance.WorkflowStep.builder()
                .id(id)
                .name(name)
                .status("pending")
                .build();
    }
}
