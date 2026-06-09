package org.example.motoadvisor.service;

import org.example.motoadvisor.service.impl.DeterministicScoringServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicScoringServiceTests {

    private final DeterministicScoringService service = new DeterministicScoringServiceImpl();

    @Test
    void deterministicScoreOrderIsStable() {
        Map<String, Object> criteria = Map.of(
                "preferredCategory", "NAKED",
                "minEngineSizeCc", 400,
                "maxEngineSizeCc", 700,
                "maxBudgetEur", 9000
        );

        List<Map<String, Object>> candidates = List.of(
                row("A", "Best Naked", "NAKED", 650, 8000),
                row("B", "Sport Mid", "SPORT", 650, 8000),
                row("C", "Naked Expensive", "NAKED", 900, 11000)
        );

        List<Map<String, Object>> first = service.scoreAndRank(criteria, candidates);
        List<Map<String, Object>> second = service.scoreAndRank(criteria, candidates);

        assertEquals(model(first.get(0)), model(second.get(0)));
        assertEquals(model(first.get(1)), model(second.get(1)));
        assertEquals(model(first.get(2)), model(second.get(2)));

        assertEquals("Best Naked", model(first.get(0)));
        assertTrue(score(first.get(0)) > score(first.get(1)));
        assertTrue(score(first.get(1)) > score(first.get(2)));
    }

    @Test
    void explanationIncludesBudgetSegmentAndCcMessages() {
        Map<String, Object> criteria = Map.of(
                "preferredCategory", "NAKED",
                "minEngineSizeCc", 400,
                "maxEngineSizeCc", 700,
                "maxBudgetEur", 9000
        );

        List<Map<String, Object>> result = service.scoreAndRank(
                criteria,
                List.of(row("A", "Explained Bike", "NAKED", 650, 8000))
        );

        String reason = String.valueOf(result.getFirst().get("reason"));

        assertTrue(reason.contains("Within budget"));
        assertTrue(reason.contains("Matches selected segment"));
        assertTrue(reason.contains("CC fits preferred range"));
    }

    private Map<String, Object> row(String brand, String model, String category, int cc, int price) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("brand", brand);
        row.put("modelName", model);
        row.put("category", category);
        row.put("engineSizeCc", cc);
        row.put("priceEur", price);
        row.put("experienceLevel", "INTERMEDIATE");
        return row;
    }

    private String model(Map<String, Object> r) {
        return String.valueOf(r.get("modelName"));
    }

    private int score(Map<String, Object> r) {
        Object s = r.get("score");
        if (s instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(s));
    }
}

