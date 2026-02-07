package com.example.aema2ui.service;

import com.example.aema2ui.agent.AemContentAgent;
import com.example.aema2ui.model.ContentSuggestion;
import com.example.aema2ui.model.UserInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE Streaming service for real-time content generation.
 *
 * Implements AG-UI protocol event types:
 * - RUN_STARTED: Generation begins
 * - TEXT_MESSAGE_START: New text field beginning
 * - TEXT_MESSAGE_DELTA: Incremental text update
 * - TEXT_MESSAGE_END: Field complete
 * - RUN_FINISHED: Generation complete
 *
 * This creates the "typing" effect where content appears progressively.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingContentService {

    private final AemContentAgent contentAgent;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // AG-UI Event Types
    public static final String RUN_STARTED = "RUN_STARTED";
    public static final String TEXT_MESSAGE_START = "TEXT_MESSAGE_START";
    public static final String TEXT_MESSAGE_DELTA = "TEXT_MESSAGE_DELTA";
    public static final String TEXT_MESSAGE_END = "TEXT_MESSAGE_END";
    public static final String TOOL_CALL_START = "TOOL_CALL_START";
    public static final String TOOL_CALL_END = "TOOL_CALL_END";
    public static final String STATE_DELTA = "STATE_DELTA";
    public static final String RUN_FINISHED = "RUN_FINISHED";
    public static final String RUN_ERROR = "RUN_ERROR";

    /**
     * Stream content generation with real-time updates.
     * @param useAi If false, uses templates for instant response. If true, uses LLM (slower).
     */
    public void streamContentGeneration(String userInput, String componentType, SseEmitter emitter, boolean useAi) {
        String runId = UUID.randomUUID().toString();

        // Track emitter state to avoid writing after completion
        final java.util.concurrent.atomic.AtomicBoolean emitterCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Set up completion callbacks
        emitter.onCompletion(() -> {
            emitterCompleted.set(true);
            log.debug("SSE completed for runId: {}", runId);
        });
        emitter.onTimeout(() -> {
            emitterCompleted.set(true);
            log.warn("SSE timeout for runId: {}", runId);
        });
        emitter.onError(e -> {
            emitterCompleted.set(true);
            if (!isClientDisconnection(e)) {
                log.error("SSE error for runId: {} - {}", runId, e.getMessage());
            }
        });

        executor.execute(() -> {
            try {
                // 1. Emit RUN_STARTED
                if (emitterCompleted.get()) return;
                emitEvent(emitter, RUN_STARTED, Map.of(
                    "runId", runId,
                    "threadId", Thread.currentThread().getName(),
                    "input", userInput
                ));

                // 2. Generate content
                // When useAi=false, skip LLM entirely for instant response
                ContentSuggestion content;
                if (useAi) {
                    // Full LLM path (slower but smarter)
                    UserInput parsed = contentAgent.parseUserIntent(userInput);
                    if (componentType != null && !componentType.isEmpty()) {
                        parsed = UserInput.builder()
                            .rawText(parsed.getRawText())
                            .detectedComponentType(componentType)
                            .targetAudience(parsed.getTargetAudience())
                            .brandStyle(parsed.getBrandStyle())
                            .toneOfVoice(parsed.getToneOfVoice())
                            .build();
                    }
                    content = contentAgent.generateContent(parsed);
                } else {
                    // Template path (instant)
                    content = contentAgent.generateTemplateContent(userInput, componentType);
                }

                // 4. Stream each field progressively (check emitter state before each)
                if (!emitterCompleted.get()) streamField(emitter, runId, "title", content.getTitle(), emitterCompleted);
                if (!emitterCompleted.get()) streamField(emitter, runId, "subtitle", content.getSubtitle(), emitterCompleted);
                if (!emitterCompleted.get()) streamField(emitter, runId, "description", content.getDescription(), emitterCompleted);

                if (!emitterCompleted.get() && content.getCtaText() != null) {
                    streamField(emitter, runId, "ctaText", content.getCtaText(), emitterCompleted);
                }
                if (!emitterCompleted.get() && content.getCtaUrl() != null) {
                    streamField(emitter, runId, "ctaUrl", content.getCtaUrl(), emitterCompleted);
                }
                if (!emitterCompleted.get() && content.getPrice() != null) {
                    streamField(emitter, runId, "price", content.getPrice(), emitterCompleted);
                }
                if (!emitterCompleted.get() && content.getImageUrl() != null) {
                    streamField(emitter, runId, "imageUrl", content.getImageUrl(), emitterCompleted);
                }

                // 5. Emit complete content as state
                if (!emitterCompleted.get()) {
                    emitEvent(emitter, STATE_DELTA, Map.of(
                        "runId", runId,
                        "delta", Map.of(
                            "content", content,
                            "componentType", content.getComponentType()
                        )
                    ));
                }

                // 6. Emit RUN_FINISHED
                if (!emitterCompleted.get()) {
                    emitEvent(emitter, RUN_FINISHED, Map.of(
                        "runId", runId,
                        "status", "completed",
                        "content", content
                    ));
                    emitter.complete();
                }

            } catch (Exception e) {
                // Check if this is a client disconnection (expected behavior)
                if (isClientDisconnection(e)) {
                    log.debug("Client disconnected during streaming (runId: {})", runId);
                } else {
                    log.error("Streaming error for runId {}: {}", runId, e.getMessage());
                    // Only try to send error if emitter is still active
                    if (!emitterCompleted.get()) {
                        try {
                            emitEvent(emitter, RUN_ERROR, Map.of(
                                "runId", runId,
                                "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
                            ));
                            emitter.completeWithError(e);
                        } catch (Exception ignored) {
                            // Emitter already closed, ignore
                        }
                    }
                }
            }
        });
    }

    /**
     * Stream a single field - sends complete value immediately (no artificial delay).
     */
    private void streamField(SseEmitter emitter, String runId, String fieldName, String value,
            java.util.concurrent.atomic.AtomicBoolean emitterCompleted)
            throws IOException, InterruptedException {
        if (value == null || value.isEmpty()) return;
        if (emitterCompleted.get()) return;

        String messageId = UUID.randomUUID().toString();

        // TEXT_MESSAGE_START
        emitEvent(emitter, TEXT_MESSAGE_START, Map.of(
            "runId", runId,
            "messageId", messageId,
            "field", fieldName
        ));

        // Send complete value immediately (no artificial delays)
        emitEvent(emitter, TEXT_MESSAGE_DELTA, Map.of(
            "runId", runId,
            "messageId", messageId,
            "field", fieldName,
            "delta", value,
            "content", value
        ));

        if (emitterCompleted.get()) return;

        // TEXT_MESSAGE_END
        emitEvent(emitter, TEXT_MESSAGE_END, Map.of(
            "runId", runId,
            "messageId", messageId,
            "field", fieldName,
            "content", value
        ));
    }

    /**
     * Emit an SSE event with AG-UI format.
     */
    private void emitEvent(SseEmitter emitter, String eventType, Map<String, Object> data)
            throws IOException {
        try {
            Map<String, Object> event = Map.of(
                "type", eventType,
                "timestamp", System.currentTimeMillis(),
                "data", data
            );

            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(json));

            log.debug("Emitted SSE event: {} - {}", eventType, data.get("field"));
        } catch (Exception e) {
            if (isClientDisconnection(e)) {
                log.debug("Client disconnected, cannot emit SSE event: {}", eventType);
            } else {
                log.error("Failed to emit SSE event: {}", e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Create a configured SseEmitter with appropriate timeout.
     * Note: Callbacks are set in streamContentGeneration to track state.
     */
    public SseEmitter createEmitter() {
        return new SseEmitter(120000L); // 2 minute timeout
    }

    /**
     * Stream raw LLM output directly - true streaming like CLI.
     * Tokens are sent to client as soon as Ollama generates them.
     */
    public void streamRawGeneration(String prompt, SseEmitter emitter) {
        String runId = UUID.randomUUID().toString();
        final java.util.concurrent.atomic.AtomicBoolean emitterCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);

        emitter.onCompletion(() -> emitterCompleted.set(true));
        emitter.onTimeout(() -> emitterCompleted.set(true));
        emitter.onError(e -> emitterCompleted.set(true));

        executor.execute(() -> {
            try {
                // Emit RUN_STARTED
                emitEvent(emitter, RUN_STARTED, Map.of("runId", runId, "mode", "raw_streaming"));

                StringBuilder fullResponse = new StringBuilder();
                String messageId = UUID.randomUUID().toString();

                // Start message
                emitEvent(emitter, TEXT_MESSAGE_START, Map.of(
                    "runId", runId,
                    "messageId", messageId,
                    "field", "content"
                ));

                // True streaming from LLM
                llmService.generateStreaming(prompt,
                    // onToken - called for each token from Ollama
                    token -> {
                        if (emitterCompleted.get()) return;
                        fullResponse.append(token);
                        try {
                            emitEvent(emitter, TEXT_MESSAGE_DELTA, Map.of(
                                "runId", runId,
                                "messageId", messageId,
                                "field", "content",
                                "delta", token,
                                "content", fullResponse.toString()
                            ));
                        } catch (Exception e) {
                            log.debug("Failed to emit token: {}", e.getMessage());
                        }
                    },
                    // onComplete
                    () -> {
                        if (emitterCompleted.get()) return;
                        try {
                            emitEvent(emitter, TEXT_MESSAGE_END, Map.of(
                                "runId", runId,
                                "messageId", messageId,
                                "field", "content",
                                "content", fullResponse.toString()
                            ));
                            emitEvent(emitter, RUN_FINISHED, Map.of(
                                "runId", runId,
                                "status", "completed",
                                "content", fullResponse.toString()
                            ));
                            emitter.complete();
                        } catch (Exception e) {
                            log.debug("Failed to complete: {}", e.getMessage());
                        }
                    }
                );

            } catch (Exception e) {
                if (!isClientDisconnection(e) && !emitterCompleted.get()) {
                    log.error("Raw streaming error: {}", e.getMessage());
                    try {
                        emitEvent(emitter, RUN_ERROR, Map.of("runId", runId, "error", e.getMessage()));
                        emitter.completeWithError(e);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * Check if the exception indicates a client disconnection or emitter already completed.
     * This is expected behavior when the user navigates away or closes the connection.
     */
    private boolean isClientDisconnection(Throwable e) {
        // Check the exception chain for common disconnection indicators
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();

            // ClientAbortException - Tomcat's indicator for client disconnect
            if (className.contains("ClientAbortException")) {
                return true;
            }

            // IllegalStateException when emitter is already completed
            if (current instanceof IllegalStateException) {
                if (message != null && (
                    message.contains("already completed") ||
                    message.contains("ResponseBodyEmitter"))) {
                    return true;
                }
            }

            // IOException with "Broken pipe" or "Connection reset" messages
            if (current instanceof IOException) {
                if (message != null && (
                    message.contains("Broken pipe") ||
                    message.contains("Connection reset") ||
                    message.contains("Connection refused") ||
                    message.contains("Stream closed"))) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }
}
