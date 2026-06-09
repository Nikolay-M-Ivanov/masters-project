package org.example.motoadvisor.service.impl;

import org.example.motoadvisor.service.DeterministicScoringService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Deterministic scorer used by RecommendationAgent.
 *
 * Formula (max 100):
 * - Budget fit: 0..40
 * - Segment/category fit: 0..35
 * - CC fit: 0..25
 */
@Service
public class DeterministicScoringServiceImpl implements DeterministicScoringService {

    @Override
    public List<Map<String, Object>> scoreAndRank(Map<String, Object> criteria, List<Map<String, Object>> candidates) {
        List<Map<String, Object>> scored = new ArrayList<>();

        for (Map<String, Object> base : candidates) {
            Map<String, Object> row = new LinkedHashMap<>(base);

            int budgetScore = scoreBudget(criteria, row);
            int segmentScore = scoreSegment(criteria, row);
            int ccScore = scoreCc(criteria, row);
            int total = clamp(budgetScore + segmentScore + ccScore, 0, 100);

            List<String> reasons = new ArrayList<>();
            reasons.add(explainBudget(criteria, row, budgetScore));
            reasons.add(explainSegment(criteria, row, segmentScore));
            reasons.add(explainCc(criteria, row, ccScore));

            row.put("score", total);
            row.put("reason", String.join("; ", reasons));
            scored.add(row);
        }

        scored.sort(Comparator.comparingInt((Map<String, Object> r) -> toInt(r.get("score"))).reversed());
        return scored;
    }

    private int scoreBudget(Map<String, Object> c, Map<String, Object> r) {
        int budget = toInt(c.get("maxBudgetEur"));
        int price = toInt(r.get("priceEur"));
        if (budget <= 0) {
            return 30;
        }
        if (price <= budget) {
            // Within budget gets full budget points
            return 40;
        }

        int over = price - budget;
        if (over <= budget * 0.1) return 20;
        if (over <= budget * 0.2) return 10;
        return 0;
    }

    private int scoreSegment(Map<String, Object> c, Map<String, Object> r) {
        String wanted = up(c.get("preferredCategory"));
        String actual = up(r.get("category"));

        if (wanted.isBlank() || "ANY".equals(wanted)) {
            return 20;
        }
        if (wanted.equals(actual)) {
            return 35;
        }
        return 0;
    }

    private int scoreCc(Map<String, Object> c, Map<String, Object> r) {
        int minCc = toInt(c.get("minEngineSizeCc"));
        int maxCc = toInt(c.get("maxEngineSizeCc"));
        int cc = toInt(r.get("engineSizeCc"));

        int upper = maxCc <= 0 ? Integer.MAX_VALUE : maxCc;
        if (cc >= minCc && cc <= upper) {
            return 25;
        }

        if (cc < minCc && (minCc - cc) <= 100) return 12;
        if (cc > upper && upper != Integer.MAX_VALUE && (cc - upper) <= 100) return 12;
        return 0;
    }

    private String explainBudget(Map<String, Object> c, Map<String, Object> r, int score) {
        int budget = toInt(c.get("maxBudgetEur"));
        int price = toInt(r.get("priceEur"));

        if (budget <= 0) return "Budget not constrained";
        if (price <= budget) return "Within budget";
        if (score > 0) return "Slightly above budget";
        return "Over budget";
    }

    private String explainSegment(Map<String, Object> c, Map<String, Object> r, int score) {
        String wanted = up(c.get("preferredCategory"));
        String actual = up(r.get("category"));

        if (wanted.isBlank() || "ANY".equals(wanted)) return "No segment preference";
        if (score > 0) return "Matches selected segment";
        return "Different segment than selected";
    }

    private String explainCc(Map<String, Object> c, Map<String, Object> r, int score) {
        int minCc = toInt(c.get("minEngineSizeCc"));
        int maxCc = toInt(c.get("maxEngineSizeCc"));
        int cc = toInt(r.get("engineSizeCc"));

        int upper = maxCc <= 0 ? Integer.MAX_VALUE : maxCc;
        if (cc >= minCc && cc <= upper) return "CC fits preferred range";
        if (score > 0) return "CC close to preferred range";
        return "CC outside preferred range";
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String up(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

