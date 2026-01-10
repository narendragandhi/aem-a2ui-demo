package com.example.aema2ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service for LLM integration supporting multiple providers:
 * - OpenAI (GPT models)
 * - Anthropic (Claude models)
 * - Ollama (Local models like Llama, Mistral, etc.)
 */
@Slf4j
@Service
public class LlmService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${aem.agent.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${aem.agent.llm.provider:ollama}")
    private String llmProvider;

    // OpenAI configuration
    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    @Value("${aem.agent.llm.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${aem.agent.llm.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    // Anthropic configuration
    @Value("${ANTHROPIC_API_KEY:}")
    private String anthropicApiKey;

    @Value("${aem.agent.llm.anthropic.model:claude-3-haiku-20240307}")
    private String anthropicModel;

    @Value("${aem.agent.llm.anthropic.base-url:https://api.anthropic.com/v1}")
    private String anthropicBaseUrl;

    // Ollama configuration
    @Value("${aem.agent.llm.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${aem.agent.llm.ollama.model:llama3.2}")
    private String ollamaModel;

    public LlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Check if AI/LLM is enabled and configured.
     */
    public boolean isEnabled() {
        if (!aiEnabled) return false;

        return switch (llmProvider.toLowerCase()) {
            case "openai" -> !openaiApiKey.isEmpty();
            case "anthropic" -> !anthropicApiKey.isEmpty();
            case "ollama" -> true; // Ollama doesn't need API key
            default -> false;
        };
    }

    /**
     * Get the configured LLM provider name.
     */
    public String getProvider() {
        return llmProvider;
    }

    /**
     * Generate text completion from the LLM.
     */
    public String generate(String prompt) {
        if (!isEnabled()) {
            throw new IllegalStateException("LLM is not enabled or configured. Set AI_ENABLED=true and configure provider.");
        }

        log.info("Generating with {} provider", llmProvider);

        return switch (llmProvider.toLowerCase()) {
            case "openai" -> generateWithOpenAI(prompt);
            case "anthropic" -> generateWithAnthropic(prompt);
            case "ollama" -> generateWithOllama(prompt);
            default -> throw new IllegalArgumentException("Unknown LLM provider: " + llmProvider);
        };
    }

    /**
     * Generate using OpenAI API.
     */
    private String generateWithOpenAI(String prompt) {
        log.debug("Calling OpenAI API with model: {}", openaiModel);

        Map<String, Object> request = Map.of(
            "model", openaiModel,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7
        );

        try {
            String response = restClient.post()
                .uri(openaiBaseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            return json.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("OpenAI API error: {}", e.getMessage());
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Generate using Anthropic API.
     */
    private String generateWithAnthropic(String prompt) {
        log.debug("Calling Anthropic API with model: {}", anthropicModel);

        Map<String, Object> request = Map.of(
            "model", anthropicModel,
            "max_tokens", 1024,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        try {
            String response = restClient.post()
                .uri(anthropicBaseUrl + "/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            return json.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Anthropic API error: {}", e.getMessage());
            throw new RuntimeException("Failed to call Anthropic API", e);
        }
    }

    /**
     * Generate using Ollama API (local).
     */
    private String generateWithOllama(String prompt) {
        log.debug("Calling Ollama API with model: {}", ollamaModel);

        Map<String, Object> request = Map.of(
            "model", ollamaModel,
            "prompt", prompt,
            "stream", false
        );

        try {
            String response = restClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            return json.path("response").asText();
        } catch (Exception e) {
            log.error("Ollama API error: {}", e.getMessage());
            throw new RuntimeException("Failed to call Ollama API. Is Ollama running? (" + ollamaBaseUrl + ")", e);
        }
    }

    /**
     * Generate structured JSON output from the LLM.
     */
    public <T> T generateObject(String prompt, Class<T> targetClass) {
        String jsonPrompt = prompt + "\n\nRespond with valid JSON only, no markdown or explanation. " +
            "The response should be parseable as: " + targetClass.getSimpleName();

        String response = generate(jsonPrompt);

        // Clean up response - remove markdown code blocks if present
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        response = response.trim();

        try {
            return objectMapper.readValue(response, targetClass);
        } catch (Exception e) {
            log.error("Failed to parse LLM response as {}: {}", targetClass.getSimpleName(), response);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
