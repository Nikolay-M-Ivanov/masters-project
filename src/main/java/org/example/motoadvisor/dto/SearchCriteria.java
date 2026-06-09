package org.example.motoadvisor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Input criteria used by search and recommendation flows.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCriteria {

    @NotBlank(message = "Experience level is required")
    private String experienceLevel;

    @NotBlank(message = "Category is required")
    private String preferredCategory;

    @Min(value = 0, message = "Min engine size must be >= 0")
    private Integer minEngineSizeCc;

    @Min(value = 0, message = "Max engine size must be >= 0")
    private Integer maxEngineSizeCc;

    @Min(value = 0, message = "Budget must be >= 0")
    private Integer maxBudgetEur;

    private String preferredBrand;
}

