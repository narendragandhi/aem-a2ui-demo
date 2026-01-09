package com.example.aema2ui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskRequest {
    private Message message;
    private Map<String, Object> context;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role = "user";
        private java.util.List<MessagePart> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessagePart {
        private String text;
    }
}
