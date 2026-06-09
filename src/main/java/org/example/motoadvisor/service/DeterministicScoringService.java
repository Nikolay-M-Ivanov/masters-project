package org.example.motoadvisor.service;

import java.util.List;
import java.util.Map;

/**
 * Deterministic scoring for recommendations.
 *
 * Score dimensions:
 * - budget fit
 * - segment/category fit
 * - engine CC range fit
 */
public interface DeterministicScoringService {

    /**
     * Scores and ranks candidate rows. Each returned row contains:
     * - original fields
     * - score (int 0..100)
     * - reason (explanation string)
     */
    List<Map<String, Object>> scoreAndRank(
            Map<String, Object> criteria,
            List<Map<String, Object>> candidates
    );
}

