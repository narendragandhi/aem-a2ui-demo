package com.example.aema2ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an AEM workflow model template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowModel {

    private String id;
    private String name;
    private String description;
    private String path;  // AEM path like /var/workflow/models/...
    private boolean requiresApproval;

    /**
     * Common AEM workflow models.
     */
    public static final WorkflowModel PUBLISH = WorkflowModel.builder()
            .id("publish")
            .name("Request for Publication")
            .description("Submits content for publication to the live site")
            .path("/var/workflow/models/request_for_publication")
            .requiresApproval(true)
            .build();

    public static final WorkflowModel ACTIVATE = WorkflowModel.builder()
            .id("activate")
            .name("Request for Activation")
            .description("Activates content immediately")
            .path("/var/workflow/models/request_for_activation")
            .requiresApproval(false)
            .build();

    public static final WorkflowModel REVIEW_APPROVE = WorkflowModel.builder()
            .id("review-approve")
            .name("Review and Approve")
            .description("Multi-step review workflow with approval gate")
            .path("/var/workflow/models/review_and_approve")
            .requiresApproval(true)
            .build();

    public static final WorkflowModel TRANSLATION = WorkflowModel.builder()
            .id("translation")
            .name("Translation Request")
            .description("Request content translation to other languages")
            .path("/var/workflow/models/translation_request")
            .requiresApproval(true)
            .build();
}
