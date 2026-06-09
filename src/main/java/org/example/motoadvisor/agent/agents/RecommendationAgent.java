package org.example.motoadvisor.agent.agents;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.agent.AclEnvelope;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main recommendation agent.
 *
 * <p>Chain:
 * UserRequestAgent (REQUEST: SEARCH_REQUEST)
 * -> RecommendationAgent
 * -> UserRequestAgent (INFORM/FAILURE: SEARCH_RESPONSE)
 */
@Slf4j
public class RecommendationAgent extends Agent {

    private static final String CATALOG_PATH = "data/motorcycles.csv";
    private List<Map<String, Object>> catalog = List.of();

    @Override
    protected void setup() {
        catalog = loadCatalogRows();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage in = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (in == null) {
                    block(150);
                    return;
                }

                AclEnvelope req;
                try {
                    req = AclEnvelope.fromJson(in.getContent());
                } catch (Exception e) {
                    replyFailure(in, "SEARCH_RESPONSE", "Invalid request JSON");
                    return;
                }

                if (!"SEARCH_REQUEST".equals(req.getType())) {
                    replyFailure(in, "SEARCH_RESPONSE", "Unsupported request type: " + req.getType());
                    return;
                }

                try {
                    Map<String, Object> criteria = extractCriteria(req);
                    List<Map<String, Object>> baseRows = catalog;
                    List<Map<String, Object>> scored = scoreAndRank(criteria, baseRows);

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("recommendations", scored);
                    payload.put("resultCount", scored.size());

                    AclEnvelope response = AclEnvelope.ok(req.getRequestId(), "SEARCH_RESPONSE", payload);

                    ACLMessage out = in.createReply();
                    out.setPerformative(ACLMessage.INFORM);
                    out.setContent(response.toJson());
                    send(out);
                } catch (Exception ex) {
                    replyFailure(in, "SEARCH_RESPONSE", "Recommendation processing failed: " + ex.getMessage());
                }
            }
        });

        log.info("[JADE] RecommendationAgent started");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractCriteria(AclEnvelope req) {
        Object raw = req.getPayload().get("criteria");
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private List<Map<String, Object>> scoreAndRank(Map<String, Object> criteria, List<Map<String, Object>> rows) {
        int minCc = toInt(criteria.get("minEngineSizeCc"));
        int maxCc = toInt(criteria.get("maxEngineSizeCc"));
        int budget = toInt(criteria.get("maxBudgetEur"));
        String category = String.valueOf(criteria.getOrDefault("preferredCategory", "ANY"));
        String experience = String.valueOf(criteria.getOrDefault("experienceLevel", ""));
        String brand = String.valueOf(criteria.getOrDefault("preferredBrand", ""));

        int resolvedMaxCc = maxCc <= 0 ? Integer.MAX_VALUE : maxCc;
        boolean anyCategory = category.isBlank() || "ANY".equalsIgnoreCase(category);
        boolean hasExperience = experience != null && !experience.isBlank();
        boolean hasBrand = brand != null && !brand.isBlank();

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int cc = toInt(row.get("engineSizeCc"));
            int price = toInt(row.get("priceEur"));
            String rowCategory = String.valueOf(row.getOrDefault("category", ""));
            String rowExperience = String.valueOf(row.getOrDefault("experienceLevel", ""));
            String rowBrand = String.valueOf(row.getOrDefault("brand", ""));

            if (cc < minCc || cc > resolvedMaxCc) {
                continue;
            }

            if (budget > 0 && price > budget) {
                continue;
            }

            if (!anyCategory && !category.equalsIgnoreCase(rowCategory)) {
                continue;
            }

            if (hasExperience && !experience.equalsIgnoreCase(rowExperience)) {
                continue;
            }

            if (hasBrand && !brand.equalsIgnoreCase(rowBrand)) {
                continue;
            }

            int score = 0;
            List<String> reason = new ArrayList<>();

            if (budget <= 0 || price <= budget) {
                score += 40;
                reason.add(budget <= 0 ? "Budget not constrained" : "Within budget");
            } else {
                reason.add("Over budget");
            }

            if (anyCategory || category.equalsIgnoreCase(rowCategory)) {
                score += 35;
                reason.add(anyCategory ? "No segment preference" : "Matches selected segment");
            } else {
                reason.add("Different segment than selected");
            }

            if (cc >= minCc && cc <= resolvedMaxCc) {
                score += 25;
                reason.add("CC fits preferred range");
            } else {
                reason.add("CC outside preferred range");
            }

            Map<String, Object> out = new LinkedHashMap<>(row);
            out.put("score", Math.max(0, Math.min(100, score)));
            out.put("reason", String.join("; ", reason));
            scored.add(out);
        }

        scored.sort(Comparator.comparingInt(m -> -toInt(m.get("score"))));
        return scored;
    }

    private List<Map<String, Object>> loadCatalogRows() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CATALOG_PATH)) {
            if (is == null) {
                log.warn("[JADE] Catalog '{}' not found. Using empty recommendation catalog.", CATALOG_PATH);
                return List.of();
            }

            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setTrim(true)
                         .setIgnoreEmptyLines(true)
                         .build()
                         .parse(reader)) {

                List<Map<String, Object>> rows = new ArrayList<>();
                for (CSVRecord r : parser) {
                    rows.add(row(
                            r.get("brand"),
                            r.get("name"),
                            r.get("category"),
                            toInt(r.get("engineSizeCc")),
                            toInt(r.get("priceEur")),
                            r.get("experienceLevel")
                    ));
                }
                log.info("[JADE] Recommendation catalog loaded: {} rows", rows.size());
                return rows;
            }
        } catch (Exception e) {
            log.error("[JADE] Failed to load recommendation catalog '{}': {}", CATALOG_PATH, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> row(String brand, String model, String category, int engine, int price, String level) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("brand", brand);
        m.put("modelName", model);
        m.put("category", category);
        m.put("engineSizeCc", engine);
        m.put("priceEur", price);
        m.put("experienceLevel", level);
        return m;
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

    private void replyFailure(ACLMessage in, String type, String msg) {
        ACLMessage out = in.createReply();
        out.setPerformative(ACLMessage.FAILURE);
        out.setContent(AclEnvelope.error(in.getConversationId(), type, msg).toJson());
        send(out);
    }
}
