package org.example.motoadvisor.dto;

import lombok.*;

/**
 * Read-only view of a single motorcycle recommendation returned to the UI.
 * Assembled by the RecommendationService from an ontology record + a match score.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDto {

    private Long motorcycleId;

    private String brand;

    private String name;

    private String category;

    private int engineSizeCc;

    private int priceEur;

    private int seatHeightMm;

    private int weightKg;

    private String experienceLevel;

    /**
     * Match score in the range [0, 100].
     * Higher is a better fit for the submitted rider preferences.
     */
    private int matchScore;
}

