package org.example.motoadvisor.dto;

import jakarta.validation.Valid;
import lombok.*;

/**
 * Thymeleaf form wrapper for search page binding.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchForm {

    @Valid
    @Builder.Default
    private SearchCriteria criteria = new SearchCriteria();
}

