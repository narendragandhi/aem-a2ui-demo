package com.example.aema2ui.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaskResponse {
    private String id;
    private String status;
    private List<Map<String, Object>> messages;
    private List<Map<String, Object>> artifacts;
}
