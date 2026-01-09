package com.example.aema2ui.config;

import com.example.aema2ui.model.TaskRequest;
import org.springframework.stereotype.Component;

/**
 * Input validation utilities.
 */
@Component
public class ValidationConfig {

    private static final int MAX_TEXT_LENGTH = 10_000;
    private static final int MAX_PARTS = 100;

    /**
     * Validate a task request.
     * @param request the request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateTaskRequest(TaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getMessage() == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        if (request.getMessage().getParts() == null || request.getMessage().getParts().isEmpty()) {
            throw new IllegalArgumentException("Message must have at least one part");
        }

        if (request.getMessage().getParts().size() > MAX_PARTS) {
            throw new IllegalArgumentException("Too many message parts (max: " + MAX_PARTS + ")");
        }

        for (TaskRequest.MessagePart part : request.getMessage().getParts()) {
            if (part.getText() != null && part.getText().length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("Text too long (max: " + MAX_TEXT_LENGTH + " characters)");
            }
        }
    }

    /**
     * Sanitize user input to prevent injection attacks.
     * @param input the input to sanitize
     * @return sanitized input
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Remove control characters except newlines and tabs
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                   .trim();
    }
}
