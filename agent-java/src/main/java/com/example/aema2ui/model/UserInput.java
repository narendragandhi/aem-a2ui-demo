package com.example.aema2ui.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents parsed user input with intent detection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInput {

    @JsonPropertyDescription("The raw text input from the user")
    private String rawText;

    @JsonPropertyDescription("Detected component type the user wants (hero, product, teaser, banner, or general)")
    private String detectedComponentType;

    @JsonPropertyDescription("The target audience or context mentioned")
    private String targetAudience;

    @JsonPropertyDescription("Any specific brand or style mentioned")
    private String brandStyle;

    @JsonPropertyDescription("The tone of voice requested (professional, casual, exciting, etc.)")
    private String toneOfVoice;
}
