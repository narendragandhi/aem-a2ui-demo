package com.example.aema2ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    // Timeout configuration (in milliseconds)
    @Value("${aem.agent.llm.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${aem.agent.llm.timeout.read:60000}")
    private int readTimeout;

    public LlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // Configure timeouts to prevent hanging on slow responses
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 second connect timeout
        factory.setReadTimeout(60000);    // 60 second read timeout for LLM responses

        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .build();
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
     * Stream generation from Ollama with real-time token callbacks.
     * This provides true streaming like CLI does.
     */
    public void generateStreaming(String prompt, Consumer<String> onToken, Runnable onComplete) {
        if (!isEnabled() || !llmProvider.equalsIgnoreCase("ollama")) {
            // Fallback: generate full response and send as one chunk
            String response = generate(prompt);
            onToken.accept(response);
            onComplete.run();
            return;
        }

        try {
            URL url = new URL(ollamaBaseUrl + "/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            // Request with streaming enabled
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", true
            ));

            conn.getOutputStream().write(requestBody.getBytes());

            // Read streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    JsonNode json = objectMapper.readTree(line);
                    String token = json.path("response").asText();

                    if (token != null && !token.isEmpty()) {
                        onToken.accept(token);
                    }

                    // Check if done
                    if (json.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }

            onComplete.run();

        } catch (Exception e) {
            log.error("Ollama streaming error: {}", e.getMessage());
            throw new RuntimeException("Ollama streaming failed", e);
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
        response = cleanJsonResponse(response);

        try {
            return objectMapper.readValue(response, targetClass);
        } catch (Exception e) {
            // Try to repair common JSON issues
            String repaired = repairJson(response);
            try {
                return objectMapper.readValue(repaired, targetClass);
            } catch (Exception e2) {
                log.error("Failed to parse LLM response as {}: {}", targetClass.getSimpleName(), response);
                throw new RuntimeException("Failed to parse LLM response", e);
            }
        }
    }

    /**
     * Clean markdown and whitespace from JSON response.
     */
    private String cleanJsonResponse(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }

    /**
     * Attempt to repair common JSON formatting issues from LLMs.
     */
    private String repairJson(String json) {
        // Extract JSON object if there's garbage before/after
        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1);
        } else if (firstBrace >= 0) {
            json = json.substring(firstBrace);
        }

        // Fix hallucinated text in URL values (common with phi3:mini)
        // Pattern: URL with garbage after the query param
        json = json.replaceAll(
            "(\"https?://[^\"]+\\?w=\\d+)[^\"]*\"",
            "$1\""
        );

        // Fix any truncated string that has no closing quote
        // Add closing quote before we add missing braces
        if (!json.endsWith("}") && !json.endsWith("\"")) {
            int lastQuote = json.lastIndexOf('"');
            int lastColon = json.lastIndexOf(':');
            if (lastColon > lastQuote) {
                // We have an unclosed value, add quote
                json = json + "\"";
            }
        }

        // Fix missing commas between properties
        json = json.replaceAll("\"\\s*\\n\\s*\"", "\",\n\"");

        // Fix trailing commas before closing brackets
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");

        // Fix missing closing brackets
        long openBraces = json.chars().filter(c -> c == '{').count();
        long closeBraces = json.chars().filter(c -> c == '}').count();
        while (closeBraces < openBraces) {
            json = json + "}";
            closeBraces++;
        }

        long openBrackets = json.chars().filter(c -> c == '[').count();
        long closeBrackets = json.chars().filter(c -> c == ']').count();
        while (closeBrackets < openBrackets) {
            json = json + "]";
            closeBrackets++;
        }

        return json;
    }
}
