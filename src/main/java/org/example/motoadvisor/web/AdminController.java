package org.example.motoadvisor.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.ontology.OntologyService;
import org.example.motoadvisor.persistence.repository.AgentLogRepository;
import org.example.motoadvisor.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin UI for ontology manual edits and agent logs.
 * Delegates business logic to AdminService.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final OntologyService ontologyService;
    private final AgentLogRepository agentLogRepository;
    private final AdminService adminService;

    @GetMapping("/admin/import")
    public String importPage(Model model) {
        populateImportModel(model, List.of());
        return "admin/import";
    }

    @PostMapping("/admin/motorcycles/{modelCode}/edit")
    public String manualEdit(
            @PathVariable String modelCode,
            @RequestParam("priceEur") int priceEur,
            @RequestParam(name = "available", defaultValue = "false") boolean available,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.updateMotorcyclePrice(modelCode, priceEur, available);
            redirectAttributes.addFlashAttribute("manualEditNotices",
                    List.of("Saved OWL changes for modelCode=" + modelCode));
            return "redirect:/admin/import";
        } catch (Exception e) {
            log.error("Manual edit failed for modelCode={}: {}", modelCode, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("manualEditNotices",
                    List.of("Manual edit failed: " + e.getMessage()));
            return "redirect:/admin/import";
        }
    }

    @GetMapping("/admin/agent-logs")
    public String agentLogs(Model model) {
        model.addAttribute("logs", agentLogRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/agent-logs";
    }

    private void populateImportModel(Model model, List<String> manualEditNotices) {
        if (!model.containsAttribute("manualEditNotices")) {
            model.addAttribute("manualEditNotices", manualEditNotices);
        }
        model.addAttribute("records", ontologyService.findAllMotorcycles());
    }
}
