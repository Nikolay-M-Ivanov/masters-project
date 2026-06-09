package org.example.motoadvisor.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.agent.AgentBridge;
import org.example.motoadvisor.dto.CsvImportResult;
import org.example.motoadvisor.ontology.OntologyService;
import org.example.motoadvisor.persistence.repository.AgentLogRepository;
import org.example.motoadvisor.persistence.repository.ImportBatchRepository;
import org.example.motoadvisor.service.CsvImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Admin UI for import operations, ontology manual edits, and agent logs.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CsvImportService csvImportService;
    private final OntologyService ontologyService;
    private final ImportBatchRepository importBatchRepository;
    private final AgentLogRepository agentLogRepository;
    private final AgentBridge agentBridge;

    @GetMapping("/admin/import")
    public String importPage(Model model) {
        populateImportModel(model, null, List.of(), List.of());
        return "admin/import";
    }

    @PostMapping("/admin/import")
    public String processImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "saveOntology", defaultValue = "true") boolean saveOntology,
            Model model
    ) {
        if (file == null || file.isEmpty()) {
            populateImportModel(model, null, List.of("Please choose a CSV file."), List.of());
            return "admin/import";
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            CsvImportResult result = csvImportService.importCsv(
                    file.getOriginalFilename() == null ? "uploaded.csv" : file.getOriginalFilename(),
                    content,
                    saveOntology
            );

            populateImportModel(model, result, List.of(), List.of());
            return "admin/import";
        } catch (Exception e) {
            populateImportModel(model, null, List.of("Import failed: " + e.getMessage()), List.of());
            return "admin/import";
        }
    }

    @PostMapping("/admin/motorcycles/{modelCode}/edit")
    public String manualEdit(
            @PathVariable String modelCode,
            @RequestParam("priceEur") int priceEur,
            @RequestParam(name = "available", defaultValue = "false") boolean available,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ontologyService.updatePrice(modelCode, priceEur);
            ontologyService.updateAvailability(modelCode, available);
            ontologyService.save();

            // Also send a lightweight audit event through AgentBridge path when available.
            if (agentBridge.isReady()) {
                agentBridge.submitSearch(Map.of(
                        "experienceLevel", "BEGINNER",
                        "preferredCategory", "ANY",
                        "minEngineSizeCc", 0,
                        "maxEngineSizeCc", 0,
                        "maxBudgetEur", 0,
                        "preferredBrand", ""
                ));
            }

            redirectAttributes.addFlashAttribute("manualEditNotices",
                    List.of("Saved OWL changes for modelCode=" + modelCode));
            return "redirect:/admin/import";
        } catch (Exception e) {
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

    private void populateImportModel(Model model,
                                     CsvImportResult result,
                                     List<String> importNotices,
                                     List<String> manualEditNotices) {
        if (!model.containsAttribute("importResult")) {
            model.addAttribute("importResult", result);
        }
        if (!model.containsAttribute("importNotices")) {
            model.addAttribute("importNotices", importNotices);
        }
        if (!model.containsAttribute("manualEditNotices")) {
            model.addAttribute("manualEditNotices", manualEditNotices);
        }
        model.addAttribute("batches", importBatchRepository.findTop20ByOrderByStartedAtDesc());
        model.addAttribute("records", ontologyService.findAllMotorcycles());
        model.addAttribute("agentBridgeReady", agentBridge.isReady());
    }
}
