package org.example.motoadvisor.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Form-binding DTO for the rider preferences submitted via the search form.
 * Validated by Spring MVC before reaching the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderPreferencesDto {

    /**
     * Rider experience level.
     * Allowed values: BEGINNER, INTERMEDIATE, ADVANCED
     */
    @NotBlank(message = "Experience level is required")
    private String experienceLevel;

    /**
     * Preferred riding category.
     * Allowed values: SPORT, NAKED, TOURING, ADVENTURE, CRUISER, COMMUTER, ANY
     */
    @NotBlank(message = "Preferred category is required")
    private String preferredCategory;

    /** Minimum engine size in cc. Must be ≥ 0. */
    @Min(value = 0, message = "Minimum engine size must be 0 or greater")
    private int minEngineSizeCc;

    /**
     * Maximum engine size in cc.
     * 0 means no upper limit. If > 0, must be ≥ minEngineSizeCc.
     */
    @Min(value = 0, message = "Maximum engine size must be 0 or greater")
    private int maxEngineSizeCc;

    /** Maximum budget in EUR. 0 means no limit. */
    @Min(value = 0, message = "Budget must be 0 or greater")
    private int maxBudgetEur;

    /** Optional preferred brand; blank means no preference. */
    private String preferredBrand;
}

