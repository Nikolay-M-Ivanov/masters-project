package org.example.motoadvisor.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.motoadvisor.agent.AclEnvelope;
import org.example.motoadvisor.agent.AgentBridge;
import org.example.motoadvisor.dto.*;
import org.example.motoadvisor.service.HomeSearchService;
import org.example.motoadvisor.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Search implementation that uses JADE chain when available.
 * Includes error mapping and UI fallback handling.
 */
@Service
@RequiredArgsConstructor
public class HomeSearchServiceStub implements HomeSearchService {

    private final AgentBridge agentBridge;
    private final RecommendationService recommendationService;

    @Override
    public SearchExecutionResult executeSearch(SearchCriteria criteria) {
        List<String> errors = new ArrayList<>();

        if (agentBridge.isReady()) {
            AclEnvelope response = agentBridge.submitSearch(Map.of(
                    "experienceLevel", value(criteria.getExperienceLevel()),
                    "preferredCategory", value(criteria.getPreferredCategory()),
                    "minEngineSizeCc", criteria.getMinEngineSizeCc(),
                    "maxEngineSizeCc", criteria.getMaxEngineSizeCc(),
                    "maxBudgetEur", criteria.getMaxBudgetEur(),
                    "preferredBrand", value(criteria.getPreferredBrand())
            ));

            List<Map<String, Object>> rows = response.payloadList("recommendations");
            if (!rows.isEmpty()) {
                List<RecommendationResult> results = rows.stream().map(this::toResult).toList();
                return SearchExecutionResult.builder()
                        .results(results)
                        .agentMessages(buildAgentMessages(results.size(), false))
                        .errors(List.of())
                        .fallbackUsed(false)
                        .source("JADE")
                        .build();
            }

            // Step 6.3: ACL FAILURE mappings
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                String joined = String.join("; ", response.getErrors());
                if (joined.toLowerCase(Locale.ROOT).contains("timeout")) {
                    errors.add("Agent response timed out. Showing fallback recommendations.");
                } else if (joined.toLowerCase(Locale.ROOT).contains("invalid")) {
                    errors.add("Agent contract error. Showing fallback recommendations.");
                } else {
                    errors.add("Agent failure: " + joined + ". Using fallback recommendations.");
                }
            } else {
                errors.add("No agent recommendations returned. Showing fallback recommendations.");
            }
        } else {
            errors.add("Agent platform is unavailable. Showing fallback recommendations.");
        }

        List<RecommendationResult> fallback = serviceFallback(criteria);
        return SearchExecutionResult.builder()
                .results(fallback)
                .agentMessages(buildAgentMessages(fallback.size(), true))
                .errors(errors)
                .fallbackUsed(true)
                .source("RECOMMENDATION_SERVICE")
                .build();
    }

    private List<RecommendationResult> serviceFallback(SearchCriteria criteria) {
        RiderPreferencesDto prefs = RiderPreferencesDto.builder()
                .experienceLevel(value(criteria.getExperienceLevel()))
                .preferredCategory(value(criteria.getPreferredCategory()))
                .minEngineSizeCc(criteria.getMinEngineSizeCc())
                .maxEngineSizeCc(criteria.getMaxEngineSizeCc())
                .maxBudgetEur(criteria.getMaxBudgetEur())
                .preferredBrand(value(criteria.getPreferredBrand()))
                .build();

        return recommendationService.recommend(prefs).stream()
                .map(r -> RecommendationResult.builder()
                        .brand(r.getBrand())
                        .modelName(r.getName())
                        .category(r.getCategory())
                        .engineSizeCc(r.getEngineSizeCc())
                        .priceEur(r.getPriceEur())
                        .experienceLevel(r.getExperienceLevel())
                        .score(r.getMatchScore())
                        .reason(buildServiceReason(criteria, r))
                        .build())
                .toList();
    }

    private String buildServiceReason(SearchCriteria criteria, RecommendationDto r) {
        List<String> parts = new ArrayList<>();

        if (criteria.getMaxBudgetEur() <= 0) {
            parts.add("Budget not constrained");
        } else if (r.getPriceEur() <= criteria.getMaxBudgetEur()) {
            parts.add("Within budget");
        } else {
            parts.add("Over budget");
        }

        if (criteria.getPreferredCategory() == null || criteria.getPreferredCategory().isBlank()
                || "ANY".equalsIgnoreCase(criteria.getPreferredCategory())) {
            parts.add("No segment preference");
        } else if (criteria.getPreferredCategory().equalsIgnoreCase(r.getCategory())) {
            parts.add("Matches selected segment");
        } else {
            parts.add("Different segment than selected");
        }

        int maxCc = criteria.getMaxEngineSizeCc() <= 0 ? Integer.MAX_VALUE : criteria.getMaxEngineSizeCc();
        if (r.getEngineSizeCc() >= criteria.getMinEngineSizeCc() && r.getEngineSizeCc() <= maxCc) {
            parts.add("CC fits preferred range");
        } else {
            parts.add("CC outside preferred range");
        }

        return String.join("; ", parts);
    }

    private List<AgentMessage> buildAgentMessages(int resultCount, boolean fallback) {
        String cid = "conv-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                AgentMessage.builder()
                        .sender("Spring")
                        .receiver("UserRequestAgent")
                        .performative("O2A")
                        .content(fallback ? "SEARCH_REQUEST (fallback mode)" : "Submit SEARCH_REQUEST")
                        .conversationId(cid)
                        .createdAt(now)
                        .build(),
                AgentMessage.builder()
                        .sender("UserRequestAgent")
                        .receiver("RecommendationAgent")
                        .performative("REQUEST")
                        .content("SEARCH_REQUEST")
                        .conversationId(cid)
                        .createdAt(now.plusSeconds(1))
                        .build(),
                AgentMessage.builder()
                        .sender("RecommendationAgent")
                        .receiver("UserRequestAgent")
                        .performative(fallback ? "FAILURE" : "INFORM")
                        .content("SEARCH_RESPONSE: " + resultCount + " rows")
                        .conversationId(cid)
                        .createdAt(now.plusSeconds(2))
                        .build()
        );
    }

    private RecommendationResult toResult(Map<String, Object> row) {
        return RecommendationResult.builder()
                .brand(String.valueOf(row.getOrDefault("brand", "")))
                .modelName(String.valueOf(row.getOrDefault("modelName", "")))
                .category(String.valueOf(row.getOrDefault("category", "")))
                .engineSizeCc(toInt(row.get("engineSizeCc")))
                .priceEur(toInt(row.get("priceEur")))
                .experienceLevel(String.valueOf(row.getOrDefault("experienceLevel", "")))
                .score(toInt(row.get("score")))
                .reason(String.valueOf(row.getOrDefault("reason", "")))
                .build();
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String value(String v) {
        return v == null ? "" : v;
    }
}
