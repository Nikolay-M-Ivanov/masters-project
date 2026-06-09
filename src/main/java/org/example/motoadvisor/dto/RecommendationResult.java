package org.example.motoadvisor.dto;

import lombok.*;

/**
 * Lightweight recommendation row used by web pages.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResult {

    private String brand;
    private String modelName;
    private String category;
    private int engineSizeCc;
    private int priceEur;
    private String experienceLevel;
    private int score;
    private String reason;
}

