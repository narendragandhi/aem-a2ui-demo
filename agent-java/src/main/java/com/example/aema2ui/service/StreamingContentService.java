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
     * Uses simulated streaming by chunking the generated content.
     */
    public void streamContentGeneration(String userInput, String componentType, SseEmitter emitter) {
        String runId = UUID.randomUUID().toString();

        executor.execute(() -> {
            try {
                // 1. Emit RUN_STARTED
                emitEvent(emitter, RUN_STARTED, Map.of(
                    "runId", runId,
                    "threadId", Thread.currentThread().getName(),
                    "input", userInput
                ));

                // 2. Parse user intent
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

                // 3. Generate content
                ContentSuggestion content = contentAgent.generateContent(parsed);

                // 4. Stream each field progressively
                streamField(emitter, runId, "title", content.getTitle());
                streamField(emitter, runId, "subtitle", content.getSubtitle());
                streamField(emitter, runId, "description", content.getDescription());

                if (content.getCtaText() != null) {
                    streamField(emitter, runId, "ctaText", content.getCtaText());
                }
                if (content.getCtaUrl() != null) {
                    streamField(emitter, runId, "ctaUrl", content.getCtaUrl());
                }
                if (content.getPrice() != null) {
                    streamField(emitter, runId, "price", content.getPrice());
                }
                if (content.getImageUrl() != null) {
                    streamField(emitter, runId, "imageUrl", content.getImageUrl());
                }

                // 5. Emit complete content as state
                emitEvent(emitter, STATE_DELTA, Map.of(
                    "runId", runId,
                    "delta", Map.of(
                        "content", content,
                        "componentType", content.getComponentType()
                    )
                ));

                // 6. Emit RUN_FINISHED
                emitEvent(emitter, RUN_FINISHED, Map.of(
                    "runId", runId,
                    "status", "completed",
                    "content", content
                ));

                emitter.complete();

            } catch (Exception e) {
                log.error("Streaming error: {}", e.getMessage(), e);
                try {
                    emitEvent(emitter, RUN_ERROR, Map.of(
                        "runId", runId,
                        "error", e.getMessage()
                    ));
                    emitter.completeWithError(e);
                } catch (IOException ignored) {}
            }
        });
    }

    /**
     * Stream a single field with character-by-character simulation.
     */
    private void streamField(SseEmitter emitter, String runId, String fieldName, String value)
            throws IOException, InterruptedException {
        if (value == null || value.isEmpty()) return;

        String messageId = UUID.randomUUID().toString();

        // TEXT_MESSAGE_START
        emitEvent(emitter, TEXT_MESSAGE_START, Map.of(
            "runId", runId,
            "messageId", messageId,
            "field", fieldName
        ));

        // Stream in chunks (words for better UX)
        String[] words = value.split("(?<=\\s)|(?=\\s)");
        StringBuilder accumulated = new StringBuilder();

        for (String word : words) {
            accumulated.append(word);

            // TEXT_MESSAGE_DELTA
            emitEvent(emitter, TEXT_MESSAGE_DELTA, Map.of(
                "runId", runId,
                "messageId", messageId,
                "field", fieldName,
                "delta", word,
                "content", accumulated.toString()
            ));

            // Small delay for streaming effect (30-80ms per word)
            Thread.sleep(30 + (int)(Math.random() * 50));
        }

        // TEXT_MESSAGE_END
        emitEvent(emitter, TEXT_MESSAGE_END, Map.of(
            "runId", runId,
            "messageId", messageId,
            "field", fieldName,
            "content", value
        ));

        // Brief pause between fields
        Thread.sleep(100);
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
            log.error("Failed to emit SSE event: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Create a configured SseEmitter with appropriate timeout.
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(120000L); // 2 minute timeout

        emitter.onCompletion(() -> log.debug("SSE completed"));
        emitter.onTimeout(() -> log.warn("SSE timeout"));
        emitter.onError(e -> log.error("SSE error: {}", e.getMessage()));

        return emitter;
    }
}
