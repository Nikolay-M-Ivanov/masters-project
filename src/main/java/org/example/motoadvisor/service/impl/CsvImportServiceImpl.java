package org.example.motoadvisor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.motoadvisor.dto.CsvImportResult;
import org.example.motoadvisor.ontology.OntologyService;
import org.example.motoadvisor.ontology.model.MotorcycleOntologyRecord;
import org.example.motoadvisor.persistence.entity.ImportBatch;
import org.example.motoadvisor.persistence.entity.Motorcycle;
import org.example.motoadvisor.persistence.repository.ImportBatchRepository;
import org.example.motoadvisor.persistence.repository.MotorcycleRepository;
import org.example.motoadvisor.service.CsvImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CSV import pipeline with strict header validation and row constraints.
 *
 * Required headers:
 * - brand,name,category,engineSizeCc,experienceLevel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportServiceImpl implements CsvImportService {

    private static final Set<String> REQUIRED_HEADERS =
            Set.of("brand", "name", "category", "engineSizeCc", "experienceLevel");

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of("SPORT", "NAKED", "TOURING", "ADVENTURE", "CRUISER", "COMMUTER");

    private static final Set<String> ALLOWED_EXPERIENCE =
            Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");

    private final MotorcycleRepository motorcycleRepository;
    private final ImportBatchRepository importBatchRepository;
    private final OntologyService ontologyService;

    @Value("${app.csv.import.path:classpath:data/motorcycles.csv}")
    private String csvPath;

    @Override
    @Transactional
    public int importMotorcycles() {
        try {
            String content = readFile(csvPath);
            CsvImportResult result = importCsv(csvPath, content, false);
            if (!result.isSuccess()) {
                log.warn("[CSV-IMPORT] Startup import had errors: {}", result.getErrors());
            }
            return result.getImportedRows();
        } catch (IOException e) {
            log.error("[CSV-IMPORT ERROR] Cannot read CSV file '{}': {}", csvPath, e.getMessage());
            return 0;
        }
    }

    @Override
    public int parseCsvContent(String csvContent) {
        try {
            ParseOutcome parsed = parseRecords(csvContent);
            return parsed.validRows.size();
        } catch (IOException e) {
            log.error("[CSV-IMPORT ERROR] Failed to parse CSV content: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional
    public CsvImportResult importCsv(String sourcePath, String csvContent, boolean saveOntology) {
        ImportBatch batch = importBatchRepository.startBatch(sourcePath);

        try {
            ParseOutcome parsed = parseRecords(csvContent);

            int importedOrUpdated = 0;
            for (Motorcycle m : parsed.validRows) {
                upsertMotorcycle(m);
                upsertOntology(m);
                importedOrUpdated++;
            }

            if (saveOntology) {
                ontologyService.save();
            }

            int skipped = parsed.rowErrors.size();
            importBatchRepository.completeBatch(batch.getId(), parsed.totalRows, importedOrUpdated, skipped);

            log.info("[CSV-IMPORT] Completed batch={} source='{}' total={} upserted={} skipped={}",
                    batch.getId(), sourcePath, parsed.totalRows, importedOrUpdated, skipped);

            return CsvImportResult.builder()
                    .batchId(batch.getId())
                    .sourcePath(sourcePath)
                    .totalRows(parsed.totalRows)
                    .importedRows(importedOrUpdated)
                    .skippedRows(skipped)
                    .errors(parsed.rowErrors)
                    .success(true)
                    .build();

        } catch (Exception e) {
            importBatchRepository.failBatch(batch.getId(), e.getMessage());
            log.error("[CSV-IMPORT] Batch {} failed: {}", batch.getId(), e.getMessage(), e);

            return CsvImportResult.builder()
                    .batchId(batch.getId())
                    .sourcePath(sourcePath)
                    .totalRows(0)
                    .importedRows(0)
                    .skippedRows(0)
                    .errors(List.of(e.getMessage()))
                    .success(false)
                    .build();
        }
    }

    private ParseOutcome parseRecords(String csvContent) throws IOException {
        List<Motorcycle> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        try (Reader reader = new StringReader(csvContent);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            validateHeaders(parser.getHeaderMap().keySet());

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                totalRows++;
                try {
                    validRows.add(toMotorcycle(record));
                } catch (Exception e) {
                    String msg = "Row " + rowNum + " skipped - " + e.getMessage();
                    errors.add(msg);
                    log.warn("[CSV-IMPORT] {}", msg);
                }
            }
        }

        return new ParseOutcome(totalRows, validRows, errors);
    }

    private void validateHeaders(Set<String> headers) {
        for (String required : REQUIRED_HEADERS) {
            if (!headers.contains(required)) {
                throw new IllegalArgumentException("Missing required CSV header: '" + required + "'");
            }
        }
    }

    private Motorcycle toMotorcycle(CSVRecord r) {
        String brand = requireNonBlank(r, "brand", 2, 40);
        String name = requireNonBlank(r, "name", 2, 80);

        String category = requireNonBlank(r, "category", 3, 20).toUpperCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Unsupported category: " + category);
        }

        int engineSizeCc = parseIntRange(r, "engineSizeCc", 50, 2500);

        String experience = requireNonBlank(r, "experienceLevel", 3, 20).toUpperCase(Locale.ROOT);
        if (!ALLOWED_EXPERIENCE.contains(experience)) {
            throw new IllegalArgumentException("Unsupported experienceLevel: " + experience);
        }

        int priceEur = parseOptionalIntRange(r, "priceEur", 0, 200000);
        int seatHeightMm = parseOptionalIntRange(r, "seatHeightMm", 0, 1200);
        int weightKg = parseOptionalIntRange(r, "weightKg", 0, 1000);

        return Motorcycle.builder()
                .brand(brand)
                .name(name)
                .category(category)
                .engineSizeCc(engineSizeCc)
                .priceEur(priceEur)
                .seatHeightMm(seatHeightMm)
                .weightKg(weightKg)
                .experienceLevel(experience)
                .build();
    }

    private void upsertMotorcycle(Motorcycle incoming) {
        Motorcycle row = motorcycleRepository
                .findByBrandAndName(incoming.getBrand(), incoming.getName())
                .orElseGet(Motorcycle::new);

        row.setBrand(incoming.getBrand());
        row.setName(incoming.getName());
        row.setCategory(incoming.getCategory());
        row.setEngineSizeCc(incoming.getEngineSizeCc());
        row.setPriceEur(incoming.getPriceEur());
        row.setSeatHeightMm(incoming.getSeatHeightMm());
        row.setWeightKg(incoming.getWeightKg());
        row.setExperienceLevel(incoming.getExperienceLevel());

        motorcycleRepository.save(row);
    }

    private void upsertOntology(Motorcycle m) {
        String modelCode = normalizeModelCode(m.getName());

        MotorcycleOntologyRecord record = new MotorcycleOntologyRecord(
                "http://example.org/moto-advisor#" + modelCode,
                modelCode,
                m.getBrand(),
                m.getName(),
                m.getCategory(),
                m.getEngineSizeCc(),
                m.getPriceEur(),
                m.getSeatHeightMm(),
                m.getWeightKg(),
                m.getExperienceLevel(),
                true
        );

        ontologyService.upsertMotorcycle(record);
    }

    private String requireNonBlank(CSVRecord r, String col, int minLen, int maxLen) {
        String val = r.get(col);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Required column '" + col + "' is blank");
        }
        String trimmed = val.trim();
        if (trimmed.length() < minLen || trimmed.length() > maxLen) {
            throw new IllegalArgumentException("Column '" + col + "' length must be between " + minLen + " and " + maxLen);
        }
        return trimmed;
    }

    private int parseIntRange(CSVRecord r, String col, int min, int max) {
        String val = r.get(col);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Required numeric column '" + col + "' is blank");
        }
        try {
            int parsed = Integer.parseInt(val.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("Column '" + col + "' must be between " + min + " and " + max);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + col + "' is not a valid integer: " + val);
        }
    }

    private int parseOptionalIntRange(CSVRecord r, String col, int min, int max) {
        if (!r.isMapped(col)) return 0;
        String val = r.get(col);
        if (val == null || val.isBlank()) return 0;

        try {
            int parsed = Integer.parseInt(val.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("Column '" + col + "' must be between " + min + " and " + max);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column '" + col + "' is not a valid integer: " + val);
        }
    }

    private String normalizeModelCode(String name) {
        return name == null
                ? "UNKNOWN"
                : name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String readFile(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resource = path.substring("classpath:".length());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
                if (is == null) throw new FileNotFoundException("Classpath resource not found: " + resource);
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private record ParseOutcome(int totalRows, List<Motorcycle> validRows, List<String> rowErrors) {
    }
}

