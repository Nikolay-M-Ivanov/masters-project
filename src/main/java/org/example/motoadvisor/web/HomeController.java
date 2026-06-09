package org.example.motoadvisor.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.dto.SearchCriteria;
import org.example.motoadvisor.dto.SearchExecutionResult;
import org.example.motoadvisor.dto.SearchForm;
import org.example.motoadvisor.service.HomeSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Public controller for home/search pages.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeSearchService homeSearchService;

    @GetMapping("/")
    public String index(Model model) {
        if (!model.containsAttribute("searchForm")) {
            model.addAttribute("searchForm", SearchForm.builder().criteria(new SearchCriteria()).build());
        }
        addReferenceData(model);
        return "index";
    }

    @PostMapping("/search")
    public String search(
            @Valid @ModelAttribute("searchForm") SearchForm searchForm,
            BindingResult bindingResult,
            Model model
    ) {
        addReferenceData(model);

        if (bindingResult.hasErrors()) {
            return "index";
        }

        SearchExecutionResult execution = homeSearchService.executeSearch(searchForm.getCriteria());

        model.addAttribute("criteria", searchForm.getCriteria());
        model.addAttribute("results", execution.getResults());
        model.addAttribute("resultCount", execution.getResults().size());
        model.addAttribute("agentMessages", execution.getAgentMessages());
        model.addAttribute("errors", execution.getErrors());
        model.addAttribute("fallbackUsed", execution.isFallbackUsed());
        model.addAttribute("resultSource", execution.getSource());

        log.info("Search request handled. source={}, resultCount={}, fallbackUsed={}",
                execution.getSource(), execution.getResults().size(), execution.isFallbackUsed());
        return "search/results";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("experienceLevels", List.of("BEGINNER", "INTERMEDIATE", "ADVANCED"));
        model.addAttribute("categories", List.of("ANY", "SPORT", "NAKED", "TOURING", "ADVENTURE", "CRUISER", "COMMUTER"));
    }
}
