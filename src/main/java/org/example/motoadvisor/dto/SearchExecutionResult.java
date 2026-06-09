package org.example.motoadvisor.dto;

import lombok.*;

import java.util.List;

/**
 * Search output bundle including result rows plus UI-facing diagnostics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchExecutionResult {

    @Builder.Default
    private List<RecommendationResult> results = List.of();

    @Builder.Default
    private List<AgentMessage> agentMessages = List.of();

    @Builder.Default
    private List<String> errors = List.of();

    @Builder.Default
    private boolean fallbackUsed = false;

    @Builder.Default
    private String source = "UNKNOWN";
}

