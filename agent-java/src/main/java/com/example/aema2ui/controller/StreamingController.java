package com.example.aema2ui.controller;

import com.example.aema2ui.service.StreamingContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE Streaming Controller for real-time content generation.
 *
 * Implements AG-UI protocol streaming pattern:
 * - Client connects to SSE endpoint
 * - Server streams events: RUN_STARTED, TEXT_MESSAGE_DELTA, RUN_FINISHED
 * - Client updates UI progressively as content arrives
 *
 * This creates the "ChatGPT-like" typing effect for content generation.
 */
@Slf4j
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StreamingController {

    private final StreamingContentService streamingService;

    /**
     * Stream content generation with SSE.
     *
     * Events emitted:
     * - RUN_STARTED: Generation begins
     * - TEXT_MESSAGE_START: Field generation starts
     * - TEXT_MESSAGE_DELTA: Incremental text updates (word by word)
     * - TEXT_MESSAGE_END: Field complete
     * - STATE_DELTA: Complete content state update
     * - RUN_FINISHED: Generation complete
     *
     * Usage from frontend:
     * ```javascript
     * const eventSource = new EventSource('/stream/generate?input=hero+banner&componentType=hero');
     * eventSource.addEventListener('TEXT_MESSAGE_DELTA', (e) => {
     *   const data = JSON.parse(e.data);
     *   updateField(data.data.field, data.data.content);
     * });
     * ```
     */
    @GetMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerate(
            @RequestParam String input,
            @RequestParam(required = false) String componentType) {

        log.info("Starting SSE stream for input: '{}', componentType: {}", input, componentType);

        SseEmitter emitter = streamingService.createEmitter();
        streamingService.streamContentGeneration(input, componentType, emitter);

        return emitter;
    }

    /**
     * POST variant for streaming (when input is complex/long).
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGeneratePost(@RequestBody Map<String, String> request) {
        String input = request.getOrDefault("input", "");
        String componentType = request.get("componentType");

        log.info("Starting SSE stream (POST) for input: '{}', componentType: {}", input, componentType);

        SseEmitter emitter = streamingService.createEmitter();
        streamingService.streamContentGeneration(input, componentType, emitter);

        return emitter;
    }

    /**
     * Health check for streaming endpoint.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "streaming", true,
            "protocol", "AG-UI",
            "events", new String[]{
                "RUN_STARTED",
                "TEXT_MESSAGE_START",
                "TEXT_MESSAGE_DELTA",
                "TEXT_MESSAGE_END",
                "STATE_DELTA",
                "RUN_FINISHED",
                "RUN_ERROR"
            }
        );
    }
}
