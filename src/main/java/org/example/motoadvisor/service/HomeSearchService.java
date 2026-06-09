package org.example.motoadvisor.service;

import org.example.motoadvisor.dto.SearchCriteria;
import org.example.motoadvisor.dto.SearchExecutionResult;

/**
 * Service contract used by HomeController for executing search requests.
 */
public interface HomeSearchService {

    SearchExecutionResult executeSearch(SearchCriteria criteria);
}
